//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.channels.resource;

import com.wire.bots.channels.Database;
import com.wire.bots.channels.NewBotHandler;
import com.wire.bots.channels.Service;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.NewBotResponseModel;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/channels/{name}/bots")
public class BotsResource {
    private final NewBotHandler handler;
    private final StorageFactory storageF;
    private final CryptoFactory cryptoF;

    public BotsResource(NewBotHandler handler, StorageFactory storageF, CryptoFactory cryptoF) {
        this.handler = handler;
        this.storageF = storageF;
        this.cryptoF = cryptoF;
    }

    @POST
    public Response newBot(@HeaderParam("Authorization") String auth,
                           @PathParam("name") String name,
                           NewBot newBot) throws Exception {

        Channel channel = handler.getChannel(name);
        if (channel == null) {
            Logger.warning("Unknown Channel: %s.", name);
            return Response.
                    status(404).
                    build();
        }

        if (!Util.compareTokens(auth, channel.token)) {
            Logger.warning("Invalid Authorization for Channel: %s.", name);
            return Response.
                    ok("Invalid Authorization: " + auth).
                    status(403).
                    build();
        }

        String botId = newBot.id;
        State storage = storageF.create(botId);

        if (!storage.saveState(newBot)) {
            Logger.error("Failed to save the state. Bot: %s, Channel: %s", botId, name);
            return Response.
                    status(409).
                    build();
        }

        Database database = new Database(Service.CONFIG.getPostgres());
        if (!database.insertSubscriber(botId, name)) {
            Logger.error("Failed to save the channel id into storage. Bot: %s, Channel: %s", botId, name);
            return Response.
                    status(409).
                    build();
        }

        if (!handler.onNewBot(name, newBot)) {
            return Response.
                    status(409).
                    build();
        }

        NewBotResponseModel ret = new NewBotResponseModel();
        ret.name = channel.name;

        try (Crypto crypto = cryptoF.create(botId)) {
            ret.lastPreKey = crypto.newLastPreKey();
            ret.preKeys = crypto.newPreKeys(0, newBot.conversation.members.size() * 8);
        }

        return Response.
                ok(ret).
                status(201).
                build();
    }
}
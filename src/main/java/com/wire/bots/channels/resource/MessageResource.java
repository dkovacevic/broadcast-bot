package com.wire.bots.channels.resource;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Config;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.server.resources.MessageResourceBase;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Path("/channels/{name}/bots/{bot}/messages")
public class MessageResource extends MessageResourceBase {
    private final Config conf;

    public MessageResource(MessageHandlerBase handler, ClientRepo repo, Config conf) {
        super(handler, repo);
        this.conf = conf;
    }

    @POST
    public Response newMessage(@HeaderParam("Authorization") String auth,
                               @PathParam("name") String channelName,
                               @PathParam("bot") String bot,
                               InboundMessage inbound) throws Exception {

        Channel channel = conf.getChannels().get(channelName);
        if (channel == null) {
            Logger.warning("Unknown channel: %s.", channelName);
            return Response.
                    status(404).
                    build();
        }

        if (!Util.compareTokens(auth, channel.token)) {
            Logger.warning("Invalid Authorization for the channel: %s.", channelName);
            return Response.
                    ok("Invalid Authorization: " + auth).
                    status(403).
                    build();
        }

        WireClient wireClient = repo.getWireClient(bot);
        if (wireClient == null) {
            return Response.
                    ok().
                    status(410).
                    build();
        }

        try {
            handleMessage(inbound, wireClient);
        } catch (Exception e) {
            Logger.error("NewMessage: Bot: %s, type: %s, error: %s", wireClient.getId(), inbound.type, e);
        }

        return Response.
                ok().
                status(200).
                build();
    }
}

package com.wire.bots.channels.resource;

import com.wire.bots.channels.Broadcaster;
import com.wire.bots.channels.Service;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Message;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.Util;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Path("/channels/{name}/broadcast")
public class BroadcastResource {
    private final Broadcaster broadcaster;

    public BroadcastResource(ClientRepo repo) {
        broadcaster = new Broadcaster(repo);
    }

    @POST
    public Response broadcast(@HeaderParam("Authorization") String auth,
                              @PathParam("name") String channelName,
                              Message msg) throws Exception {

        Channel channel = Service.storage.getChannel(channelName);
        if (channel == null) {
            Logger.warning("Unknown Channel: %s.", channelName);
            return Response.
                    status(404).
                    build();
        }

        if (!Util.compareTokens(auth, channel.token)) {
            Logger.warning("Invalid Authorization for Channel: %s.", channelName);
            return Response.
                    ok("Invalid Authorization: " + auth + "\n").
                    status(403).
                    build();
        }

        if (channel.admin == null) {
            return Response.
                    ok("Channel not yet activated\n").
                    status(403).
                    build();
        }

        broadcaster.broadcast(channelName, msg.getText());

        return Response.
                ok("Successfully broadcast into channel: " + channelName + "\n").
                status(200).
                build();
    }
}

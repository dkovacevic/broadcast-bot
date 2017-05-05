package com.wire.bots.channels.resource;

import com.wire.bots.channels.Service;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Config;
import com.wire.bots.sdk.*;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.server.resources.MessageResourceBase;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Path("/channels/{name}/bots/{bot}/messages")
public class ChannelMessageResource extends MessageResourceBase {
    public ChannelMessageResource(MessageHandlerBase handler, ClientRepo repo, Config conf) {
        super(handler, conf, repo);
    }

    @POST
    public Response newMessage(@HeaderParam("Authorization") String auth,
                               @PathParam("name") String channelName,
                               @PathParam("bot") String bot,
                               InboundMessage inbound) throws Exception {

        Channel channel = Service.dbManager.getChannel(channelName);
        if (!Util.compareTokens(auth, channel.token)) {
            Logger.warning("Invalid Authorization for the channel: %s.", channelName);
            return Response.
                    status(403).
                    build();
        }

        WireClient client = repo.getWireClient(bot);

        handleMessage(inbound, client);

        return Response.
                ok().
                status(200).
                build();
    }
}

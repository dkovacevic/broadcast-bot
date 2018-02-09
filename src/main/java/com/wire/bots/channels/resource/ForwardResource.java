package com.wire.bots.channels.resource;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/forward/{bot}")
public class ForwardResource {

    private final ClientRepo repo;

    public ForwardResource(ClientRepo repo) {
        this.repo = repo;
    }

    @POST
    public Response forward(@PathParam("bot") String botId, String payload) throws Exception {

        WireClient wireClient = repo.getWireClient(botId);
        if (wireClient == null) {
            return Response.
                    status(410).
                    build();
        }

        try {
            wireClient.sendText(payload);
        } catch (IOException e) {
            return Response.
                    status(409).
                    entity(e.getMessage()).
                    build();
        }

        return Response.
                ok().
                build();
    }
}
package com.wire.bots.channels.resource;

import com.wire.bots.channels.model.BatchForward;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/forward/batch")
@Consumes(MediaType.APPLICATION_JSON)
public class BatchForwardResource {

    private final ClientRepo repo;

    public BatchForwardResource(ClientRepo repo) {
        this.repo = repo;
    }

    @PUT
    public Response forward(BatchForward batch) throws Exception {

        for (String botId : batch.bots) {
            try {
                WireClient wireClient = repo.getWireClient(botId);
                wireClient.sendText(batch.payload);
            } catch (IOException e) {
                Logger.warning(e.toString());
            }
        }
        return Response.
                ok().
                build();
    }
}
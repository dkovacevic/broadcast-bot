package com.wire.bots.channels.resource;

import com.wire.bots.channels.model.BatchForward;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/forward/batch")
@Consumes(MediaType.APPLICATION_JSON)
public class BatchForwardResource {

    private final ClientRepo repo;

    public BatchForwardResource(ClientRepo repo) {
        this.repo = repo;
    }

    @PUT
    public Response forward(BatchForward batch) throws Exception {
        int success = 0;
        for (String botId : batch.bots) {
            WireClient wireClient = repo.getWireClient(botId);
            try {
                wireClient.sendText(batch.payload);
                success++;
            } catch (HttpException e) {
                if (e.getStatusCode() == 404) {
                    repo.purgeBot(wireClient.getId());
                }
            } catch (Exception e) {
                Logger.warning("BatchForwardResource.forward: Bot: %s. %s", botId, e);
            }
        }
        Logger.info("Forwarded: %d out of %d", success, batch.bots.size());
        return Response.
                ok().
                build();
    }
}
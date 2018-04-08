package com.wire.bots.channels.resource;

import com.wire.bots.channels.Database;
import com.wire.bots.channels.Service;
import com.wire.bots.channels.model.BatchForward;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.exceptions.MissingStateException;
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
        Database database = new Database(Service.CONFIG.getPostgres());

        int success = 0;
        for (String botId : batch.bots) {
            try {
                WireClient wireClient = repo.getWireClient(botId);
                if (wireClient == null) {
                    boolean unsubscribed = database.unsubscribe(botId);
                    Logger.warning("Unsubscribed: %s, %s", botId, unsubscribed);
                    repo.purgeBot(botId);
                    continue;
                }

                wireClient.sendText(batch.payload);
                success++;
            } catch (HttpException e) {
                if (e.getStatusCode() == 404) {
                    repo.purgeBot(botId);
                }
            } catch (MissingStateException e) {
                database.unsubscribe(botId);
                repo.purgeBot(botId);
            } catch (Exception e) {
                Logger.warning("BatchForwardResource.forward: Bot: %s. %s", botId, e);
            }
        }
        //Logger.info("Forwarded: %d out of %d", success, batch.bots.size());
        return Response.
                ok().
                build();
    }
}
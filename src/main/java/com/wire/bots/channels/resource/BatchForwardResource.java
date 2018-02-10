package com.wire.bots.channels.resource;

import com.wire.bots.channels.Service;
import com.wire.bots.channels.model.BatchForward;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Path("/forward/batch")
@Consumes(MediaType.APPLICATION_JSON)
public class BatchForwardResource {

    private final ClientRepo repo;

    public BatchForwardResource(ClientRepo repo) {
        this.repo = repo;
    }

    @PUT
    public Response forward(BatchForward batch) throws Exception {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(Service.CONFIG.threads);

        for (String botId : batch.bots) {
            executor.execute(() -> {
                try {
                    WireClient wireClient = repo.getWireClient(botId);
                    wireClient.sendText(batch.payload);
                } catch (Exception e) {
                    Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        return Response.
                ok().
                build();
    }
}
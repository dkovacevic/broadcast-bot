package com.wire.bots.channels.resource;

import com.wire.bots.channels.Service;
import com.wire.bots.channels.model.Admin;
import com.wire.bots.channels.model.Config;
import com.wire.bots.sdk.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

@Path("/admin/channels/{name}")
public class AdminResource {

    private final Config config;

    public AdminResource(Config config) {
        this.config = config;
    }

    @POST
    public Response newBot(@HeaderParam("Authorization") String auth,
                           @PathParam("name") String channelName,
                           Admin admin) throws Exception {

        if (!auth.equals(config.getAppSecret())) {
            Logger.warning("Admin: Invalid Authorization.");
            return Response.
                    status(403).
                    build();
        }

        Logger.info("New Channel: %s, origin: %s, token: %s created",
                channelName,
                admin.getOrigin(),
                admin.getToken());

        try {
            Service.dbManager.insertChannel(channelName, admin.token, admin.origin);
        } catch (SQLException e) {
            Logger.warning(e.getMessage());
            return Response.
                    ok("Channel named: " + channelName + " already exists").
                    status(405).
                    build();
        }

        return Response.
                ok().
                status(200).
                build();
    }

    @DELETE
    public Response deleteBot(@HeaderParam("Authorization") String auth,
                              @PathParam("name") String channelName,
                              Admin admin) throws Exception {

        if (!auth.equals(config.getAppSecret())) {
            Logger.warning("Admin: Invalid Authorization.");
            return Response.
                    status(403).
                    build();
        }

        Logger.info("Delete Channel: %s, origin: %s, token: %s",
                channelName,
                admin.getOrigin(),
                admin.getToken());

        Service.dbManager.deleteChannel(channelName, admin.origin);

        Service.dbManager.deleteBots(channelName);

        return Response.
                ok().
                status(201).
                build();
    }
}

package com.wire.bots.channels.resource;

import com.wire.bots.channels.Service;
import com.wire.bots.channels.model.Admin;
import com.wire.bots.channels.model.Config;
import com.wire.bots.sdk.Logger;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

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

        Logger.info("Admin: channel: %s, origin: %s, token: %s",
                channelName,
                admin.getOrigin(),
                admin.getToken());

        Service.dbManager.updateChannel(channelName, admin.token, admin.origin);

        return Response.
                ok().
                status(200).
                build();
    }
}
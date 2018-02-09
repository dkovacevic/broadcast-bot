package com.wire.bots.channels.clients;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.bots.channels.Service;
import com.wire.bots.sdk.models.TextMessage;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ForwardClient {

    private static final WebTarget target;

    static {
        ClientConfig cfg = new ClientConfig(JacksonJsonProvider.class);
        target = JerseyClientBuilder
                .createClient(cfg)
                .target(Service.CONFIG.host)
                .path("admin")
                .path("forward");
    }

    public static int forward(String bot, TextMessage msg) {
        Response response = target.
                path(bot).
                request().
                post(Entity.entity(msg.getText(), MediaType.TEXT_PLAIN));

        return response.getStatus();
    }
}

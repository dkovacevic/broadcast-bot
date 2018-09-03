package com.wire.bots.channels;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.resource.BotsResource;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.crypto.storage.RedisStorage;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.*;
import com.wire.bots.sdk.state.RedisState;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

public class BotsResourceTest {
    @ClassRule
    public static final ResourceTestRule resources;

    private static final String CHANNEL_NAME = "official";

    private final static CryptoFactory cryptoFactory;
    private final static StorageFactory storageFactory;
    private final static NewBotHandler handler;
    private static final String TOKEN = "official_token";
    private final static String admin = UUID.randomUUID().toString();

    static {
        Service.CONFIG = new Config();

        Configuration.DB postgres = Service.CONFIG.postgres = new Configuration.DB();
        postgres.host = "localhost";
        postgres.port = 5432;
        postgres.database = "postgres";
        postgres.user = "dejankovacevic";
        postgres.password = "password";

        Configuration.DB redis = Service.CONFIG.db = new Configuration.DB();
        redis.host = "localhost";
        redis.port = 6379;

        Channel channel = new Channel();
        channel.id = CHANNEL_NAME;
        channel.name = "Wire Official";
        channel.token = TOKEN;
        channel.admin = admin;

        handler = new NewBotHandler(Service.CONFIG, null);

        cryptoFactory = (botId) -> new CryptoDatabase(botId, new RedisStorage(
                redis.host
        ));

        storageFactory = (bot) -> new RedisState(bot, redis);

        resources = ResourceTestRule.builder()
                .addResource(new BotsResource(handler, storageFactory, cryptoFactory))
                .build();
    }

    @Test
    public void testStorage() throws Exception {
        WebTarget path = resources
                .target(CHANNEL_NAME)
                .path("bots");

        NewBot newBot = new NewBot();
        newBot.id = admin;
        newBot.token = TOKEN;
        newBot.origin = new User();
        newBot.origin.handle = "handle";
        newBot.conversation = new Conversation();
        newBot.conversation.members = new ArrayList<>();
        newBot.conversation.members.add(new Member());

        Response res = path.request(MediaType.APPLICATION_JSON)
                .header("Authorization", TOKEN)
                .post(Entity.entity(newBot, MediaType.APPLICATION_JSON));

        assert res.getStatus() == 201;

        NewBotResponseModel newBotResponseModel = res.readEntity(NewBotResponseModel.class);

        assert newBotResponseModel.name.equals(CHANNEL_NAME);
    }
}

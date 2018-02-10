package com.wire.bots.channels;

import com.wire.bots.channels.model.Config;
import com.wire.bots.channels.resource.BotsResource;
import com.wire.bots.sdk.crypto.CryptoFile;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.NewBotResponseModel;
import com.wire.bots.sdk.storage.FileStorage;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.testing.junit.ResourceTestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class BotsResourceTest {
    //@ClassRule
    public static final DropwizardAppRule<Config> app = new DropwizardAppRule<>(Service.class, "channels.yaml");
    private static final String CHANNEL_NAME = "official";

    private static CryptoFactory cryptoFactory = (bot) -> new CryptoFile(app.getConfiguration().data, bot);
    private static StorageFactory storageFactory = (bot) -> new FileStorage(app.getConfiguration().data, bot);
    private static NewBotHandler handler = new NewBotHandler(app.getConfiguration(), null);
    private static BotsResource RESOURCE;

    //@ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(RESOURCE)
            .build();

    //@BeforeClass
    public static void setUp() throws Exception {
        RESOURCE = new BotsResource(handler, storageFactory, cryptoFactory);

    }

    //@Test
    public void testStorage() throws Exception {
        WebTarget path = resources
                .target("channels")
                .path(CHANNEL_NAME)
                .path("bots");

        NewBot newBot = new NewBot();
        Response res = path.request(MediaType.APPLICATION_JSON)
                .header("Authorization", "official_token")
                .post(Entity.entity(newBot, MediaType.APPLICATION_JSON));

        NewBotResponseModel newBotResponseModel = res.readEntity(NewBotResponseModel.class);

        assert newBotResponseModel.name.equals(CHANNEL_NAME);
    }
}

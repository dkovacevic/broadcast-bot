package com.wire.bots.channels;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Config;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;

public class ConfigTest {
    @ClassRule
    public static final DropwizardAppRule<Config> app = new DropwizardAppRule<>(Service.class, "channels.yaml");

    @BeforeClass
    public static void setUp() throws Exception {
    }

    @Test
    public void configLoadTest() {
        Config configuration = app.getConfiguration();
        HashMap<String, Channel> channels = configuration.getChannels();
        assert channels.size() == 2;
        assert channels.get("official").name.equals("official");
    }
}

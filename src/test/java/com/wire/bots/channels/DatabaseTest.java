package com.wire.bots.channels;

import com.wire.bots.sdk.Configuration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;

public class DatabaseTest {

    @Test
    public void test() throws Exception {
        Configuration.DB conf = new Configuration.DB();
        conf.host = "localhost";
        conf.port = 5432;
        conf.database = "postgres";
        conf.user = "dejankovacevic";
        conf.password = "password";

        String botId = UUID.randomUUID().toString();

        Database db = new Database(conf);
        String channel = "test_channel_123";

        boolean b = db.insertSubscriber(botId, channel);
        assert b;

        ArrayList<String> subscribers = db.getSubscribers(channel);
        assert subscribers.contains(botId);

        String dbChannel = db.getChannel(botId);
        assert dbChannel != null;
        assert dbChannel.equalsIgnoreCase(channel);
    }
}

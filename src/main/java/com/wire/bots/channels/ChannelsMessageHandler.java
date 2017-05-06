package com.wire.bots.channels;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Config;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;

public class ChannelsMessageHandler extends ChannelsMessageHandlerBase {
    public ChannelsMessageHandler(ClientRepo repo, Config config) {
        super(repo, config);
    }

    @Override
    protected void broadcast(Channel channel, TextMessage msg) throws Exception {
        broadcaster.broadcast(channel.name, msg);
    }

    @Override
    protected void broadcast(Channel channel, ImageMessage msg, byte[] bytes) throws Exception {
        broadcaster.broadcast(channel.name, msg, bytes);
    }

    @Override
    protected void onNewSubscriber(NewBot newBot) throws Exception {
        Channel channel = getChannel(newBot.id);
        User origin = newBot.origin;
        Logger.info(String.format("onNewSubscriber: channel: %s, origin: %s, '%s' locale: %s",
                channel.name,
                origin.id,
                origin.name,
                newBot.locale));

        if (!channel.muted)
            broadcaster.newUserFeedback(channel.name, origin.name);
    }

    @Override
    protected void onNewFeedback(Channel channel, TextMessage msg) throws Exception {
        broadcaster.forwardFeedback(channel.name, msg);
    }

    @Override
    protected void onNewFeedback(Channel channel, ImageMessage msg) throws Exception {
        broadcaster.forwardFeedback(channel.name, msg);
    }
}

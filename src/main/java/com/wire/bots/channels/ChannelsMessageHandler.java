package com.wire.bots.channels;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;

public class ChannelsMessageHandler extends ChannelsMessageHandlerBase {
    public ChannelsMessageHandler(ClientRepo repo) {
        super(repo);
    }

    @Override
    protected void broadcast(Channel channel, TextMessage msg) throws Exception {
        broadcaster.broadcast(channel, msg);
    }

    @Override
    protected void broadcast(Channel channel, ImageMessage msg, byte[] bytes) throws Exception {
        broadcaster.broadcast(channel.name, msg, bytes);
    }

    @Override
    protected void onNewSubscriber(Channel channel, NewBot newBot) throws Exception {
        User origin = newBot.origin;
        Logger.info(String.format("onNewSubscriber: channel: %s, origin: %s, '%s' locale: %s",
                channel.name,
                origin.id,
                origin.name,
                newBot.locale));

        if (!channel.muted)
            broadcaster.newUserFeedback(channel.admin, origin.name);
    }

    @Override
    protected void onNewFeedback(Channel channel, TextMessage msg) throws Exception {
        broadcaster.forwardFeedback(channel.admin, msg);
    }

    @Override
    protected void onNewFeedback(Channel channel, ImageMessage msg) throws Exception {
        broadcaster.forwardFeedback(channel.admin, msg);
    }
}

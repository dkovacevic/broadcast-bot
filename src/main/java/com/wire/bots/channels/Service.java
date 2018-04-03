//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.channels;

import com.github.mtakaki.dropwizard.admin.AdminResourceBundle;
import com.wire.bots.channels.resource.BatchForwardResource;
import com.wire.bots.channels.resource.BotsResource;
import com.wire.bots.channels.resource.ForwardResource;
import com.wire.bots.channels.resource.MessageResource;
import com.wire.bots.cryptobox.storage.PgStorage;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.Server;
import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.state.PostgresState;
import com.wire.bots.sdk.tools.Logger;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class Service extends Server<Config> {
    public static Config CONFIG;
    private final AdminResourceBundle admin = new AdminResourceBundle();
    private Broadcaster broadcaster;

    public static void main(String[] args) {
        try {
            new Service().run(args);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.toString());
        }
    }

    @Override
    protected MessageHandlerBase createHandler(Config config, Environment env) throws Exception {
        StorageFactory storageFactory = getStorageFactory(config);
        broadcaster = new Broadcaster(repo, storageFactory);

        return new MessageHandler(broadcaster);
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        bootstrap.addBundle(admin);
    }

    @Override
    protected void initialize(Config config, Environment env) throws Exception {
        CONFIG = config;
        Logger.info("Channel Service Host: %s", config.host);
        Logger.info("DB Host: %s", config.db.host);
    }

    @Override
    protected void onRun(Config config, Environment env) {
        admin.getJerseyEnvironment()
                .register(new ForwardResource(repo));
        admin.getJerseyEnvironment()
                .register(new BatchForwardResource(repo));
    }

    @Override
    protected void botResource(Config config, Environment env, MessageHandlerBase handler) {
        CryptoFactory cryptoFactory = getCryptoFactory(config);
        StorageFactory storageFactory = getStorageFactory(config);

        NewBotHandler newBotHandler = new NewBotHandler(config, broadcaster);
        BotsResource botsResource = new BotsResource(newBotHandler, storageFactory, cryptoFactory);

        addResource(botsResource, env);
    }

    @Override
    protected void messageResource(Config config, Environment env, MessageHandlerBase handler) {
        MessageResource messageResource = new MessageResource(handler, repo, config);
        addResource(messageResource, env);
    }

    @Override
    protected StorageFactory getStorageFactory(Config config) {
        return botId -> new PostgresState(botId, config.db);
    }

    @Override
    protected CryptoFactory getCryptoFactory(Config config) {
        return (botId) -> new CryptoDatabase(botId, new PgStorage(
                config.db.user,
                config.db.password,
                config.db.database,
                config.db.host,
                config.db.port));
    }
}

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

import com.wire.bots.channels.model.Config;
import com.wire.bots.channels.resource.AdminResource;
import com.wire.bots.channels.resource.BotsResource;
import com.wire.bots.channels.resource.MessageResource;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.Server;
import io.dropwizard.setup.Environment;

public class Service extends Server<Config> {
    public static Storage storage;

    public static void main(String[] args) throws Exception {
        new Service().run(args);
    }

    @Override
    protected MessageHandlerBase createHandler(Config config, Environment env) {
        return new MessageHandler(repo);
    }

    @Override
    protected void onRun(Config config, Environment env) {
        addResource(new AdminResource(config), env);
        storage = new Storage(config.getDatabase());
    }

    @Override
    protected void botResource(Config config, Environment env, MessageHandlerBase handler) {
        addResource(new BotsResource(handler, repo, config), env);
    }

    @Override
    protected void messageResource(Config config, Environment env, MessageHandlerBase handler) {
        addResource(new MessageResource(handler, repo, config), env);
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.net;


import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

import java.util.HashMap;
import java.util.UUID;

/**
 * GuacamoleTunnel implementation which uses a provided socket. The UUID of
 * the tunnel will be randomly generated.
 */
public class SimpleGuacamoleTunnel extends AbstractGuacamoleTunnel {

    /**
     * The UUID associated with this tunnel. Every tunnel must have a
     * corresponding UUID such that tunnel read/write requests can be
     * directed to the proper tunnel.
     */
    private final UUID uuid = UUID.randomUUID();

    private String tunnelRequestTypeName = "";
    private GuacamoleConfiguration connectionConfiguration = null;
    private GuacamoleClientInformation tunnelRequestInfo;
    // @rjp
    private HashMap<String, Object> extras = new HashMap<String, Object>();

    /**
     * The GuacamoleSocket that tunnel should use for communication on
     * behalf of the connecting user.
     */
    private final GuacamoleSocket socket;

    /**
     * Creates a new GuacamoleTunnel which synchronizes access to the
     * Guacamole instruction stream associated with the given GuacamoleSocket.
     *
     * @param socket The GuacamoleSocket to provide synchronized access for.
     */
    public SimpleGuacamoleTunnel(GuacamoleSocket socket) {
        this.socket = socket;
    }

    @Override
    public void setTunnelRequestTypeName(String tunnelRequestTypeName) {
        this.tunnelRequestTypeName = tunnelRequestTypeName;
    }

    @Override
    public String getTunnelRequestTypeName() {
        return this.tunnelRequestTypeName;
    }

    @Override
    public void setConnectionConfiguration(GuacamoleConfiguration configuration) {
        this.connectionConfiguration = configuration;
    }

    @Override
    public GuacamoleConfiguration getConnectionConfiguration() {
        return this.connectionConfiguration;
    }

    @Override
    public void setTunnelRequestInfo(GuacamoleClientInformation info) {
        this.tunnelRequestInfo = info;
    }

    @Override
    public GuacamoleClientInformation getTunnelRequestInfo() {
        return this.tunnelRequestInfo;
    }

    // @rjp
    @Override
    public void setExtra(String key, Object object) {
        this.extras.put(key, object);
    }

    // @rjp
    @Override
    public Object getExtra(String key) {
        return this.extras.get(key);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public GuacamoleSocket getSocket() {
        return socket;
    }

}

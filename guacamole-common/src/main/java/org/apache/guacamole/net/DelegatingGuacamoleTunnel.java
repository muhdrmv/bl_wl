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

import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * GuacamoleTunnel implementation which simply delegates all function calls to
 * an underlying GuacamoleTunnel.
 */
public class DelegatingGuacamoleTunnel implements GuacamoleTunnel {

    /**
     * The wrapped GuacamoleTunnel.
     */
    private final GuacamoleTunnel tunnel;

    /**
     * Wraps the given tunnel such that all function calls against this tunnel
     * will be delegated to it.
     *
     * @param tunnel
     *     The GuacamoleTunnel to wrap.
     */
    public DelegatingGuacamoleTunnel(GuacamoleTunnel tunnel) {
        this.tunnel = tunnel;
    }

    @Override
    public void setTunnelRequestTypeName(String tunnelRequestTypeName) {
        this.tunnel.setTunnelRequestTypeName(tunnelRequestTypeName);
    }

    @Override
    public String getTunnelRequestTypeName() {
        return this.tunnel.getTunnelRequestTypeName();
    }

    @Override
    public void setConnectionConfiguration(GuacamoleConfiguration configuration) {
        this.tunnel.setConnectionConfiguration(configuration);
    }

    @Override
    public GuacamoleConfiguration getConnectionConfiguration() {
        return this.tunnel.getConnectionConfiguration();
    }

    @Override
    public void setTunnelRequestInfo(GuacamoleClientInformation info) {
        this.tunnel.setTunnelRequestInfo(info);
    }

    @Override
    public GuacamoleClientInformation getTunnelRequestInfo() {
        return this.tunnel.getTunnelRequestInfo();
    }

    // @rjp
    @Override
    public void setExtra(String key, Object object) {
        this.tunnel.setExtra(key, object);
    }

    // @rjp
    @Override
    public Object getExtra(String key) {
        return this.tunnel.getExtra(key);
    }


    @Override
    public GuacamoleReader acquireReader() {
        return tunnel.acquireReader();
    }

    @Override
    public void releaseReader() {
        tunnel.releaseReader();
    }

    @Override
    public boolean hasQueuedReaderThreads() {
        return tunnel.hasQueuedReaderThreads();
    }

    @Override
    public GuacamoleWriter acquireWriter() {
        return tunnel.acquireWriter();
    }

    @Override
    public void releaseWriter() {
        tunnel.releaseWriter();
    }

    @Override
    public boolean hasQueuedWriterThreads() {
        return tunnel.hasQueuedWriterThreads();
    }

    @Override
    public UUID getUUID() {
        return tunnel.getUUID();
    }

    @Override
    public GuacamoleSocket getSocket() {
        return tunnel.getSocket();
    }

    @Override
    public void close() throws GuacamoleException {
        tunnel.close();
    }

    @Override
    public boolean isOpen() {
        return tunnel.isOpen();
    }

}

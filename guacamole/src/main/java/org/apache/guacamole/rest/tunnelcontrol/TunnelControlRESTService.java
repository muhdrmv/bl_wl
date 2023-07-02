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

package org.apache.guacamole.rest.tunnelcontrol;


import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleSession;
import org.apache.guacamole.rest.auth.TokenSessionMap;
import org.apache.guacamole.tunnel.UserTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.UUID;


/**
 * A REST Service for controlling tunnel service
 */
@Path("/tunnel-control")
@Produces(MediaType.APPLICATION_JSON)
public class TunnelControlRESTService {

    /**
     * The map of auth tokens to sessions for the REST endpoints.
     */
    @Inject
    private TokenSessionMap tokenSessionMap;

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TunnelControlRESTService.class);

    /**
     * Simple rest response for health check requests
     */
    @Path("/live")
    @GET
    public String getLive() {
        long currentEpoch = System.currentTimeMillis() / 1000L;
        String jsonString = "{\"live\": true, \"time\": " + currentEpoch + "}";
        return jsonString;
    }

    /**
     * if specified token has or ever had any live tunnels
     */
    @Path("/session-status/{token}")
    @GET
    public String sessionStatus(
            @PathParam("token") String authToken) {

        GuacamoleSession session = tokenSessionMap.get(authToken);
        boolean hasTunnel = session.hasTunnels();
        boolean hadTunnel = session.hadTunnels();

        String jsonString = "{\"ok\": true, " +
                "\"hasTunnel\": " + hasTunnel +
                ",\"hadTunnel\": " + hadTunnel + "}";
        return jsonString;
    }

    /**
     * Invalidate specified token, terminate it's live tunnels
     */
    @Path("/invalidate-session/{token}")
    @GET
    public String invalidateSession(
            @PathParam("token") String authToken) {

        GuacamoleSession session = tokenSessionMap.remove(authToken);
        session.invalidate();

        String jsonString = "{\"ok\": true}";
        return jsonString;
    }

    /**
     * if specified token has or ever had any live tunnels
     */
    @Path("/session-tunnel/{token}")
    @GET
    public String sessionTunnel(
            @PathParam("token") String authToken) {
        try {
            GuacamoleSession session = tokenSessionMap.get(authToken);
            Map<String, UserTunnel> tunnels = session.getTunnels();
            Map.Entry<String, UserTunnel> entry = tunnels.entrySet().iterator().next();
            String tunnelKey = entry.getKey();
            UserTunnel tunnel = entry.getValue();
            UUID tunnelUUID = tunnel.getUUID();
            String tunnelId = tunnelUUID.toString();

            String jsonString = "{\"ok\": true, \"tunnelId\": \"" + tunnelId + "\"}";

            return jsonString;
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            String jsonString = "{\"ok\": false, \"msg\": \"" + msg + "\"}";
            return jsonString;
        }
    }
}

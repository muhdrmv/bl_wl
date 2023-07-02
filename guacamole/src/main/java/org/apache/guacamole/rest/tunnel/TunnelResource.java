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

package org.apache.guacamole.rest.tunnel;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.protocols.ProtocolInfo;
import org.apache.guacamole.rest.activeconnection.APIActiveConnection;
import org.apache.guacamole.rest.directory.DirectoryObjectResource;
import org.apache.guacamole.rest.directory.DirectoryObjectResourceFactory;
import org.apache.guacamole.rest.tunnelcontrol.TunnelControlRESTService;
import org.apache.guacamole.tunnel.UserTunnel;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Struct;

/**
 * A REST resource which abstracts the operations available for an individual
 * tunnel.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TunnelResource {

    /**
     * The media type to send as the content type of stream contents if no
     * other media type is specified.
     */
    private static final String DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_OCTET_STREAM;

    /**
     * The tunnel that this TunnelResource represents.
     */
    private final UserTunnel tunnel;

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * A factory which can be used to create instances of resources representing
     * ActiveConnections.
     */
    @Inject
    private DirectoryObjectResourceFactory<ActiveConnection, APIActiveConnection>
            activeConnectionResourceFactory;

    /**
     * Creates a new TunnelResource which exposes the operations and
     * subresources available for the given tunnel.
     *
     * @param tunnel
     *     The tunnel that this TunnelResource should represent.
     */
    @AssistedInject
    public TunnelResource(@Assisted UserTunnel tunnel) {
        this.tunnel = tunnel;
    }

    /**
     * Retrieves a resource representing the ActiveConnection object associated
     * with this tunnel.
     *
     * @return
     *     A resource representing the ActiveConnection object associated with
     *     this tunnel.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the ActiveConnection.
     */
    @Path("activeConnection")
    public DirectoryObjectResource<ActiveConnection, APIActiveConnection>
        getActiveConnection() throws GuacamoleException {

        // Pull the UserContext from the tunnel
        UserContext userContext = tunnel.getUserContext();

        // Fail if the active connection cannot be found
        ActiveConnection activeConnection = tunnel.getActiveConnection();
        if (activeConnection == null)
            throw new GuacamoleResourceNotFoundException("No readable active connection for tunnel.");

        // Return the associated ActiveConnection as a resource
        return activeConnectionResourceFactory.create(userContext,
                userContext.getActiveConnectionDirectory(), activeConnection);

    }

    /**
     * Retrieves the underlying protocol used by the connection associated with
     * this tunnel. If possible, the parameters available for that protocol are
     * retrieved, as well.
     *
     * @return
     *     A ProtocolInfo object describing the protocol used by the connection
     *     associated with this tunnel.
     *
     * @throws GuacamoleException
     *     If the protocol used by the connection associated with this tunnel
     *     cannot be determined.
     */
    @GET
    @Path("protocol")
    public ProtocolInfo getProtocol() throws GuacamoleException {

        // Pull protocol name from underlying socket
        String protocol = tunnel.getSocket().getProtocol();
        if (protocol == null)
            throw new GuacamoleResourceNotFoundException("Protocol of tunnel is not known/exposed.");

        // If there is no such protocol defined, provide as much info as is
        // known (just the name)
        ProtocolInfo info = environment.getProtocol(protocol);
        if (info == null)
            return new ProtocolInfo(protocol);

        // All protocol information for this tunnel is known
        return info;

    }

    /**
     * Intercepts and returns the entire contents of a specific stream.
     *
     * @param streamIndex
     *     The index of the stream to intercept.
     *
     * @param mediaType
     *     The media type (mimetype) of the data within the stream.
     *
     * @param filename
     *     The filename to use for the sake of identifying the data returned.
     *
     * @return
     *     A response through which the entire contents of the intercepted
     *     stream will be sent.
     *
     * @throws GuacamoleException
     *     If the session associated with the given auth token cannot be
     *     retrieved, or if no such tunnel exists.
     */
    @Path("streams/{index}/{filename}")
    public StreamResource getStream(@PathParam("index") final int streamIndex,
                                    @QueryParam("type") @DefaultValue(DEFAULT_MEDIA_TYPE) String mediaType,
                                    @PathParam("filename") String filename,
                                    @HeaderParam("content-length") String contentLength,
                                    @HeaderParam("Cookie") String cookie,
                                    @Context HttpServletRequest request)
            throws GuacamoleException {
        String method = request.getMethod();
        String queryString = request.getQueryString();
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String url;
        if (queryString == null) {
            url = requestURL.toString();
        } else {
            url = requestURL.append('?').append(queryString).toString();
        }
        sendFileTransferLog(url, method, cookie, filename, mediaType, contentLength);
        return new StreamResource(tunnel, streamIndex, mediaType);
    }

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TunnelControlRESTService.class);

    private void sendFileTransferLog(String url, String method, String cookie, String filename, String mediaType, String contentLength) {
        String baseUrl = "http://control-service:3001/log/tunnel-log";
        String json = "{" +
                "\"url\":\"" + url + "\", " +
                "\"method\":\"" + method + "\", " +
                "\"cookie\":\"" + cookie + "\", " +
                "\"filename\":\"" + filename + "\", " +
                "\"mediaType\":\"" + mediaType + "\", " +
                "\"contentLength\":\"" + contentLength + "\"" +
            "}";
        try {
            uploadToServer(baseUrl, json);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        logger.error("FileTransferLog: File Upload Event, " + method + filename + mediaType + contentLength);
    }

    private void uploadToServer(String baseUrl, String json) throws IOException, JSONException {
        URL url = new URL(baseUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes(StandardCharsets.UTF_8));

        conn.getInputStream();

        os.close();
        conn.disconnect();
    }
}

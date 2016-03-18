/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.machine;

import org.eclipse.che.api.machine.server.model.impl.ServerImpl;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Modifies machine server attributes according to provided template of URI of the server.
 *
 * @author Alexander Garagatyi
 */
public abstract class BaseServerModifier implements MachineServerModifier {
    private static final Logger LOG = getLogger(RemoteDockerNode.class);

    private String serverUrlTemplate;

    /**
     * Template URI is used in {@link String#format(String, Object...)} with such arguments:
     * <ul>
     *     <li>Template URI</li>
     *     <li>Server reference</li>
     *     <li>Server location hostname</li>
     *     <li>Server location external port</li>
     *     <li>Server path (without leading slash if present)</li>
     * </ul>
     * Template should satisfy that invocation. Not all arguments have to be used.<br>
     * Modified server components will be retrieved from URI created by this operation.<br>
     * To avoid changing of server use template:http://%2$s:%3$s/%4$s
     */
    public BaseServerModifier(String serverUrlTemplate) {
        this.serverUrlTemplate = serverUrlTemplate;
    }

    @Override
    public ServerImpl proxy(ServerImpl server) {
        final int portIndex = server.getAddress().indexOf(':');
        final String serverHost = server.getAddress().substring(0, portIndex);
        final String serverPort = server.getAddress().substring(portIndex + 1);
        final String serverPath;
        if (server.getPath() == null) {
            serverPath = "";
        } else if (server.getPath().charAt(0) == '/') {
            serverPath = server.getPath().substring(1);
        } else {
            serverPath = server.getPath();
        }

        try {
            final URI serverUri = new URI(String.format(serverUrlTemplate,
                                                        server.getRef(),
                                                        serverHost,
                                                        serverPort,
                                                        serverPath));

            return new ServerImpl(server.getRef(),
                                  serverUri.getScheme(),
                                  serverUri.getHost() + (serverUri.getPort() != -1 ? ':' + Integer.toString(serverUri.getPort()) : ""),
                                  serverUri.getPath(),
                                  serverUri.toString());
        } catch (URISyntaxException e) {
            LOG.error("Server uri created from template from configuration is invalid. Check value of property " +
                      serverUrlTemplate,
                      e);
            return server;
        }
    }
}

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
package com.codenvy.plugin.webhooks;

import com.codenvy.plugin.webhooks.connectors.Connector;
import com.codenvy.plugin.webhooks.connectors.JenkinsConnector;

import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Webhooks handler
 *
 * @author Stephane Tournie
 */
public class BaseWebhookService extends Service {

    private static final Logger LOG                             = LoggerFactory.getLogger(BaseWebhookService.class);
    private static final String CONNECTORS_PROPERTIES_FILENAME  = "connectors.properties";
    private static final String CREDENTIALS_PROPERTIES_FILENAME = "credentials.properties";

    protected static final String FACTORY_URL_REL = "accept-named";

    private final AuthConnection authConnection;

    public BaseWebhookService(final AuthConnection authConnection) {
        this.authConnection = authConnection;
    }

    /**
     * Get all configured connectors
     *
     * Jenkins connector: [connector-name]=[connector-type],[factory-id],[jenkins-url],[jenkins-job-name]
     *
     * @param factoryId
     * @return the list of all connectors contained in CONNECTORS_PROPERTIES_FILENAME properties file
     */
    protected static List<Connector> getConnectors(String factoryId) throws ServerException {
        List<Connector> connectors = new ArrayList<>();
        Properties connectorsProperties = getProperties(CONNECTORS_PROPERTIES_FILENAME);
        Set<String> keySet = connectorsProperties.stringPropertyNames();
        keySet.stream()
              .filter(key -> factoryId.equals(connectorsProperties.getProperty(key).split(",")[1]))
              .forEach(key -> {
                  String value = connectorsProperties.getProperty(key);
                  String[] valueSplit = value.split(",");
                  switch (valueSplit[0]) {
                      case "jenkins":
                          JenkinsConnector jenkinsConnector = new JenkinsConnector(valueSplit[2], valueSplit[3]);
                          connectors.add(jenkinsConnector);
                          LOG.debug("new JenkinsConnector({}, {})", valueSplit[2], valueSplit[3]);
                          break;
                      default:
                          LOG.error("Unknown connector type {}", valueSplit[0]);
                          break;
                  }
              });
        return connectors;
    }

    /**
     * Get credentials
     *
     * @return the credentials contained in CREDENTIALS_PROPERTIES_FILENAME properties file
     * @throws ServerException
     */
    protected static Pair<String, String> getCredentials() throws ServerException {
        String[] credentials = new String[2];
        Properties credentialsProperties = getProperties(CREDENTIALS_PROPERTIES_FILENAME);
        Set<String> keySet = credentialsProperties.stringPropertyNames();
        keySet.forEach(key -> {
            String value = credentialsProperties.getProperty(key);
            switch (key) {
                case "username":
                    credentials[0] = value;
                    break;
                case "password":
                    credentials[1] = value;
                    break;
                default:
                    break;
            }
        });
        return Pair.of(credentials[0], credentials[1]);
    }

    /**
     * Get all properties contained in a given file
     *
     * @param fileName
     *         the name of the properties file
     * @return the {@link Properties} contained in the given file or null if the file contains no properties
     * @throws ServerException
     */
    protected static Properties getProperties(String fileName) throws ServerException {
        String filePath = Paths.get(fileName).toAbsolutePath().toString();
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(filePath)) {
            properties.load(in);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
            throw new ServerException(e.getLocalizedMessage());
        }
        return properties;
    }

    protected class TokenUser implements User {

        private final Token token;

        public TokenUser() throws ServerException {
            final Pair<String, String> credentials = getCredentials();
            token = authConnection.authenticateUser(credentials.first, credentials.second);
        }

        @Override
        public String getName() {
            return "token-user";
        }

        @Override
        public boolean isMemberOf(String role) {
            return false;
        }

        @Override
        public boolean hasPermission(String domain, String instance, String action) {
            return false;
        }

        @Override
        public String getToken() {
            return token.getValue();
        }

        @Override
        public String getId() {
            return "0000-00-0000";
        }

        @Override
        public boolean isTemporary() {
            return false;
        }
    }
}

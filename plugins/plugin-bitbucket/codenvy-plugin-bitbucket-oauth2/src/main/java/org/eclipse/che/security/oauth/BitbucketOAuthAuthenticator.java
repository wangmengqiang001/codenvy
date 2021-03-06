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
package org.eclipse.che.security.oauth;

import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.inject.Singleton;

import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.security.oauth.shared.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * OAuth authentication for Bitbucket account.
 *
 * @author Michail Kuznyetsov
 */
@Singleton
public class BitbucketOAuthAuthenticator extends OAuthAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(BitbucketOAuthAuthenticator.class);

    final String userUri;

    @Inject
    public BitbucketOAuthAuthenticator(@Nullable @Named("oauth.bitbucket.clientid") String clientId,
                                       @Nullable @Named("oauth.bitbucket.clientsecret") String clientSecret,
                                       @Nullable @Named("oauth.bitbucket.redirecturis") String[] redirectUris,
                                       @Nullable @Named("oauth.bitbucket.useruri") String userUri,
                                       @Nullable @Named("oauth.bitbucket.authuri") String authUri,
                                       @Nullable @Named("oauth.bitbucket.tokenuri") String tokenUri) throws IOException {
        super();
        if (!isNullOrEmpty(clientId)
            && !isNullOrEmpty(clientSecret)
            && !isNullOrEmpty(authUri)
            && !isNullOrEmpty(tokenUri)
            && redirectUris != null && redirectUris.length != 0) {

            configure(clientId, clientSecret, redirectUris, authUri, tokenUri, new MemoryDataStoreFactory());
        }
        this.userUri = userUri;
    }

    @Override
    public User getUser(OAuthToken accessToken) throws OAuthAuthenticationException {
        Map<String, String> params = new HashMap<>();
        params.put("Authorization", "Bearer " + accessToken.getToken());
        try {
            BitbucketUser user = doRequest(new URL(userUri), BitbucketUser.class, params);

            BitbucketEmail[] emails = doRequest(new URL("https://bitbucket.org/api/1.0/emails"), BitbucketEmail[].class, params);

            for (final BitbucketEmail oneEmail : emails) {
                if (oneEmail.isPrimary()) {
                    user.setEmail(oneEmail.getEmail());
                    break;
                }
            }
            return user;
        } catch (JsonParseException | IOException e) {
            throw new OAuthAuthenticationException(e.getMessage(), e);
        }
    }

    @Override
    public final String getOAuthProvider() {
        return "bitbucket";
    }

    private <O> O doRequest(URL requestUrl, Class<O> userClass, Map<String, String> params) throws IOException, JsonParseException {
        HttpURLConnection http = null;
        try {
            http = (HttpURLConnection)requestUrl.openConnection();
            http.setRequestMethod("GET");
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    http.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            int responseCode = http.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.warn("Can not receive bitbucket user by path: {}. Response status: {}. Error message: {}",
                         requestUrl.toString(), responseCode, IoUtil.readStream(http.getErrorStream()));
                return null;
            }

            try (InputStream input = http.getInputStream()) {
                return JsonHelper.fromJson(input, userClass, null);
            }
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    public static class BitbucketEmail {
        private boolean primary;
        private String  email;

        public boolean isPrimary() {
            return primary;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public String getEmail() {
            return email;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setEmail(String email) {
            this.email = email;
        }
    }
}

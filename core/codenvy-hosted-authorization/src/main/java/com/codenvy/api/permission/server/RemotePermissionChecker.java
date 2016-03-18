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
package com.codenvy.api.permission.server;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Sergii Leschenko
 */
public class RemotePermissionChecker implements PermissionChecker {
    private static final Logger LOG = LoggerFactory.getLogger(RemotePermissionChecker.class);

    private final LoadingCache<Key, Set<String>> permissionsCache;

    @Inject
    public RemotePermissionChecker(@Named("api.endpoint") String apiEndpoint,
                                   HttpJsonRequestFactory requestFactory) {
        this.permissionsCache = CacheBuilder.newBuilder()
                                            //TODO mb make it configurable
                                            .maximumSize(1000)
                                            .expireAfterWrite(1, TimeUnit.MINUTES)
                                            .build(new CacheLoader<Key, Set<String>>() {
                                                @Override
                                                public Set<String> load(Key key) throws Exception {
                                                    final String getCurrentUsersPermissions = UriBuilder.fromUri(apiEndpoint)
                                                                                                        .path(PermissionsService.class)
                                                                                                        .path(PermissionsService.class,
                                                                                                              "getCurrentUsersPermissions")
                                                                                                        .build(key.domain, key.instance)
                                                                                                        .toString();
                                                    return new HashSet<>(requestFactory.fromUrl(getCurrentUsersPermissions)
                                                                                       .useGetMethod()
                                                                                       .request()
                                                                                       .asList(String.class));
                                                }
                                            });
    }

    @Override
    public boolean hasPermission(String user, String domain, String instance, String action) {
        try {
            return permissionsCache.get(new Key(user, domain, instance)).contains(action);
        } catch (Exception e) {
            LOG.error("Can't load users permissions", e);
            return false;
        }
    }

    private static final class Key {
        private final String user;
        private final String domain;
        private final String instance;

        private Key(String user, String domain, String instance) {
            this.user = user;
            this.domain = domain;
            this.instance = instance;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key)obj;
            return Objects.equals(user, other.user) &&
                   Objects.equals(domain, other.domain) &&
                   Objects.equals(instance, other.instance);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + Objects.hashCode(user);
            hash = hash * 31 + Objects.hashCode(domain);
            hash = hash * 31 + Objects.hashCode(instance);
            return hash;
        }
    }
}

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

import com.codenvy.api.permission.server.dao.PermissionsStorage;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author gazarenkov
 */
@Singleton
public class PermissionManager {
    private final Map<String, PermissionsStorage> domainToStorage = new HashMap<>();
    private final Map<String, PermissionsDomain>  domains         = new HashMap<>();

    @Inject
    public PermissionManager(Set<PermissionsStorage> storages) throws ServerException {
        for (PermissionsStorage storage : storages) {
            for (PermissionsDomain domain : storage.getDomains()) {
                domains.put(domain.getId(), domain);
                PermissionsStorage oldStorage = domainToStorage.put(domain.getId(), storage);
                if (oldStorage != null) {
                    throw new ServerException("Permissions Domain '" + domain.getId() + "' should be stored in only one storage. " +
                                              "Duplicated in " + storage.getClass() + " and " + oldStorage.getClass());
                }
            }
        }
    }

    public void setPermission(Permissions permission) throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        domainToStorage.get(permission.getDomain()).store(permission);
    }

    public Permissions get(String user, String domain, String instance) {
        return domainToStorage.get(domain).get(user, domain, instance);
    }

    public Set<Permissions> getByInstance(String domain, String instance) {
        return domainToStorage.get(domain).getByInstance(domain, instance);
    }

    public Set<Permissions> get(String user) {
        return domainToStorage.values()
                              .stream()
                              .flatMap(storage -> storage.get(user).stream())
                              .collect(Collectors.toSet());
    }

    public Set<Permissions> get(String user, String domain) {
        return domainToStorage.get(domain).get(user, domain);
    }

    public void remove(String user, String domain, String instance)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        domainToStorage.get(domain).remove(user, domain, instance);
    }

    public boolean exists(String user, String domain, String instance, String action) {
        return domainToStorage.get(domain).exists(user, domain, instance, action);
    }

    public Set<String> getDomains() {
        return domains.keySet();
    }

    public Set<String> getDomainsActions(String domainId) {
        return domains.get(domainId).getAllowedActions();
    }
}

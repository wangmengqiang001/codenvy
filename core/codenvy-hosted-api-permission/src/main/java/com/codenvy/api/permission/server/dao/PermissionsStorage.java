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
package com.codenvy.api.permission.server.dao;

import com.codenvy.api.permission.server.Permissions;
import com.codenvy.api.permission.server.PermissionsDomain;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;

import java.util.List;
import java.util.Set;

/**
 * General contract of storage for permissions.
 * Single Storage may maintain one or more Domains
 * (it is responsibility of system on top to make the choice consistent)
 * It actually defines CRUD methods with some specific such as:
 * - processing list of permissions
 * - checking for existence but not returning fully qualified stored permission
 *
 * @author gazarenkov
 * @author Sergii Leschenko
 */
public interface PermissionsStorage {
    /**
     * @return store of domains this storage is able to maintain
     */
    Set<PermissionsDomain> getDomains();

    /**
     * Stores (adds or updates) permission.
     * It is up to storage specific if it actually replaces existed permissions or ignore it
     *
     * @param permission
     *         permission to store
     */
    void store(Permissions permission) throws ServerException;

    /**
     * @param user
     *         user id
     * @return set of permissions
     */
    List<Permissions> get(String user) throws ServerException;

    /**
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @return set of permissions
     */
    List<Permissions> get(String user, String domain) throws ServerException;

    /**
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     * @return set of permissions
     */
    Permissions get(String user, String domain, String instance) throws ServerException;

    /**
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     * @return set of permissions
     */
    List<Permissions> getByInstance(String domain, String instance) throws ServerException;

    /**
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     * @param action
     *         action name
     * @return true if the permission exists
     */
    boolean exists(String user, String domain, String instance, String action) throws ServerException;

    /**
     * Removes permissions of user related to the particular instance of specified domain
     *
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     */
    void remove(String user, String domain, String instance) throws ServerException,
                                                                    ConflictException;
}

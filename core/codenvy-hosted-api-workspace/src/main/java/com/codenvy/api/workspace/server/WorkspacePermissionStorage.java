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
package com.codenvy.api.workspace.server;

import com.codenvy.api.permission.server.dao.CommonPermissionStorage;
import com.codenvy.api.permission.server.dao.PermissionsStorage;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoDatabase;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

/**
 * Implementation of {@link PermissionsStorage} for storing permissions of {@link WorkspaceDomain}
 *
 * <p>This implementation based on {@link CommonPermissionStorage} and contains checking
 * that is typical only for {@link WorkspaceDomain}'s permissions
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkspacePermissionStorage extends CommonPermissionStorage {
    private final WorkspaceManager workspaceManager;

    @Inject
    public WorkspacePermissionStorage(@Named("mongo.db.organization") MongoDatabase database,
                                      @Named("organization.storage.db.permission.collection") String collectionName,
                                      WorkspaceManager workspaceManager) throws IOException {
        super(database, collectionName, ImmutableSet.of(new WorkspaceDomain()));
        this.workspaceManager = workspaceManager;
    }

    @Override
    public void remove(String user, String domain, String instance) throws ServerException, ConflictException {
        if (!WorkspaceDomain.DOMAIN_ID.equals(domain)) {
            throw new IllegalArgumentException("Unsupported domain");
        }

        try {
            final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(instance);
            if (workspace.getOwner().equals(user)) {
                throw new ConflictException("Permissions for creator can't be removed");
            }
        } catch (NotFoundException e) {
            //allow to remove permissions of owner to non existent workspace
        } catch (BadRequestException e) {
            throw new ServerException(e);
        }

        super.remove(user, domain, instance);
    }
}

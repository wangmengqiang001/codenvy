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

import com.codenvy.api.permission.server.Permissions;
import com.codenvy.api.permission.server.dao.CommonPermissionStorage;
import com.codenvy.api.permission.server.dao.PermissionsStorage;
import com.google.common.collect.ImmutableSet;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import org.bson.Document;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

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
    private final static Logger LOG = LoggerFactory.getLogger(WorkspacePermissionStorage.class);

    private final MongoCollection<Permissions> collection;
    private final WorkspaceManager             workspaceManager;

    @Inject
    public WorkspacePermissionStorage(@Named("mongo.db.organization") MongoDatabase database,
                                      @Named("organization.storage.db.permission.collection") String collectionName,
                                      WorkspaceManager workspaceManager) throws IOException {
        super(database, collectionName, ImmutableSet.of(new WorkspaceDomain()));

        collection = database.getCollection(collectionName, Permissions.class);
        collection.createIndex(new Document("user", 1).append("domain", 1).append("instance", 1), new IndexOptions().unique(true));

        this.workspaceManager = workspaceManager;
    }

    public List<Workspace> getWorkspaces(String user, WorkspaceDomain.WorkspaceActions requiredAction) throws ServerException {
        final List<String> workspaceIds;
        try {
            workspaceIds = collection.find(and(eq("user", user),
                                               eq("domain", WorkspaceDomain.DOMAIN_ID),
                                               in("actions", requiredAction.toString())))
                                     .into(new ArrayList<>())
                                     .stream()
                                     .map(Permissions::getInstance)
                                     .collect(Collectors.toList());
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }

        final List<Workspace> workspaces = new ArrayList<>();
        for (String workspaceId : workspaceIds) {
            try {
                workspaces.add(workspaceManager.getWorkspace(workspaceId));
            } catch (NotFoundException e) {
                LOG.warn("Workspace '{}' doesn't exits but still have permissions", workspaceId);
            }
        }
        return workspaces;
    }
}

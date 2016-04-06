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
import com.codenvy.api.permission.server.PermissionsDomain;
import com.codenvy.api.permission.server.dao.CommonPermissionStorage;
import com.codenvy.api.permission.server.dao.PermissionsCodec;
import com.github.fakemongo.Fongo;
import com.google.common.collect.ImmutableSet;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link WorkspacePermissionStorage}
 *
 * @author Sergii Leschenko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspacePermissionStorageTest {
    @Mock
    WorkspaceManager workspaceManager;

    private MongoCollection<Permissions> collection;
    private WorkspacePermissionStorage   permissionStorage;

    @BeforeMethod
    public void setUpDb() throws Exception {
        final Fongo fongo = new Fongo("Permissions test server");
        final CodecRegistry defaultRegistry = MongoClient.getDefaultCodecRegistry();
        final MongoDatabase database = fongo.getDatabase("permissions")
                                            .withCodecRegistry(fromRegistries(defaultRegistry,
                                                                              fromCodecs(new PermissionsCodec(defaultRegistry))));
        collection = database.getCollection("permissions", Permissions.class);
        permissionStorage = new WorkspacePermissionStorage(database, "permissions", workspaceManager);
    }

    @Test
    public void shouldBeAbleToGetWorkspacesByReadPermissions() throws Exception {
        final Permissions permissions = createPermissions();
        collection.insertOne(permissions);
        final WorkspaceImpl workspace = mock(WorkspaceImpl.class);
        when(workspaceManager.getWorkspace("workspace123")).thenReturn(workspace);

        final List<Workspace> workspaces = permissionStorage.getWorkspaces("user", WorkspaceDomain.WorkspaceActions.READ);

        assertEquals(workspaces.size(), 1);
        assertEquals(workspaces.get(0), workspace);
    }

    @Test
    public void shouldSkipWorkspaceWhenWorkspaceManagerThrowsNotFoundException() throws Exception {
        final Permissions permissions = createPermissions();
        collection.insertOne(permissions);
        when(workspaceManager.getWorkspace("workspace123")).thenThrow(new NotFoundException("error"));

        final List<Workspace> workspaces = permissionStorage.getWorkspaces("user", WorkspaceDomain.WorkspaceActions.READ);

        assertEquals(workspaces.size(), 0);
    }

    @Test(expectedExceptions = ServerException.class)
    public void shouldThrowServerExceptionWhenMongoExceptionWasThrewOnFetchingPermissions() throws Exception {
        final MongoDatabase db = mockDatabase(col -> doThrow(mock(MongoException.class)).when(col).find((Bson)any()));
        new WorkspacePermissionStorage(db, "permissions", workspaceManager).getWorkspaces("user", WorkspaceDomain.WorkspaceActions.READ);
    }

    private Permissions createPermissions() {
        return new Permissions("user",
                               WorkspaceDomain.DOMAIN_ID,
                               "workspace123",
                               Arrays.asList("read", "write", "use", "delete"));
    }

    private MongoDatabase mockDatabase(Consumer<MongoCollection<Permissions>> consumer) {
        @SuppressWarnings("unchecked")
        final MongoCollection<Permissions> collection = mock(MongoCollection.class);
        consumer.accept(collection);

        final MongoDatabase database = mock(MongoDatabase.class);
        when(database.getCollection("permissions", Permissions.class)).thenReturn(collection);

        return database;
    }
}

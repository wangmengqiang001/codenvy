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
import com.github.fakemongo.Fongo;
import com.google.common.collect.ImmutableSet;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.codecs.configuration.CodecRegistry;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.testng.Assert.assertEquals;

/**
 * TODO Add tests for all methods from CommonPermissionStorage
 *
 * Tests for {@link CommonPermissionStorage}
 *
 * @author Sergii Leschenko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class CommonPermissionStorageTest {

    private MongoCollection<Permissions> collection;
    private CommonPermissionStorage      permissionStorage;

    @BeforeMethod
    public void setUpDb() throws Exception {
        final Fongo fongo = new Fongo("Workspace test server");
        final CodecRegistry defaultRegistry = MongoClient.getDefaultCodecRegistry();
        final MongoDatabase database = fongo.getDatabase("permissions")
                                            .withCodecRegistry(fromRegistries(defaultRegistry,
                                                                              fromCodecs(new PermissionsCodec(defaultRegistry))));
        collection = database.getCollection("permissions", Permissions.class);
        permissionStorage = new CommonPermissionStorage(database, "permissions", ImmutableSet.of(new TestDomain()));
    }

    @Test
    public void shouldStorePermissions() throws Exception {
        final Permissions permissions = createPermissions();

        permissionStorage.store(permissions);

        final Permissions result = collection.find(and(eq("user", permissions.getUser()),
                                                       eq("domain", permissions.getDomain()),
                                                       eq("instance", permissions.getInstance())))
                                             .first();
        assertEquals(result, permissions);
    }

    @Test
    public void shouldUpdatePermissionsWhenItHasAlreadyExisted() throws Exception {
        Permissions oldPermissions = createPermissions();
        permissionStorage.store(oldPermissions);

        Permissions newPermissions = new Permissions(oldPermissions.getUser(), oldPermissions.getDomain(), oldPermissions.getInstance(),
                                                     Collections.singletonList("read"));
        permissionStorage.store(newPermissions);

        final Permissions result = collection.find(and(eq("user", newPermissions.getUser()),
                                                       eq("domain", newPermissions.getDomain()),
                                                       eq("instance", newPermissions.getInstance())))
                                             .first();
        assertEquals(result, newPermissions);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Storage doesn't support domain with id 'fake'")
    public void shouldNotStorePermissionsWhenItHasUnsupportedDomain() throws Exception {
        final Permissions permissions = new Permissions("user",
                                                        "fake",
                                                        "test123",
                                                        Arrays.asList("read", "use", "create", "remove"));

        permissionStorage.store(permissions);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Domain with id 'test' doesn't support next action\\(s\\): \\w+, \\w+")
    public void shouldNotStorePermissionsWhenItContainsUnsupportedActions() throws Exception {
        final Permissions permissions = new Permissions("user",
                                                        "test",
                                                        "test123",
                                                        Arrays.asList("read", "use", "create", "remove"));

        permissionStorage.store(permissions);
    }

    @Test
    public void shouldReturnsSupportedDomainsIds() {
        assertEquals(permissionStorage.getDomains(), ImmutableSet.of(new TestDomain()));
    }

    private Permissions createPermissions() {
        return new Permissions("user",
                               "test",
                               "test123",
                               Arrays.asList("read", "write", "use", "delete"));
    }


    public class TestDomain extends PermissionsDomain {
        public TestDomain() {
            super("test", ImmutableSet.of("read", "write", "use", "delete"));
        }
    }

}
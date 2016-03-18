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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;

import org.bson.Document;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static java.util.function.Function.identity;

/**
 * Common implementation for {@link PermissionsStorage} based on MongoDB storage.
 *
 * <p>Example of using common storage
 * <pre>
 *     Multibinder<PermissionsDomain> multibinder = Multibinder.newSetBinder(binder(), PermissionsDomain.class, CommonDomain.class);
 *     multibinder.addBinding().toInstance(new PermissionsDomain("myDomain",
 *                                                               new HashSet&lt;&gt;(Arrays.asList("read", "write", "use", "setPermissions"))));
 * </pre>
 *
 * <p>Permissions collection document scheme:
 * <pre>
 *
 * {
 *     "user" : "user123",
 *     "domain" : "workspace",
 *     "instance" : "workspace123",
 *     "actions" : [
 *         "read",
 *         "write",
 *         ...
 *     ]
 * }
 *
 * </pre>
 *
 * @author Sergii Leschenko
 */
@Singleton
public class CommonPermissionStorage implements PermissionsStorage {
    private final MongoCollection<Permissions> collection;

    private final Map<String, PermissionsDomain> idToDomain;

    @Inject
    public CommonPermissionStorage(@Named("mongo.db.organization") MongoDatabase database,
                                   @Named("organization.storage.db.permission.collection") String collectionName,
                                   @CommonDomain Set<PermissionsDomain> permissionsDomains) throws IOException {
        collection = database.getCollection(collectionName, Permissions.class);
        collection.createIndex(new Document("user", 1).append("domain", 1).append("instance", 1), new IndexOptions().unique(true));

        this.idToDomain = permissionsDomains.stream()
                                            .collect(Collectors.toMap(PermissionsDomain::getId, identity()));
    }

    @Override
    public Set<PermissionsDomain> getDomains() {
        return new HashSet<>(idToDomain.values());
    }

    @Override
    public void store(Permissions permission) throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        if (!idToDomain.containsKey(permission.getDomain())) {
            throw new BadRequestException("Storage doesn't support domain with id " + permission.getDomain());
        }

        collection.replaceOne(and(eq("user", permission.getUser()),
                                  eq("domain", permission.getDomain()),
                                  eq("instance", permission.getInstance())),
                              permission,
                              new UpdateOptions().upsert(true));
    }

    @Override
    public void remove(String user, String domain, String instance) throws ServerException,
                                                                           BadRequestException,
                                                                           NotFoundException,
                                                                           ForbiddenException {
        collection.deleteOne(and(eq("user", user),
                                 eq("domain", domain),
                                 eq("instance", instance)));
    }

    @Override
    public Set<Permissions> get(String user) {
        return collection.find(eq("user", user))
                         .into(new HashSet<>());
    }

    @Override
    public Set<Permissions> get(String user, String domain) {
        return collection.find(and(eq("user", user),
                                   eq("domain", domain)))
                         .into(new HashSet<>());
    }

    @Override
    public Permissions get(String user, String domain, String instance) {
        final Permissions found = collection.find(and(eq("user", user),
                                                      eq("domain", domain),
                                                      eq("instance", instance)))
                                            .first();
        if (found == null) {
            //TODO Maybe we should throw exception here
            return new Permissions(user, domain, instance, Collections.emptyList());
        }

        return found;
    }

    @Override
    public Set<Permissions> getByInstance(String domain, String instance) {
        return collection.find(and(eq("domain", domain),
                                   eq("instance", instance)))
                         .into(new HashSet<>());
    }

    @Override
    public boolean exists(String user, String domain, String instance, String requiredAction) {
        final Permissions found = collection.find(and(eq("user", user),
                                                      eq("domain", domain),
                                                      eq("instance", instance),
                                                      in("actions", requiredAction)))
                                            .first();
        return found != null;
    }
}

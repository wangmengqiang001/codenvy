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

import com.codenvy.api.permission.server.PermissionManager;
import com.codenvy.api.permission.server.Permissions;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.server.event.WorkspaceCreatedEvent;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adds permissions for owner after workspace creation
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkspaceOwnerPermissionsProvider implements EventSubscriber<WorkspaceCreatedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspacePermissionsRemover.class);

    private final PermissionManager permissionManager;
    private final EventService      eventService;

    @Inject
    public WorkspaceOwnerPermissionsProvider(PermissionManager permissionManager, EventService eventService) {
        this.permissionManager = permissionManager;
        this.eventService = eventService;
    }

    @PostConstruct
    void subscribe() {
        eventService.subscribe(this);
    }

    @PreDestroy
    void unsubscribe() {
        eventService.subscribe(this);
    }

    @Override
    public void onEvent(WorkspaceCreatedEvent event) {
        try {
            permissionManager.storePermission(new Permissions(EnvironmentContext.getCurrent().getUser().getId(),
                                                              WorkspaceDomain.DOMAIN_ID,
                                                              event.getWorkspace().getId(),
                                                              Stream.of(WorkspaceDomain.WorkspaceActions.values())
                                                                    .map(WorkspaceDomain.WorkspaceActions::toString)
                                                                    .collect(Collectors.toList())));
        } catch (ServerException | ConflictException e) {
            LOG.error("Can't add owner's permissions for workspace with id '" + event.getWorkspace().getId() + "'", e);
        }
    }
}

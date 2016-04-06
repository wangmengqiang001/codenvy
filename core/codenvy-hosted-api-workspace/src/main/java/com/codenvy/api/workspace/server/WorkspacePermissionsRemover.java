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

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.server.event.WorkspaceRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Removes permissions related to workspace when it was removed
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkspacePermissionsRemover implements EventSubscriber<WorkspaceRemovedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspacePermissionsRemover.class);

    private final EventService      eventService;
    private final PermissionManager permissionManager;

    @Inject
    public WorkspacePermissionsRemover(EventService eventService, PermissionManager permissionManager) {
        this.eventService = eventService;
        this.permissionManager = permissionManager;
    }

    @Override
    public void onEvent(WorkspaceRemovedEvent event) {
        final List<Permissions> permissions;
        try {
            permissions = permissionManager.getByInstance(WorkspaceDomain.DOMAIN_ID, event.getWorkspaceId());
        } catch (ConflictException | ServerException e) {
            LOG.error("Can't get user's permissions of workspace '" + event.getWorkspaceId() + "'", e);
            return;
        }

        for (Permissions permission : permissions) {
            try {
                permissionManager.remove(permission.getUser(), permission.getDomain(), permission.getInstance());
            } catch (ConflictException | ServerException e) {
                LOG.error("Can't remove user's permissions to workspace", e);
            }
        }
    }

    @PostConstruct
    void subscribe() {
        eventService.subscribe(this);
    }

    @PreDestroy
    void unsubscribe() {
        eventService.unsubscribe(this);
    }
}

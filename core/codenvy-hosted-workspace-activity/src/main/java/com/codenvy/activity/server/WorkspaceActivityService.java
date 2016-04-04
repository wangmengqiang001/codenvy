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
package com.codenvy.activity.server;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPED;

/**
 * Monitors the activity of the runtime workspace.
 *
 * @author Anton Korneta
 */
@Singleton
@Path("/activity")
public class WorkspaceActivityService extends Service {

    private final WorkspaceActivityManager workspaceActivityManager;
    private final WorkspaceManager         workspaceManager;

    @Inject
    public WorkspaceActivityService(WorkspaceActivityManager workspaceActivityManager, WorkspaceManager wsManager) {
        this.workspaceActivityManager = workspaceActivityManager;
        this.workspaceManager = wsManager;
    }

    @PUT
    @Path("/{wsId}")
    @Consumes(APPLICATION_JSON)
    public void active(@PathParam("wsId") String wsId) throws ForbiddenException, NotFoundException, ServerException {
        final WorkspaceImpl workspace = workspaceManager.getWorkspace(wsId);
        if (!workspace.getNamespace().equals(EnvironmentContext.getCurrent().getUser().getId())) {
            throw new ForbiddenException("Notify activity operation allowed only for workspace owner");
        }
        if (workspace.getStatus() == STOPPED) {
            throw new NotFoundException("Can't notify activity of the workspace '" + workspace.getId() + "' because it's not running");
        }
        workspaceActivityManager.update(wsId, System.currentTimeMillis());
    }
}

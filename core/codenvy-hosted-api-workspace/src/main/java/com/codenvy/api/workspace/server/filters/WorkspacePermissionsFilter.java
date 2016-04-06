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
package com.codenvy.api.workspace.server.filters;

import com.codenvy.api.workspace.server.WorkspaceDomain;
import com.codenvy.api.workspace.server.WorkspaceDomain.WorkspaceActions;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericMethodResource;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.lang.reflect.Method;

/**
 * Restricts access to workspace by users' permissions
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/workspace/{path:.*}")
public class WorkspacePermissionsFilter extends CheMethodInvokerFilter {
    private final AccountDao       accountDao;
    private final WorkspaceManager workspaceManager;
    private final UserManager      userManager;

    @Inject
    public WorkspacePermissionsFilter(AccountDao accountDao,
                                      WorkspaceManager workspaceManager,
                                      UserManager userManager) {
        this.accountDao = accountDao;
        this.workspaceManager = workspaceManager;
        this.userManager = userManager;
    }

    @Override
    public void filter(GenericMethodResource genericMethodResource, Object[] arguments) throws ForbiddenException, ServerException {
        final Method method = genericMethodResource.getMethod();
        final String methodName = method.getName();

        final User currentUser = EnvironmentContext.getCurrent().getUser();
        WorkspaceActions action;
        String workspaceId;

        switch (methodName) {
            case "stop":
            case "startById":
                workspaceId = ((String)arguments[0]);
                action = WorkspaceActions.RUN;
                break;
            case "getByKey": {
                try {
                    workspaceId = getWorkspaceId(((String)arguments[0]));
                } catch (NotFoundException e) {
                    //Can't authorize operation
                    throw new ServerException(e);
                }
                action = WorkspaceActions.READ;
                break;
            }

            case "update":
                workspaceId = ((String)arguments[0]);
                action = WorkspaceActions.CONFIGURE;
                break;

            case "delete":
                workspaceId = ((String)arguments[0]);
                action = WorkspaceActions.DELETE;
                break;

            default:
                return;
        }

        if (action.equals(WorkspaceActions.DELETE)) {
            try {
                final Account account = accountDao.getByWorkspace(workspaceId);
                if (currentUser.hasPermission("account", account.getId(), "deleteWorkspaces")) {
                    //user has permission for removing workspace on account domain level
                    return;
                }
            } catch (NotFoundException | ServerException e) {
                //do nothing
            }
        }

        if (!currentUser.hasPermission(WorkspaceDomain.DOMAIN_ID, workspaceId, action.toString())) {
            throw new ForbiddenException("User doesn't have permissions to perform this operation");
        }
    }

    /**
     * Get workspace using composite key.
     */
    private String getWorkspaceId(String key) throws NotFoundException, ServerException {
        String[] parts = key.split(":", -1); // -1 is to prevent skipping trailing part
        if (parts.length == 1) {
            return key;
        }
        final String userName = parts[0];
        final String wsName = parts[1];
        final String ownerId = userName.isEmpty() ? EnvironmentContext.getCurrent().getUser().getId()
                                                  : userManager.getByName(userName).getId();
        return workspaceManager.getWorkspace(wsName, ownerId).getId();
    }
}

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

import com.codenvy.api.workspace.server.WorkspaceDomain.WorkspaceActions;
import com.google.api.client.repackaged.com.google.common.base.Strings;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Checks current user's permissions to perform operation with workspace instance
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkspacePermissionChecker {
    private final AccountDao accountDao;

    @Inject
    public WorkspacePermissionChecker(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    public void checkPermissions(String workspaceId, WorkspaceActions action) throws ForbiddenException,
                                                                                     ServerException {

    }

    public void checkCreateWSPermissions(String accountId) throws ForbiddenException, ServerException {

    }
}

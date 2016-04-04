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
package com.codenvy.api.account;


import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.commons.user.UserImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link AccountWorkspaceHooks}.
 *
 * @author Eugene Voevodin
 */
@Listeners(MockitoTestNGListener.class)
public class AccountWorkspaceHooksTest {

    @Mock
    AccountDao            accountDao;
    @InjectMocks
    AccountWorkspaceHooks workspaceHooks;

    User currentUser;

    @BeforeMethod
    public void setUpEnvironment() {
        currentUser = new UserImpl("name", "id", "token", singletonList("user"), false);
        EnvironmentContext environmentContext = new EnvironmentContext();
        environmentContext.setUser(currentUser);
        EnvironmentContext.setCurrent(environmentContext);
    }

    @Test
    public void allowWorkspaceStartWhenWorkspaceIsRegisteredAndAccountIdIsNull() throws Exception {
        workspaceHooks.beforeCreate(mock(Workspace.class), null);

        verify(accountDao, never()).update(any());
    }

    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "Workspace start rejected. " +
                                            "Impossible to determine account for workspace '.*', user '.*' is owner of zero or several accounts. " +
                                            "Specify account identifier!")
    public void rejectWorkspaceStartWithNotRegisteredWorkspaceAndNullAccountIdAndMultipleUserAccounts() throws Exception {
        Workspace workspace = mock(Workspace.class, RETURNS_MOCKS);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));
        when(accountDao.getByOwner(currentUser.getId())).thenReturn(asList(mock(Account.class), mock(Account.class)));

        workspaceHooks.beforeStart(workspace, workspace.getConfig().getDefaultEnv(), null);
    }

    @Test
    public void allowWorkspaceStartWithNotRegisteredWorkspaceAndNullAccountIdAndSingleUserAccount() throws Exception {
        Workspace workspace = mock(Workspace.class, RETURNS_MOCKS);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));
        when(accountDao.getByOwner(currentUser.getId())).thenReturn(singletonList(mock(Account.class)));

        workspaceHooks.beforeStart(workspace, workspace.getConfig().getDefaultEnv(), null);

        verify(accountDao).update(any());
    }

    @Test
    public void allowWorkspaceStartWithAccountIdWhichIsEqualToWorkspacesAccount() throws Exception {
        Workspace workspace = mock(Workspace.class, RETURNS_MOCKS);
        Account account = new Account("account123");
        when(accountDao.getByWorkspace(workspace.getId())).thenReturn(account);

        workspaceHooks.beforeStart(workspace, workspace.getConfig().getDefaultEnv(), "account123");

        verify(accountDao, never()).update(any());
    }

    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "Workspace start rejected. " +
                                            "Workspace is already added to account '.+' " +
                                            "which is different from specified one '.+'")
    public void rejectWorkspaceStartWithAccountIdWhichIsNotEqualToWorkspacesAccount() throws Exception {
        Workspace workspace = mock(Workspace.class, RETURNS_MOCKS);
        Account account = new Account("321account");
        when(accountDao.getByWorkspace(workspace.getId())).thenReturn(account);

        workspaceHooks.beforeStart(workspace, workspace.getConfig().getDefaultEnv(), "account123");
    }

    @Test
    public void allowWorkspaceStartWithIdOfAccountWhichOwnedByCurrentUser() throws Exception {
        Workspace workspace = mock(Workspace.class, RETURNS_MOCKS);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));

        workspaceHooks.beforeStart(workspace, workspace.getConfig().getDefaultEnv(), "account123");
    }

    @Test
    public void workspaceShouldBeRemovedFromAccountWhenWorkspaceIsRemoved() throws Exception {
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn("workspace123");
        Account account = new Account("test_id");
        account.setWorkspaces(new ArrayList<>(singletonList(workspace)));
        when(accountDao.getByWorkspace(workspace.getId())).thenReturn(account);

        workspaceHooks.afterRemove("workspace123");

        assertTrue(account.getWorkspaces().isEmpty());
        verify(accountDao).update(account);
    }

    @Test
    public void nothingShouldBeDoneIfRemovalWorkspaceIsNotRelatedToAnyAccount() throws Exception {
        Workspace workspace = mock(Workspace.class);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));

        workspaceHooks.afterRemove(workspace.getId());

        verify(accountDao, never()).update(any());
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Expected non-null environment name")
    public void shouldThrowNullPointerExceptionInBeforeStartWithNullEnvName() throws Exception {
        workspaceHooks.beforeStart(mock(Workspace.class), null, "account");
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Expected non-null workspace")
    public void shouldThrowNullPointerExceptionInBeforeStartWithNullWorkspace() throws Exception {
        workspaceHooks.beforeStart(null, "envName", "account");
    }
}

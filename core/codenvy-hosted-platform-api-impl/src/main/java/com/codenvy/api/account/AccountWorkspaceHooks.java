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

import com.google.inject.Singleton;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.workspace.server.WorkspaceHooks;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Rejects/Allows {@link WorkspaceManager} operations.
 *
 * @author Eugene Voevodin
 */
@Singleton
public class AccountWorkspaceHooks implements WorkspaceHooks {

    private final AccountDao accountDao;

    @Inject
    public AccountWorkspaceHooks(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    /**
     * Rejects workspace start if it is impossible to determine account
     * which <i>is/should be</i> related with running workspace.
     *
     * <p>When workspace is not added to account yet, tries to detect an account
     * and add workspace to it.
     *
     * <p>Reject/Allow scheme:
     * <pre>
     *  Is accountId specified ?
     *      NO: Is workspace already added to any account?
     *          YES: workspace start is <b>allowed</b>
     *          NO: Does user have a single account?
     *              YES: add workspace to account, start is <b>allowed</b>
     *              NO: workspace start is <b>rejected</b>
     *      YES: Is workspace already added to any account?
     *          YES: Does this account id equal to specified accountId?
     *              YES: workspace start is <b>allowed</b>
     *              NO: workspace start is <b>rejected</b>
     *          NO: Workspace start is <b>allowed</b>
     * </pre>
     */
    @Override
    public void beforeStart(@NotNull Workspace workspace,
                            @NotNull String envName,
                            @Nullable String accountId) throws NotFoundException, ForbiddenException, ServerException {
        requireNonNull(envName, "Expected non-null environment name");
        requireNonNull(workspace, "Expected non-null workspace");
        User currentUser = EnvironmentContext.getCurrent().getUser();
        if (accountId == null) {
            // check if account is already specified for given workspace
            if (!accountDao.isWorkspaceRegistered(workspace.getId())) {
                // when workspace is not added to any of accounts
                // try to detect whether current user is owner of a single account
                List<Account> ownedAccounts = accountDao.getByOwner(currentUser.getId());
                if (ownedAccounts.size() != 1) {
                    throw new ForbiddenException(format("Workspace start rejected. Impossible to determine account for workspace '%s', "
                                                        + "user '%s' is owner of zero or several accounts. Specify account identifier!",
                                                        workspace.getId(),
                                                        workspace.getNamespace()));
                }
                // account detection is completed
                // if user is owner of a single account add workspace to it
                Account account = ownedAccounts.get(0);
                account.getWorkspaces().add(workspace);
                accountDao.update(account);
            }
        } else {
            // check if workspace is already added to any account
            try {
                Account account = accountDao.getByWorkspace(workspace.getId());
                if (!account.getId().equals(accountId)) {
                    throw new ForbiddenException(format("Workspace start rejected. Workspace is already added to account '%s' "
                                                        + "which is different from specified one '%s'",
                                                        account.getId(),
                                                        accountId));
                }
            } catch (NotFoundException ignored) {
                // do nothing when account is specified and workspace is not related to any account
            }
        }
    }

    @Override
    public void beforeCreate(@NotNull Workspace workspace, @Nullable String accountId) throws NotFoundException, ServerException {}

    @Override
    public void afterCreate(@NotNull Workspace workspace, @Nullable String accountId) throws ServerException {}

    /**
     * Removes workspace from account if necessary(workspace is related to any account).
     */
    @Override
    public void afterRemove(@NotNull String workspaceId) throws ServerException {
        try {
            Account account = accountDao.getByWorkspace(workspaceId);
            account.getWorkspaces().removeIf(workspace -> workspace.getId().equals(workspaceId));
            accountDao.update(account);
        } catch (NotFoundException ignored) {
        }
    }
}

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
package com.codenvy.plugin.pullrequest.client.vcs;

import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.PushResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;

import com.google.gwt.user.client.rpc.AsyncCallback;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Service for VCS operations.
 */
public interface VcsService {

    /**
     * Add a remote to the project VCS metadata.
     *
     * @param project
     *         the project descriptor.
     * @param remote
     *         the remote name.
     * @param remoteUrl
     *         the remote URL.
     * @param callback
     *         callback when the operation is done.
     */
    void addRemote(@NotNull ProjectConfigDto project, @NotNull String remote, @NotNull String remoteUrl,
                   @NotNull AsyncCallback<Void> callback);

    /**
     * Checkout a branch of the given project.
     *
     * @param project
     *         the project descriptor.
     * @param branchName
     *         the name of the branch to checkout.
     * @param createNew
     *         create a new branch if {@code true}.
     * @param callback
     *         callback when the operation is done.
     */
    void checkoutBranch(@NotNull ProjectConfigDto project, @NotNull String branchName, boolean createNew,
                        @NotNull AsyncCallback<String> callback);

    /**
     * Commits the current changes of the given project.
     *
     * @param project
     *         the project descriptor.
     * @param includeUntracked
     *         {@code true} to include untracked files, {@code false} otherwise.
     * @param commitMessage
     *         the commit message.
     * @param callback
     *         callback when the operation is done.
     */
    void commit(@NotNull ProjectConfigDto project, boolean includeUntracked, @NotNull String commitMessage,
                @NotNull AsyncCallback<Void> callback);

    /**
     * Removes a remote to the project VCS metadata.
     *
     * @param project
     *         the project descriptor.
     * @param remote
     *         the remote name.
     * @param callback
     *         callback when the operation is done.
     */
    void deleteRemote(@NotNull ProjectConfigDto project, @NotNull String remote, @NotNull AsyncCallback<Void> callback);

    /**
     * Get the current branch for the project.
     *
     * @param project
     *         the project descriptor.
     * @param callback
     *         callback when the operation is done.
     */
    void getBranchName(@NotNull ProjectConfigDto project, @NotNull AsyncCallback<String> callback);

    /**
     * Returns the name of the current branch for the given {@code project}.
     *
     * @param project
     *         the project.
     * @return the promise that resolves branch name or rejects with an error
     */
    Promise<String> getBranchName(ProjectConfigDto project);

    /**
     * Returns if the given project has uncommitted changes.
     *
     * @param project
     *         the project descriptor.
     * @param callback
     *         what to do if the project has uncommitted changes.
     */
    void hasUncommittedChanges(@NotNull ProjectConfigDto project, @NotNull AsyncCallback<Boolean> callback);

    /**
     * Returns if a local branch with the given name exists in the given project.
     *
     * @param project
     *         the project descriptor.
     * @param branchName
     *         the branch name.
     * @param callback
     *         callback called when operation is done.
     */
    void isLocalBranchWithName(@NotNull ProjectConfigDto project, @NotNull String branchName, @NotNull AsyncCallback<Boolean> callback);

    /**
     * List the local branches.
     *
     * @param project
     *         the project descriptor.
     * @param callback
     *         what to do with the branches list.
     */
    void listLocalBranches(@NotNull ProjectConfigDto project, @NotNull AsyncCallback<List<Branch>> callback);

    /**
     * List remotes.
     *
     * @param project
     *         the project descriptor.
     * @param callback
     *         what to do with the remotes list
     * @deprecated use {@link #listRemotes(ProjectConfigDto)}
     */
    void listRemotes(@NotNull ProjectConfigDto project, @NotNull AsyncCallback<List<Remote>> callback);

    /**
     * Returns the list of the remotes for given {@code project}.
     *
     * @param project
     *         the project
     * @return the promise which resolves {@literal List<Remote>} or rejects with an error
     */
    Promise<List<Remote>> listRemotes(ProjectConfigDto project);

    /**
     * Push a local branch to the given remote.
     *
     * @param project
     *         the project descriptor.
     * @param remote
     *         the remote name
     * @param localBranchName
     *         the local branch name
     * @param callback
     *         callback when the operation is done.
     */
    void pushBranch(@NotNull ProjectConfigDto project,
                    @NotNull String remote,
                    @NotNull String localBranchName,
                    @NotNull AsyncCallback<PushResponse> callback);

    /**
     * Push a local branch to remote.
     *
     * @param project
     *         the project descriptor.
     * @param remote
     *         the remote name
     * @param localBranchName
     *         the local branch name
     */
    Promise<PushResponse> pushBranch(ProjectConfigDto project,
                                     String remote,
                                     String localBranchName);
}

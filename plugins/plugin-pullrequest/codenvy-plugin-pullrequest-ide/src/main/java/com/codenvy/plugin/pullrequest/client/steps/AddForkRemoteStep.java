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
package com.codenvy.plugin.pullrequest.client.steps;


import com.codenvy.plugin.pullrequest.client.ContributeMessages;
import com.codenvy.plugin.pullrequest.client.workflow.Context;
import com.codenvy.plugin.pullrequest.client.workflow.Step;
import com.codenvy.plugin.pullrequest.client.workflow.WorkflowExecutor;
import com.codenvy.plugin.pullrequest.client.vcs.VcsServiceProvider;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.git.shared.Remote;

import javax.inject.Inject;
import java.util.List;

/**
 * Adds the forked remote repository to the remotes of the project.
 */
public class AddForkRemoteStep implements Step {
    private final static String ORIGIN_REMOTE_NAME = "origin";
    private final static String FORK_REMOTE_NAME   = "fork";

    private final Step               delegate;
    private final String             remoteUrl;
    private final VcsServiceProvider vcsServiceProvider;
    private final ContributeMessages messages;

    @Inject
    public AddForkRemoteStep(@Assisted("delegate") Step delegate,
                             @Assisted("remoteUrl") String remoteUrl,
                             final VcsServiceProvider vcsServiceProvider,
                             final ContributeMessages messages) {
        this.delegate = delegate;
        this.remoteUrl = remoteUrl;
        this.vcsServiceProvider = vcsServiceProvider;
        this.messages = messages;
    }

    @Override
    public void execute(final WorkflowExecutor executor, final Context context) {
        final String originRepositoryOwner = context.getOriginRepositoryOwner();
        final String originRepositoryName = context.getOriginRepositoryName();
        final String upstreamRepositoryOwner = context.getUpstreamRepositoryOwner();
        final String upstreamRepositoryName = context.getUpstreamRepositoryName();

        // the fork remote has to be added only if we cloned the upstream else it's origin
        if (originRepositoryOwner.equalsIgnoreCase(upstreamRepositoryOwner) &&
            originRepositoryName.equalsIgnoreCase(upstreamRepositoryName)) {
                checkRemotePresent(executor, context, remoteUrl);
        } else {
            context.setForkedRemoteName(ORIGIN_REMOTE_NAME);
            proceed(executor, context);
        }
    }

    private void checkRemotePresent(final WorkflowExecutor executor, final Context context, final String remoteUrl) {
        vcsServiceProvider.getVcsService(context.getProject())
                          .listRemotes(context.getProject(), new AsyncCallback<List<Remote>>() {
                              @Override
                              public void onSuccess(final List<Remote> result) {
                                  for (final Remote remote : result) {
                                      if (FORK_REMOTE_NAME.equals(remote.getName())) {
                                          context.setForkedRemoteName(FORK_REMOTE_NAME);
                                          if (remoteUrl.equals(remote.getUrl())) {
                                              // all is correct, continue
                                              proceed(executor, context);

                                          } else {
                                              replaceRemote(executor, context, remoteUrl);
                                          }
                                          // leave the method, do not go to addRemote(...)
                                          return;
                                      }
                                  }
                                  addRemote(executor, context, remoteUrl);
                              }

                              @Override
                              public void onFailure(final Throwable exception) {
                                  executor.fail(delegate, context, messages.stepAddForkRemoteErrorCheckRemote());
                              }
                          });
    }

    /**
     * Add the remote to the project.
     *
     * @param executor
     *         the {@link WorkflowExecutor}.
     * @param remoteUrl
     *         the url of the remote
     */
    private void addRemote(final WorkflowExecutor executor, final Context context, final String remoteUrl) {
        vcsServiceProvider.getVcsService(context.getProject())
                          .addRemote(context.getProject(), FORK_REMOTE_NAME, remoteUrl, new AsyncCallback<Void>() {
                              @Override
                              public void onSuccess(final Void notUsed) {
                                  context.setForkedRemoteName(FORK_REMOTE_NAME);

                                  proceed(executor, context);
                              }

                              @Override
                              public void onFailure(final Throwable exception) {
                                  executor.fail(delegate, context, messages.stepAddForkRemoteErrorAddFork());
                              }
                          });
    }

    /**
     * Removes the fork remote from the project before adding it with the correct URL.
     *
     * @param executor
     *         the {@link WorkflowExecutor}.
     * @param remoteUrl
     *         the url of the remote
     */
    private void replaceRemote(final WorkflowExecutor executor, final Context context, final String remoteUrl) {
        vcsServiceProvider.getVcsService(context.getProject())
                          .deleteRemote(context.getProject(), FORK_REMOTE_NAME, new AsyncCallback<Void>() {
                              @Override
                              public void onSuccess(final Void result) {
                                  addRemote(executor, context, remoteUrl);
                              }

                              @Override
                              public void onFailure(final Throwable caught) {
                                  executor.fail(delegate,
                                                context,
                                                messages.stepAddForkRemoteErrorSetForkedRepositoryRemote());
                              }
                          });
    }

    private void proceed(final WorkflowExecutor executor, final Context context) {
        executor.done(delegate, context);
    }
}

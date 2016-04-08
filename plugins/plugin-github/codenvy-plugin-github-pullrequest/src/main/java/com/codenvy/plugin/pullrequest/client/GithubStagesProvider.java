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
package com.codenvy.plugin.pullrequest.client;

import com.codenvy.plugin.pullrequest.client.parts.contribute.StagesProvider;
import com.codenvy.plugin.pullrequest.client.steps.CommitWorkingTreeStep;
import com.codenvy.plugin.pullrequest.client.steps.DetectPullRequestStep;
import com.codenvy.plugin.pullrequest.client.steps.PushBranchOnOriginStep;
import com.codenvy.plugin.pullrequest.client.workflow.Context;
import com.codenvy.plugin.pullrequest.client.steps.CreateForkStep;
import com.codenvy.plugin.pullrequest.client.steps.IssuePullRequestStep;
import com.codenvy.plugin.pullrequest.client.steps.PushBranchOnForkStep;
import com.codenvy.plugin.pullrequest.client.workflow.Step;
import com.codenvy.plugin.pullrequest.client.steps.UpdatePullRequestStep;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Provides displayed stages for GitHub contribution workflow.
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class GithubStagesProvider implements StagesProvider {

    private static final Set<Class<? extends Step>> UPDATE_STEP_DONE_TYPES;
    private static final Set<Class<? extends Step>> CREATION_ORIGIN_STEP_DONE_TYPES;
    private static final Set<Class<? extends Step>> CREATION_FORK_STEP_DONE_TYPES;

    static {
        UPDATE_STEP_DONE_TYPES = ImmutableSet.of(PushBranchOnForkStep.class,
                                                 UpdatePullRequestStep.class);
        CREATION_FORK_STEP_DONE_TYPES = ImmutableSet.of(CreateForkStep.class,
                                                   PushBranchOnForkStep.class,
                                                   IssuePullRequestStep.class);
        CREATION_ORIGIN_STEP_DONE_TYPES = ImmutableSet.of(PushBranchOnOriginStep.class,
                                                   IssuePullRequestStep.class);
    }

    private final ContributeMessages messages;

    @Inject
    public GithubStagesProvider(final ContributeMessages messages) {
        this.messages = messages;
    }

    @Override
    public List<String> getStages(final Context context) {
        if (context.isUpdateMode()) {
            return asList(messages.contributePartStatusSectionNewCommitsPushedStepLabel(),
                          messages.contributePartStatusSectionPullRequestUpdatedStepLabel());
        }
        if (context.isForkAvailable()){
            return asList(messages.contributePartStatusSectionForkCreatedStepLabel(),
                          messages.contributePartStatusSectionBranchPushedForkStepLabel(),
                          messages.contributePartStatusSectionPullRequestIssuedStepLabel());
        } else {
            return asList(messages.contributePartStatusSectionBranchPushedOriginStepLabel(),
                          messages.contributePartStatusSectionPullRequestIssuedStepLabel());
        }
    }

    @Override
    public Set<Class<? extends Step>> getStepDoneTypes(Context context) {
        if (context.isUpdateMode()) {
            return UPDATE_STEP_DONE_TYPES;
        }
        return context.isForkAvailable() ? CREATION_FORK_STEP_DONE_TYPES : CREATION_ORIGIN_STEP_DONE_TYPES;
    }

    @Override
    public Set<Class<? extends Step>> getStepErrorTypes(Context context) {
        return getStepDoneTypes(context);
    }

    @Override
    public Class<? extends Step> getDisplayStagesType(Context context) {
        return context.isUpdateMode() ? CommitWorkingTreeStep.class : DetectPullRequestStep.class;
    }
}

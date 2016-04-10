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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.shared.Constants;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;
import org.eclipse.che.commons.schedule.ScheduleRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.codenvy.activity.shared.Constants.ACTIVITY_CHECKER;
import static org.eclipse.che.api.workspace.shared.Constants.WORKSPACE_STOPPED_BY;

/**
 * Stops the inactive workspaces by given expiration time.
 *
 * <p>Note that the workspace is not stopped immediately, scheduler will stop the workspaces with one minute rate.
 * If workspace expiry period is negative, then workspace would not be stopped automatically.
 *
 * @author Anton Korneta
 */
@Singleton
public class WorkspaceActivityManager {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceActivityManager.class);

    private final long               expirePeriod;
    private final Map<String, Long>  activeWorkspaces;
    private final WorkspaceManager   workspaceManager;
    private final EventService       eventService;
    private final EventSubscriber<?> workspaceEventsSubscriber;

    @Inject
    public WorkspaceActivityManager(@Named("machine.ws_agent.inactive_stop_timeout_ms") long expirePeriod,
                                    WorkspaceManager workspaceManager,
                                    EventService eventService) {
        this.expirePeriod = expirePeriod;
        this.workspaceManager = workspaceManager;
        this.eventService = eventService;
        this.activeWorkspaces = new ConcurrentHashMap<>();
        this.workspaceEventsSubscriber = new EventSubscriber<WorkspaceStatusEvent>() {
            @Override
            public void onEvent(WorkspaceStatusEvent event) {
                switch (event.getEventType()) {
                    case RUNNING:
                        try {
                            Workspace workspace = workspaceManager.getWorkspace(event.getWorkspaceId());
                            if (workspace.getAttributes().remove(WORKSPACE_STOPPED_BY) != null); {
                                workspaceManager.updateWorkspace(event.getWorkspaceId(), workspace);
                            }
                        } catch (Exception ex) {
                            LOG.warn("Failed to remove stopped information attribute for workspace " + event.getWorkspaceId());
                        }
                        update(event.getWorkspaceId(), System.currentTimeMillis());
                        break;
                    case STOPPED:
                        activeWorkspaces.remove(event.getWorkspaceId());
                        break;
                    default:
                        //do nothing
                }
            }
        };
    }

    /**
     * Update the expiry period the workspace if it exists, otherwise add new one
     *
     * @param wsId
     *         active workspace identifier
     * @param activityTime
     *         moment in which the activity occurred
     */
    public void update(String wsId, long activityTime) {
        if (expirePeriod > 0) {
            activeWorkspaces.put(wsId, activityTime + expirePeriod);
        }
    }

    @ScheduleRate(periodParameterName = "stop.workspace.scheduler.period")
    private void invalidate() {
        if (expirePeriod <= 0) {
            return;
        }
        final long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> workspaceExpireEntry : activeWorkspaces.entrySet()) {
            if (workspaceExpireEntry.getValue() <= currentTime) {
                try {
                    String workspaceId = workspaceExpireEntry.getKey();

                    Workspace workspace = workspaceManager.getWorkspace(workspaceId);
                    workspace.getAttributes().put(WORKSPACE_STOPPED_BY, ACTIVITY_CHECKER);
                    workspaceManager.updateWorkspace(workspaceId, workspace);
                    workspaceManager.stopWorkspace(workspaceId);
                } catch (NotFoundException e) {
                    LOG.info("Workspace already stopped");
                } catch (Exception ex) {
                    LOG.error("Failed to stop the workspace", ex);
                } finally {
                    activeWorkspaces.remove(workspaceExpireEntry.getKey());
                }
            }
        }
    }

    @VisibleForTesting
    @PostConstruct
    void subscribe() {
        if (expirePeriod > 0) {
            eventService.subscribe(workspaceEventsSubscriber);
        }
    }

    @PreDestroy
    private void unsubscribe() {
        eventService.unsubscribe(workspaceEventsSubscriber);
    }
}

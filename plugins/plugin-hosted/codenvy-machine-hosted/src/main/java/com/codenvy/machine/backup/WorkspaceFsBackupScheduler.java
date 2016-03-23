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
package com.codenvy.machine.backup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.spi.InstanceNode;
import org.eclipse.che.commons.schedule.ScheduleRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Schedule backups of workspace fs of running machines.
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class WorkspaceFsBackupScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceFsBackupScheduler.class);

    private final long                 syncTimeoutMillisecond;
    private final MachineManager       machineManager;
    private final MachineBackupManager backupManager;

    private final Map<String, Long> lastMachineSynchronizationTime;
    private final Set<String>       pendingMachineSyncs;
    private final ExecutorService   executor;

    @Inject
    public WorkspaceFsBackupScheduler(MachineManager machineManager,
                                      MachineBackupManager backupManager,
                                      @Named("machine.backup.backup_period_second") long syncTimeoutSecond) {
        this.machineManager = machineManager;
        this.backupManager = backupManager;
        this.syncTimeoutMillisecond = TimeUnit.SECONDS.toMillis(syncTimeoutSecond);

        this.executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("MachineFsBackupScheduler-%s").build());
        this.lastMachineSynchronizationTime = new ConcurrentHashMap<>();
        this.pendingMachineSyncs = ConcurrentHashMap.newKeySet();
    }

    @ScheduleRate(initialDelay = 1, period = 1, unit = TimeUnit.MINUTES)
    public void scheduleBackup() {
        try {
            for (final MachineImpl machine : machineManager.getMachines()) {
                final String machineId = machine.getId();

                if (machine.getConfig().isDev() &&
                    machine.getStatus() == MachineStatus.RUNNING &&
                    isTimeToBackup(machineId)) {

                    if (pendingMachineSyncs.add(machineId)) {
                        executor.execute(() -> {
                            try {
                                backupWorkspaceInMachine(machine);

                                lastMachineSynchronizationTime.put(machine.getId(), System.currentTimeMillis());
                            } catch (ServerException | NotFoundException e) {
                                LOG.error(e.getLocalizedMessage(), e);
                            } finally {
                                pendingMachineSyncs.remove(machineId);
                            }
                        });
                    }
                }
            }
        } catch (ServerException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    @VisibleForTesting
    void backupWorkspaceInMachine(MachineImpl machine) throws NotFoundException, ServerException {
        final InstanceNode node = machineManager.getInstance(machine.getId()).getNode();

        backupManager.backupWorkspace(machine.getWorkspaceId(), node.getProjectsFolder(), node.getHost());
    }

    @VisibleForTesting
    boolean isTimeToBackup(String machineId) {
        final Long lastMachineSyncTime = lastMachineSynchronizationTime.get(machineId);

        return lastMachineSyncTime == null || System.currentTimeMillis() - lastMachineSyncTime > syncTimeoutMillisecond;
    }

    @PreDestroy
    private void teardown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.warn("Unable terminate main pool");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}

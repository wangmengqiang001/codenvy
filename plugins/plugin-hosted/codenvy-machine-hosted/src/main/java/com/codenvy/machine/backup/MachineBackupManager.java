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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.core.util.Watchdog;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Copies workspace files between machine's host and backup storage.
 *
 * @author Alexander Garagatyi
 */
public class MachineBackupManager {
    private static final Logger LOG = getLogger(MachineBackupManager.class);

    private final String backupScript;
    private final String restoreScript;
    private final int    maxBackupDuration;
    private final int    restoreDuration;
    private final File   backupsRootDir;

    @Inject
    public MachineBackupManager(@Named("machine.backup.backup_script") String backupScript,
                                @Named("machine.backup.restore_script") String restoreScript,
                                @Named("machine.backup.backup_duration_second") int maxBackupDurationSec,
                                @Named("machine.backup.restore_duration_second") int restoreDurationSec,
                                @Named("che.user.workspaces.storage") File backupsRootDir) {
        this.backupScript = backupScript;
        this.restoreScript = restoreScript;
        this.maxBackupDuration = maxBackupDurationSec;
        this.restoreDuration = restoreDurationSec;
        this.backupsRootDir = backupsRootDir;
    }

    /**
     * Copies workspace files from machine's host to backup storage.
     *
     * @param srcPath
     *         path to folder that should be backed up
     * @param srcAddress
     *         address of the server from which workspace files should be backed up
     * @param workspaceId
     *         id of workspace that should be backed up
     */
    public void backupWorkspace(final String workspaceId,
                                final String srcPath,
                                final String srcAddress) throws ServerException {
        backupWorkspace(workspaceId, srcPath, srcAddress, false);
    }

    /**
     * Copies workspace files from machine's host to backup storage and remove all files from the source.
     *
     * @param srcPath
     *         path to folder that should be backed up
     * @param srcAddress
     *         address of the server from which workspace files should be backed up
     * @param workspaceId
     *         id of workspace that should be backed up
     */
    public void backupWorkspaceAndCleanup(final String workspaceId,
                                          final String srcPath,
                                          final String srcAddress) throws ServerException {
        backupWorkspace(workspaceId, srcPath, srcAddress, true);
    }

    private void backupWorkspace(final String workspaceId, final String srcPath, final String srcAddress, boolean removeSourceOnSuccess)
            throws ServerException {
        final File destPath = WorkspaceIdHashLocationFinder.calculateDirPath(backupsRootDir, workspaceId);

        CommandLine commandLine = new CommandLine(backupScript,
                                                  srcPath,
                                                  srcAddress,
                                                  destPath.toString(),
                                                  Boolean.toString(removeSourceOnSuccess));

        try {
            execute(commandLine.asArray(), maxBackupDuration);
        } catch (TimeoutException e) {
            throw new ServerException("Backup of workspace " + workspaceId + " filesystem terminated due to timeout.");
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException("Backup of workspace " + workspaceId + " filesystem interrupted.");
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException("Backup of workspace " + workspaceId + " filesystem terminated. " + e.getLocalizedMessage());
        }
    }

    /**
     * Synchronously copies workspace files from backup storage to machine's host.
     *
     * @param workspaceId
     *         id of workspace that should be copied to machine
     * @param destPath
     *         path where files should be copied to
     * @param destAddress
     *         address of the server where workspace should be copied to
     * @throws ServerException
     */
    public void restoreWorkspaceBackup(final String workspaceId,
                                       final String destPath,
                                       final String userId,
                                       final String groupId,
                                       final String destAddress) throws ServerException {
        try {
            final String srcPath = WorkspaceIdHashLocationFinder.calculateDirPath(backupsRootDir, workspaceId).toString();

            Files.createDirectories(Paths.get(srcPath));

            CommandLine commandLine = new CommandLine(restoreScript,
                                                      srcPath,
                                                      destPath,
                                                      destAddress,
                                                      userId,
                                                      groupId);

            execute(commandLine.asArray(), restoreDuration);
        } catch (TimeoutException e) {
            throw new ServerException("Restoring of workspace " + workspaceId + " filesystem terminated due to timeout.");
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException("Restoring of workspace " + workspaceId + " filesystem interrupted.");
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException("Restoring of workspace " + workspaceId + " filesystem terminated. " + e.getLocalizedMessage());
        }
    }

    private void execute(String[] commandLine, int timeout) throws TimeoutException, IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(commandLine).redirectErrorStream(true);
        final ListLineConsumer outputConsumer = new ListLineConsumer();

        // process will be stopped after timeout
        Watchdog watcher = new Watchdog(timeout, TimeUnit.SECONDS);

        try {
            final Process process = pb.start();

            final ValueHolder<Boolean> isTimeoutExceeded = new ValueHolder<>(false);
            watcher.start(() -> {
                isTimeoutExceeded.set(true);
                ProcessUtil.kill(process);
            });

            // consume logs until process ends
            ProcessUtil.process(process, outputConsumer);

            process.waitFor();

            if (isTimeoutExceeded.get()) {
                throw new TimeoutException();
            } else if (process.exitValue() != 0) {
                LOG.error(outputConsumer.getText());
                throw new IOException("Process failed. Exit code " + process.exitValue());
            }
        } finally {
            watcher.stop();
        }
    }
}

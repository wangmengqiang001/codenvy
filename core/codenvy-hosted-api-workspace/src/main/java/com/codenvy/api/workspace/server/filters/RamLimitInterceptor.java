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

import com.google.common.util.concurrent.Striped;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Restricts number of used RAM per user
 *
 * @author Sergii Leschenko
 */
public class RamLimitInterceptor implements MethodInterceptor {
    private static final int RAM_LIMIT = 2000;

    private static final Striped<Lock> START_LOCKS = Striped.lazyWeakLock(100);

    @Inject
    private WorkspaceDao     workspaceDao;
    @Inject
    private WorkspaceManager workspaceManager;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final String methodName = invocation.getMethod().getName();
        final Object[] arguments = invocation.getArguments();

        WorkspaceConfig workspaceConfig;
        String envName = null;
        switch (methodName) {
            case "startById": {
                String workspaceId = ((String)arguments[0]);
                envName = ((String)arguments[1]);
                try {
                    workspaceConfig = workspaceDao.get(workspaceId).getConfig();
                } catch (NotFoundException | ServerException e) {
                    //Can't authorize operation
                    throw new ServerException(e);
                }
                break;
            }

            case "startFromConfig": {
                workspaceConfig = ((WorkspaceConfigDto)arguments[0]);
                break;
            }
            default:
                return invocation.proceed();
        }

        //TODO Rework synchronization per resources owner
        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final Lock lock = START_LOCKS.get(userId);
        lock.lock();
        try {
            if (envName == null) {
                envName = workspaceConfig.getDefaultEnv();
            }

            final String finalEnvName = envName;
            final Optional<? extends Environment> environmentOptional = workspaceConfig.getEnvironments()
                                                                                       .stream()
                                                                                       .filter(env -> env.getName().equals(finalEnvName))
                                                                                       .findFirst();
            final long requiredRam = getUsedRam(environmentOptional);

            final List<Optional<EnvironmentImpl>> activeEnviroments =
                    workspaceManager.getWorkspaces(EnvironmentContext.getCurrent().getUser().getId())
                                    .stream()
                                    .map(workspace -> workspace.getConfig().getEnvironment(
                                            workspace.getRuntime().getActiveEnv()))
                                    .collect(Collectors.toList());
            int usedRam = 0;
            for (Optional<EnvironmentImpl> activeEnviroment : activeEnviroments) {
                usedRam += getUsedRam(activeEnviroment);
            }

            if (requiredRam + usedRam > RAM_LIMIT) {
                throw new ForbiddenException("Current user can't use more than " + RAM_LIMIT + " mb");
            }

            return invocation.proceed();
        } finally {
            lock.unlock();
        }
    }

    private int getUsedRam(Optional<? extends Environment> environment) throws ServerException {
        if (!environment.isPresent()) {
            throw new ServerException("Can't find workspace's environment");
        }
        return environment.get()
                          .getMachineConfigs()
                          .stream()
                          .filter(machineCfg -> machineCfg.getLimits() != null)
                          .mapToInt(machineCfg -> machineCfg.getLimits().getRam())
                          .sum();
    }
}

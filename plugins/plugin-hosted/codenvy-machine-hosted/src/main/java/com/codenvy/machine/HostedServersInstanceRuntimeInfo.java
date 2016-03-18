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
package com.codenvy.machine;

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.machine.server.model.impl.ServerImpl;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.machine.DockerInstanceRuntimeInfo;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites machine servers to proxy all requests to them.
 *
 * @author Alexander Garagatyi
 */
public class HostedServersInstanceRuntimeInfo extends DockerInstanceRuntimeInfo {
    private final Map<String, MachineServerModifier> modifiers;

    @Inject
    public HostedServersInstanceRuntimeInfo(@Assisted ContainerInfo containerInfo,
                                            @Assisted String dockerNodeHost,
                                            @Assisted MachineConfig machineConfig,
                                            @Named("machine.docker.dev_machine.machine_servers") Set<ServerConf> devMachineServers,
                                            @Named("machine.docker.machine_servers") Set<ServerConf> allMachinesServers,
                                            Map<String, MachineServerModifier> modifiers) {
        super(containerInfo, dockerNodeHost, machineConfig, devMachineServers, allMachinesServers);
        this.modifiers = modifiers;
    }

    @Override
    public Map<String, ServerImpl> getServers() {
        final HashMap<String, ServerImpl> servers = new HashMap<>(super.getServers());
        for (Map.Entry<String, ServerImpl> serverEntry : servers.entrySet()) {
            if (modifiers.containsKey(serverEntry.getValue().getRef())) {
                serverEntry.setValue(modifiers.get(serverEntry.getValue().getRef())
                                              .proxy(serverEntry.getValue()));
            }
        }

        return servers;
    }
}

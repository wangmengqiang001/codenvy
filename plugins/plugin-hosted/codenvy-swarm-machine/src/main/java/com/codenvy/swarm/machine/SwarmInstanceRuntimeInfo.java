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
package com.codenvy.swarm.machine;

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.machine.DockerInstanceRuntimeInfo;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Set;

/**
 * Swarm implementation of {@link org.eclipse.che.api.core.model.machine.MachineRuntimeInfo}
 *
 * @author Alexander Garagatyi
 */
public class SwarmInstanceRuntimeInfo extends DockerInstanceRuntimeInfo {

    private final ContainerInfo info;

    @Inject
    public SwarmInstanceRuntimeInfo(@Assisted ContainerInfo containerInfo,
                                    @Assisted String dockerNodeHost,
                                    @Assisted MachineConfig machineConfig,
                                    @Named("machine.docker.dev_machine.machine_servers") Set<ServerConf> devMachineServers,
                                    @Named("machine.docker.machine_servers") Set<ServerConf> allMachinesServers) {
        super(containerInfo, dockerNodeHost, machineConfig, devMachineServers, allMachinesServers);
        info = containerInfo;
    }

    @Override
    public Map<String, String> getProperties() {
        final Map<String, String> dockerProperties = super.getProperties();

        dockerProperties.put("node.ip", info.getNode().getIP());
        dockerProperties.put("node.addr", info.getNode().getAddr());
        dockerProperties.put("node.id", info.getNode().getID());
        dockerProperties.put("node.name", info.getNode().getName());

        return dockerProperties;
    }
}

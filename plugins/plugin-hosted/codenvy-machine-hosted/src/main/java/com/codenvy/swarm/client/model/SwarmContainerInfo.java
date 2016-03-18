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
package com.codenvy.swarm.client.model;

import org.eclipse.che.plugin.docker.client.json.ContainerInfo;

import java.util.Arrays;

/**
 * Extends {@link ContainerInfo} by adding docker node description
 *
 * @author Alexander Garagatyi
 */
public class SwarmContainerInfo extends ContainerInfo {
    private Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "ContainerInfo{" +
               "id='" + getId() + '\'' +
               ", created='" + getCreated() + '\'' +
               ", appArmorProfile='" + getAppArmorProfile() + '\'' +
               ", path='" + getPath() + '\'' +
               ", args=" + Arrays.toString(getArgs()) +
               ", config=" + getConfig() +
               ", state=" + getState() +
               ", image='" + getImage() + '\'' +
               ", networkSettings=" + getNetworkSettings() +
               ", resolvConfPath='" + getResolvConfPath() + '\'' +
               ", hostConfig=" + getHostConfig() +
               ", driver='" + getDriver() + '\'' +
               ", execDriver='" + getExecDriver() + '\'' +
               ", hostnamePath='" + getHostnamePath() + '\'' +
               ", hostsPath='" + getHostsPath() + '\'' +
               ", mountLabel='" + getMountLabel() + '\'' +
               ", name='" + getName() + '\'' +
               ", processLabel='" + getProcessLabel() + '\'' +
               ", node=" + node +
               '}';
    }
}

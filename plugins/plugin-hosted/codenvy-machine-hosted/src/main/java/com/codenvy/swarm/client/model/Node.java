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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describe docker node in swarm model
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
public class Node {

    private String id;
    private String name;
    private String addr;
    private String ip;
    private int    cpus;
    private long   memory;

    private Map<String, String> labels = new HashMap<>();

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getIP() {
        return ip;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }

    public int getCpus() {
        return cpus;
    }

    public void setCpus(int cpus) {
        this.cpus = cpus;
    }

    public long getMemory() {
        return memory;
    }

    public void setMemory(long memory) {
        this.memory = memory;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "Node{" +
               "ID='" + id + '\'' +
               ", name='" + name + '\'' +
               ", addr='" + addr + '\'' +
               ", IP='" + ip + '\'' +
               ", cpus=" + cpus +
               ", memory=" + memory +
               ", labels=" + labels +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node)o;
        return Objects.equals(getCpus(), node.getCpus()) &&
               Objects.equals(getMemory(), node.getMemory()) &&
               Objects.equals(getID(), node.getID()) &&
               Objects.equals(getName(), node.getName()) &&
               Objects.equals(getAddr(), node.getAddr()) &&
               Objects.equals(getIP(), node.getIP()) &&
               Objects.equals(getLabels(), node.getLabels());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID(), getName(), getAddr(), getIP(), getCpus(), getMemory(), getLabels());
    }
}

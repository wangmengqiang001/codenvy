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
package com.codenvy.api.workspace.server;

import com.codenvy.api.permission.server.PermissionsDomain;

import org.eclipse.che.api.core.model.workspace.Workspace;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Sergii Leschenko
 */
public class WorkspaceDomain extends PermissionsDomain {
    public static final String DOMAIN_ID = "workspace";

    public enum WorkspaceActions {
        READ("read"),
        RUN("run"),
        USE("use"),
        CONFIGURE("configure"),
        SET_PERMISSIONS("setPermissions"),
        READ_PERMISSIONS("readPermissions"),
        DELETE("delete");

        private final String value;

        WorkspaceActions(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public WorkspaceDomain() {
        super(DOMAIN_ID, Stream.of(WorkspaceActions.values())
                               .map(WorkspaceActions::toString)
                               .collect(Collectors.toSet()));
    }
}

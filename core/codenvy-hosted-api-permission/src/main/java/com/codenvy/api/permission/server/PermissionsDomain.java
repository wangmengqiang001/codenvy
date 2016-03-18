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
package com.codenvy.api.permission.server;

import java.util.Set;

/**
 * @author gazarenkov
 * @author Sergii Leschenko
 */
public class PermissionsDomain {
    private final String      id;
    private final Set<String> allowedActions;

    public PermissionsDomain(String id, Set<String> allowedActions) {
        this.id = id;
        this.allowedActions = allowedActions;
    }

    public String getId() {
        return id;
    }

    public Set<String> getAllowedActions() {
        return allowedActions;
    }
}

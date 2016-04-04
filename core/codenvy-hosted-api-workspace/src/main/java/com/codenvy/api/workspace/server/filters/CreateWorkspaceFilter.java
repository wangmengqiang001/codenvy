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

import com.google.api.client.repackaged.com.google.common.base.Strings;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericMethodResource;

import javax.ws.rs.Path;

/**
 * Forbids create workspace linked to account if user doesn't have createWorkspaces permission there
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/workspace")
public class CreateWorkspaceFilter extends CheMethodInvokerFilter {
    @Override
    public void filter(GenericMethodResource genericMethodResource, Object[] arguments) throws ForbiddenException {
        final String methodName = genericMethodResource.getMethod().getName();
        if (methodName.equals("create")) {
            final String accountId = ((String)arguments[1]);

            if (!Strings.isNullOrEmpty(accountId)) {
                if (!EnvironmentContext.getCurrent().getUser().hasPermission("account", accountId, "createWorkspaces")) {
                    throw new ForbiddenException("User doesn't have permissions to perform this operation");
                }
            }
        }
    }
}

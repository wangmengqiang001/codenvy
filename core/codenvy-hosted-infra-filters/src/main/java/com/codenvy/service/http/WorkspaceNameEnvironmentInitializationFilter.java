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
package com.codenvy.service.http;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import org.eclipse.che.api.core.model.workspace.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

//TODO fix it, the place of workspace-name or id is undefined for now

/**
 * Set information about workspace in request by following path:
 * <p/>
 * /{war}/{service}/{ws-name}
 *
 * @author Alexander Garagatyi
 * @author Sergii Kabashniuk
 */
@Singleton
public class WorkspaceNameEnvironmentInitializationFilter extends WorkspaceEnvironmentInitializationFilter {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceIdEnvironmentInitializationFilter.class);

    @Inject
    private WorkspaceInfoCache cache;


    @Override
    protected Workspace getWorkspaceFromRequest(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        String requestUrl = httpRequest.getRequestURI();
        String[] pathParts = requestUrl.split("/", 5);
        try {
            return cache.getByName(pathParts[3], null);
        } catch (NotFoundException e) {
            return null;
        } catch (ServerException e) {
            LOG.warn(e.getLocalizedMessage(), e);
            return null;
        }
    }
}

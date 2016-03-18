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

import com.codenvy.api.permission.shared.dto.PermissionsDTO;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Defines Permissions REST API
 *
 * @author Sergii Leschenko
 */
@Path("/permissions")
public class PermissionsService extends Service {
    private final PermissionManager permissionManager;

    @Inject
    public PermissionsService(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    /**
     * @return supported domains' identifiers
     */
    @GET
    @Produces(APPLICATION_JSON)
    public Set<String> getSupportedDomains() {
        return permissionManager.getDomains();
    }

    /**
     * @param domain
     *         domain id to retrieve supported actions
     * @return supported actions by given domain
     */
    @GET
    @Path("/{domain}")
    @Produces(APPLICATION_JSON)
    public Set<String> getSupportedActions(@PathParam("domain") String domain) {
        return permissionManager.getDomainsActions(domain);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public void setPermissions(PermissionsDTO permissionsDto)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        permissionManager.setPermission(new Permissions(permissionsDto.getUser(),
                                                        permissionsDto.getDomain(),
                                                        permissionsDto.getInstance(),
                                                        permissionsDto.getActions()));
    }

    /**
     * @param domain
     *         domain id to retrieve permitted actions
     * @param instance
     *         instance id to retrieve permitted actions
     * @return a list of actions which can be performed by current user to given domain and instance
     */
    @GET
    @Path("/{domain}/{instance}")
    @Produces(APPLICATION_JSON)
    public List<String> getUsersPermissions(@PathParam("domain") String domain,
                                            @PathParam("instance") String instance) {
        final Permissions permissions = permissionManager.get(EnvironmentContext.getCurrent().getUser().getId(), domain, instance);
        if (permissions != null) {
            return permissions.getActions();
        }
        return Collections.emptyList();
    }

    @GET
    @Path("/{domain}/{instance}/list")
    public List<PermissionsDTO> getUsersPermissionsByInstance(@PathParam("domain") String domain,
                                                              @PathParam("instance") String instance) {
        return permissionManager.getByInstance(domain, instance)
                                .stream()
                                .map(permissions -> DtoFactory.newDto(PermissionsDTO.class)
                                                              .withUser(permissions.getUser())
                                                              .withDomain(permissions.getDomain())
                                                              .withInstance(permissions.getInstance())
                                                              .withActions(permissions.getActions()))
                                .collect(Collectors.toList());
    }

    /**
     * Removes all the permissions related to the specified user, domain and instance
     *
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     */
    @DELETE
    //TODO Think about path
    @Path("/{domain}/{instance}/{user}")
    public void removePermissions(@PathParam("domain") String domain,
                                  @PathParam("instance") String instance,
                                  @PathParam("user") String user)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        permissionManager.remove(user, domain, instance);
    }
}

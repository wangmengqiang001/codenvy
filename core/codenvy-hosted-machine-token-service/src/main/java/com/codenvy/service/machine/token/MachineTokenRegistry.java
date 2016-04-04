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
package com.codenvy.service.machine.token;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.eclipse.che.api.core.NotFoundException;

import javax.inject.Singleton;

import static java.lang.String.format;
import static org.eclipse.che.commons.lang.NameGenerator.generate;

/**
 *
 * Table-based storage of machine security tokens.
 * Table rows is workspace Id's, columns - user Id's.
 *
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 */
@Singleton
public class MachineTokenRegistry {

    private Table<String, String, String> tokens = HashBasedTable.create();

    /**
     * Generates new machine security token for given user and workspace.
     *
     * @param userId
     *        id of user to generate token for
     * @param workspaceId
     *        id of workspace to generate token for
     */
    public void generateToken(String userId, String workspaceId) {
        tokens.put(workspaceId, userId, generate("", 64));
    }

    /**
     * Gets machine security token for user and workspace.
     *
     * @param userId
     *        id of user to get token
     * @param workspaceId
     *        id of workspace to get token
     * @return machine security token
     * @throws NotFoundException
     *         when no token exists for given user and/or workspace
     */
    public String getToken(String userId, String workspaceId) throws NotFoundException {
        final String token = tokens.get(workspaceId, userId);
        if (token == null) {
            throw new NotFoundException(format("Token not found for user %s and/or workspace %s", userId, workspaceId));
        }
        return token;
    }

    /**
     * Invalidates machine security tokens for all users of given workspace.
     * @param workspaceId
     *        workspace to invalidate tokens
     */
    public void removeTokens(String workspaceId) {
        tokens.row(workspaceId).clear();
    }
}

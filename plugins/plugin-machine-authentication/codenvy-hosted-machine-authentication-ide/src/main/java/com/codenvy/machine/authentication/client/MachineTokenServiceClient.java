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
package com.codenvy.machine.authentication.client;


import com.codenvy.machine.authentication.shared.dto.MachineTokenDto;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

/**
 * @author Anton Korneta
 */
@Singleton
public class MachineTokenServiceClient {

    private final AppContext             appContext;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final String                 baseUrl;

    private MachineTokenDto machineTokenDto;

    @Inject
    public MachineTokenServiceClient(@RestContext String restContext,
                                     AppContext appContext,
                                     AsyncRequestFactory asyncRequestFactory,
                                     DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.appContext = appContext;
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.baseUrl = restContext + "/machine/token";
    }

    public Promise<MachineTokenDto> getMachineToken() {
        if (machineTokenDto != null) {
            Promises.resolve(machineTokenDto);
        }
        return asyncRequestFactory.createGetRequest(baseUrl + appContext.getWorkspaceId())
                                  .send(dtoUnmarshallerFactory.newUnmarshaller(MachineTokenDto.class))
                                  .then(new Operation<MachineTokenDto>() {
                                      @Override
                                      public void apply(MachineTokenDto machineTokenDto) throws OperationException {
                                          MachineTokenServiceClient.this.machineTokenDto = machineTokenDto;
                                      }
                                  });
    }
}

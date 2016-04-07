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

import com.codenvy.machine.authentication.server.shared.dto.MachineTokenDto;
import com.google.gwt.http.client.RequestBuilder;
import com.google.inject.Inject;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Executor;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.promises.client.js.RejectFunction;
import org.eclipse.che.api.promises.client.js.ResolveFunction;
import org.eclipse.che.ide.rest.AsyncRequest;
import org.eclipse.che.ide.rest.Unmarshallable;

/**
 * @author Anton Korneta
 */
public class MachineAsyncRequest extends AsyncRequest {

    private final MachineTokenServiceClient machineTokenServiceClient;

    @Inject
    protected MachineAsyncRequest(RequestBuilder.Method method,
                                  String url,
                                  boolean async,
                                  MachineTokenServiceClient machineTokenServiceClient) {
        super(method, url, async);
        this.machineTokenServiceClient = machineTokenServiceClient;
    }

    @Override
    public <R> Promise<R> send(final Unmarshallable<R> unmarshaller) {
        final Executor.ExecutorBody<R> body = new Executor.ExecutorBody<R>() {
            @Override
            public void apply(final ResolveFunction<R> resolve, final RejectFunction reject) {
                machineTokenServiceClient.getMachineToken().then(new Operation<MachineTokenDto>() {
                    @Override
                    public void apply(MachineTokenDto machineTokenDto) throws OperationException {
                        MachineAsyncRequest.this.user("codenvy").password(machineTokenDto.getMachineToken());
                    }
                });
            }
        };
        final Executor<R> executor = Executor.create(body);
        return Promises.create(executor);
    }
}

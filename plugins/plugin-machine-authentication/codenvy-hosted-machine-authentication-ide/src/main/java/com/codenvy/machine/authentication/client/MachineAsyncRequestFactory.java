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

import com.google.gwt.http.client.RequestBuilder;
import com.google.inject.Inject;

import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequest;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.HTTPHeader;

import java.util.List;

/**
 * @author Anton Korneta
 */
public class MachineAsyncRequestFactory extends AsyncRequestFactory {
    private static final String DTO_CONTENT_TYPE = MimeType.APPLICATION_JSON;

    private final MachineTokenServiceClient machineTokenServiceClient;
    private final DtoFactory                dtoFactory;

    @Inject
    public MachineAsyncRequestFactory(DtoFactory dtoFactory,
                                      MachineTokenServiceClient machineTokenServiceClient) {
        super(dtoFactory);
        this.machineTokenServiceClient = machineTokenServiceClient;
        this.dtoFactory = dtoFactory;
    }

    @Override
    protected AsyncRequest doCreateRequest(RequestBuilder.Method method, String url, Object dtoBody, boolean async) {
        if (!url.contains("/ext/")) {
            return super.doCreateRequest(method, url, dtoBody, async);
        }
        return doCreateMachineRequest(method, url, dtoBody, async);

    }

    private AsyncRequest doCreateMachineRequest(RequestBuilder.Method method, String url, Object dtoBody, boolean async) {
        final AsyncRequest asyncRequest = new MachineAsyncRequest(method, url, async, machineTokenServiceClient);
        if (dtoBody != null) {
            if (dtoBody instanceof List) {
                asyncRequest.data(dtoFactory.toJson((List)dtoBody));
            } else {
                asyncRequest.data(dtoFactory.toJson(dtoBody));
            }
            asyncRequest.header(HTTPHeader.CONTENT_TYPE, DTO_CONTENT_TYPE);
        }
        return asyncRequest;
    }
}

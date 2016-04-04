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
package com.codenvy.api.account;

import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests of {@link AddDefaultAccountIdInterceptor}.
 *
 * @author Yevhenii Voevodin
 */
@Listeners(MockitoTestNGListener.class)
public class AddDefaultAccountIdInterceptorTest {

    @Mock
    private MethodInvocation invocation;

    @Test(dataProvider = "workspaceServiceMethods")
    public void shouldUseDefaultAccountIdIfWorkspaceCreateMethodArgumentIsNull(Object[] methodDescriptor) throws Throwable {
        // preparing workspace service's method
        final Method method = getServiceMethod(methodDescriptor);
        when(invocation.getMethod()).thenReturn(method);
        // preparing invocation arguments which are actually the same size as method parameters
        final Object[] invocationArgs = new Object[method.getParameterCount()];
        when(invocation.getArguments()).thenReturn(invocationArgs);

        // do the interception
        new AddDefaultAccountIdInterceptor().invoke(invocation);

        // check if the last argument was set to default account identifier
        assertEquals(invocationArgs[invocationArgs.length - 1], DefaultAccountCreator.DEFAULT_ACCOUNT_ID);
    }

    @Test(dataProvider = "workspaceServiceMethods")
    public void shouldNotUseDefaultAccountIdIfWorkspaceCreateMethodArgumentIsNotNull(Object[] methodDescriptor) throws Throwable {
        // preparing workspace service's method
        final Method method = getServiceMethod(methodDescriptor);
        when(invocation.getMethod()).thenReturn(method);
        // preparing invocation arguments which are actually the same size as method parameters
        // settings the last argument value to fake account identifier
        final Object[] invocationArgs = new Object[method.getParameterCount()];
        invocationArgs[invocationArgs.length - 1] = "not default account id";
        when(invocation.getArguments()).thenReturn(invocationArgs);

        // do the interception
        new AddDefaultAccountIdInterceptor().invoke(invocation);

        // check if the last argument wasn't changed
        assertEquals(invocationArgs[invocationArgs.length - 1], "not default account id");
    }

    /**
     * Provides 1 argument which is array of objects, first element is method name, the other elements are type parameters.
     */
    @DataProvider(name = "workspaceServiceMethods")
    private Object[][] serviceMethodsProvider() {
        return new Object[][] {
                {new Object[] {"create", WorkspaceConfigDto.class, List.class, String.class}},
                {new Object[] {"startById", String.class, String.class, String.class}},
                {new Object[] {"startFromConfig", WorkspaceConfigDto.class, Boolean.class, String.class}},
                {new Object[] {"recoverWorkspace", String.class, String.class, String.class}}
        };
    }

    /**
     * Gets a {@link WorkspaceService} method based on data provided by {@link #serviceMethodsProvider()
     */
    private Method getServiceMethod(Object[] methodDescription) throws NoSuchMethodException {
        final Class<?>[] methodParams = new Class<?>[methodDescription.length - 1];
        for (int i = 1; i < methodDescription.length; i++) {
            methodParams[i - 1] = (Class<?>)methodDescription[i];
        }
        return WorkspaceService.class.getMethod(methodDescription[0].toString(), methodParams);
    }
}

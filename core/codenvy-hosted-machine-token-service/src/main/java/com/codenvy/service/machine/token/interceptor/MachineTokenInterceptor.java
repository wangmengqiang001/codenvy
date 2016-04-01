package com.codenvy.service.machine.token.interceptor;

import com.codenvy.service.machine.token.MachineTokenRegistry;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 *
 */
@Singleton
public class MachineTokenInterceptor implements MethodInterceptor {

    @Inject
    MachineTokenRegistry tokenRegistry;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();

        if (invocation.getMethod().getName().equals("stopWorkspace")) {
            String workspaceId = (String)invocation.getArguments()[0];
            tokenRegistry.removeTokens(workspaceId);
            return  result;
        }

        if (result instanceof UsersWorkspace) {
            UsersWorkspace workspace = ((UsersWorkspace)result);
            tokenRegistry.generateToken(workspace.getOwner(), workspace.getId());
        }
        return result;
    }
}

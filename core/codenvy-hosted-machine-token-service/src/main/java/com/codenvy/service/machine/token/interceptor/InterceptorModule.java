package com.codenvy.service.machine.token.interceptor;

import com.google.inject.AbstractModule;

import org.eclipse.che.api.workspace.server.WorkspaceManager;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static org.eclipse.che.inject.Matchers.names;


/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 *
 */
public class InterceptorModule extends AbstractModule {

    @Override
    protected void configure() {
        final MachineTokenInterceptor tokenInterceptor = new MachineTokenInterceptor();
        requestInjection(tokenInterceptor);
        bindInterceptor(subclassesOf(WorkspaceManager.class), names("startById"), tokenInterceptor);
        bindInterceptor(subclassesOf(WorkspaceManager.class), names("startByName"), tokenInterceptor);
        bindInterceptor(subclassesOf(WorkspaceManager.class), names("recoverWorkspace"), tokenInterceptor);

        bindInterceptor(subclassesOf(WorkspaceManager.class), names("stopWorkspace"), tokenInterceptor);
    }
}

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
package com.codenvy.ext.java.server;


import com.codenvy.api.permission.server.PermissionTokenHandler;
import com.codenvy.api.permission.server.PermissionChecker;
import com.codenvy.api.permission.server.RemotePermissionChecker;
import com.codenvy.auth.sso.client.RecoverableTokenHandler;
import com.codenvy.auth.sso.client.SSOContextResolver;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;

import org.eclipse.che.api.auth.oauth.OAuthTokenProvider;
import org.eclipse.che.api.core.notification.WSocketEventBusClient;
import org.eclipse.che.api.core.rest.ApiInfoService;
import org.eclipse.che.api.core.rest.CoreRestModule;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitUserResolver;
import org.eclipse.che.api.project.server.BaseProjectModule;
import org.eclipse.che.api.ssh.server.HttpSshServiceClient;
import org.eclipse.che.api.ssh.server.SshServiceClient;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.vfs.server.VirtualFileSystemModule;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistryPlugin;
import org.eclipse.che.everrest.CheAsynchronousJobPool;
import org.eclipse.che.generator.archetype.ArchetypeGenerator;
import org.eclipse.che.generator.archetype.ArchetypeGeneratorModule;
import org.eclipse.che.git.impl.nativegit.LocalGitUserResolver;
import org.eclipse.che.git.impl.nativegit.NativeGitConnectionFactory;
import org.eclipse.che.ide.ext.github.server.inject.GitHubModule;
import org.eclipse.che.ide.ext.java.jdi.server.DebuggerService;
import org.eclipse.che.ide.ext.microsoft.server.inject.MicrosoftModule;
import org.eclipse.che.ide.extension.maven.server.inject.MavenModule;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.security.oauth.RemoteOAuthTokenProvider;
import org.eclipse.che.vfs.impl.fs.AutoMountVirtualFileSystemRegistry;
import org.eclipse.che.vfs.impl.fs.LocalFSMountStrategy;
import org.eclipse.che.vfs.impl.fs.MachineFSMountStrategy;
import org.eclipse.che.vfs.impl.fs.VirtualFileSystemFSModule;
import org.everrest.core.impl.async.AsynchronousJobPool;
import org.everrest.core.impl.async.AsynchronousJobService;
import org.everrest.guice.ServiceBindingHelper;
import org.eclipse.che.commons.lang.Pair;

import javax.inject.Named;

/**
 * @author Evgen Vidolob
 * @author Sergii Kabashniuk
 * @author Max Shaposhnik
 * @author Alexander Garagatyi
 * @author Anton Korneta
 * @author Vitaly Parfonov
 */
@DynaModule
public class MachineModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ApiInfoService.class);

        bind(VirtualFileSystemRegistryPlugin.class); //VirtualFileSystemRegistryPlugin

        bind(SshServiceClient.class).to(HttpSshServiceClient.class);
        bind(LocalFSMountStrategy.class).to(MachineFSMountStrategy.class);
        bind(VirtualFileSystemRegistry.class).to(AutoMountVirtualFileSystemRegistry.class);
        bind(OAuthTokenProvider.class).to(RemoteOAuthTokenProvider.class);
        bind(LocalFSMountStrategy.class).to(MachineFSMountStrategy.class);
        bind(org.eclipse.che.git.impl.nativegit.ssh.SshKeyProvider.class)
                .to(org.eclipse.che.git.impl.nativegit.ssh.SshKeyProviderImpl.class);

        install(new CoreRestModule());
        install(new BaseProjectModule());
        install(new VirtualFileSystemModule());
        install(new VirtualFileSystemFSModule());
        install(new MavenModule());
        install(new ArchetypeGeneratorModule());
        install(new GitHubModule());
        install(new MicrosoftModule());
        install(new org.eclipse.che.swagger.deploy.DocsModule());

        bind(ArchetypeGenerator.class);
        bind(DebuggerService.class);

        bind(GitUserResolver.class).to(LocalGitUserResolver.class);
        bind(GitConnectionFactory.class).to(NativeGitConnectionFactory.class);

        bind(AsynchronousJobPool.class).to(CheAsynchronousJobPool.class);
        bind(ServiceBindingHelper.bindingKey(AsynchronousJobService.class, "/async/{ws-id}")).to(AsynchronousJobService.class);

        bind(String.class).annotatedWith(Names.named("api.endpoint")).toProvider(ApiEndpointProvider.class);
        bind(String.class).annotatedWith(Names.named("user.token")).toProvider(UserTokenProvider.class);

        bind(SSOContextResolver.class).to(com.codenvy.auth.sso.client.EnvironmentContextResolver.class);

        bind(PermissionChecker.class).to(RemotePermissionChecker.class);
        bind(com.codenvy.auth.sso.client.TokenHandler.class).to(PermissionTokenHandler.class);
        bind(com.codenvy.auth.sso.client.TokenHandler.class).annotatedWith(Names.named("delegate.handler"))
                                                            .to(RecoverableTokenHandler.class);

        bindConstant().annotatedWith(Names.named("auth.sso.cookies_disabled_error_page_url"))
                      .to("/site/error/error-cookies-disabled");
        bindConstant().annotatedWith(Names.named("auth.sso.login_page_url")).to("/site/login");

        bind(PreferenceDao.class).to(org.eclipse.che.api.local.RemotePreferenceDao.class);

        bind(String.class).annotatedWith(Names.named("user.token")).toProvider(UserTokenProvider.class);
        bind(WSocketEventBusClient.class).asEagerSingleton();

        bind(String.class).annotatedWith(Names.named("event.bus.url")).toProvider(EventBusURLProvider.class);
    }

    //it's need for WSocketEventBusClient and in the future will be replaced with the property
    @Named("notification.client.event_subscriptions")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] eventSubscriptionsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[] {Pair.of(eventBusURL, "")};
    }

    //it's need for EventOriginClientPropagationPolicy and in the future will be replaced with the property
    @Named("notification.client.propagate_events")
    @Provides
    @SuppressWarnings("unchecked")
    Pair<String, String>[] propagateEventsProvider(@Named("event.bus.url") String eventBusURL) {
        return new Pair[] {Pair.of(eventBusURL, "")};
    }
}
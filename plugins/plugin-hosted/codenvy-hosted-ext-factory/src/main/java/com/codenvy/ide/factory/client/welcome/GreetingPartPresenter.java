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
package com.codenvy.ide.factory.client.welcome;

import com.codenvy.ide.factory.client.welcome.preferences.ShowWelcomePreferencePagePresenter;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectHandler;
import org.eclipse.che.ide.api.event.project.OpenProjectEvent;
import org.eclipse.che.ide.api.event.project.OpenProjectHandler;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy Guliy
 * @author Sergii Leschenko
 */
@Singleton
public class GreetingPartPresenter extends BasePresenter implements GreetingPartView.ActionDelegate, HasView {
    private static final String DEFAULT_TITLE = "Greeting";

    private final NotificationManager notificationManager;
    private final WorkspaceAgent      workspaceAgent;
    private final AppContext          appContext;
    private final EventBus            eventBus;
    private final GreetingPartView    view;
    private final PreferencesManager  preferencesManager;

    private List<HandlerRegistration> projectActionHandlers;

    private boolean performed = false;

    private String title = DEFAULT_TITLE;

    @Inject
    public GreetingPartPresenter(GreetingPartView view,
                                 NotificationManager notificationManager,
                                 EventBus eventBus,
                                 WorkspaceAgent workspaceAgent,
                                 AppContext appContext,
                                 PreferencesManager preferencesManager) {
        this.view = view;
        this.notificationManager = notificationManager;
        this.workspaceAgent = workspaceAgent;
        this.appContext = appContext;
        this.preferencesManager = preferencesManager;
        this.projectActionHandlers = new ArrayList<>();
        this.eventBus = eventBus;

        view.setDelegate(this);
    }

    public void process() {
        //show default greeting in persistent workspaces after opening of project
        if (!appContext.getWorkspace().isTemporary()) {
            projectActionHandlers.add(eventBus.addHandler(OpenProjectEvent.TYPE, new OpenProjectHandler() {
                @Override
                public void onProjectOpened(OpenProjectEvent event) {
                    showGreeting();
                }
            }));
            projectActionHandlers.add(eventBus.addHandler(CloseCurrentProjectEvent.TYPE, new CloseCurrentProjectHandler() {
                @Override
                public void onCloseCurrentProject(CloseCurrentProjectEvent event) {
                    hideGreeting();
                }
            }));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    @NotNull
    @Override
    public String getTitle() {
        return title != null ? title : DEFAULT_TITLE;
    }

    @Nullable
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    @Nullable
    @Override
    public String getTitleToolTip() {
        return "Greeting the user";
    }

    @Override
    public int getSize() {
        return 320;
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    public void showGreeting(Map<String, String> parameters) {
        if (performed) {
            return;
        }
        performed = true;

        if (!projectActionHandlers.isEmpty()) {
            for (HandlerRegistration projectActionHandler : projectActionHandlers) {
                projectActionHandler.removeHandler();
            }
        }

        if (appContext.getCurrentUser().isUserPermanent()) {
            showGreeting(parameters.get("authenticatedTitle"),
                         parameters.get("authenticatedContentUrl"),
                         parameters.get("authenticatedNotification"));

        } else {
            showGreeting(parameters.get("nonAuthenticatedTitle"),
                         parameters.get("nonAuthenticatedContentUrl"),
                         parameters.get("nonAuthenticatedNotification"));
        }

        if (appContext.getWorkspace().isTemporary()) {
            workspaceAgent.setActivePart(this);
        }
    }

    /**
     * Displays a welcome page in accordance to visibility of project
     */
    public void showGreeting() {
        if (performed) {
            return;
        }
        performed = true;

        // Remove project action handler only when workspace is temporary
        if (appContext.getWorkspace().isTemporary() && !projectActionHandlers.isEmpty()) {
            for (HandlerRegistration projectActionHandler : projectActionHandlers) {
                projectActionHandler.removeHandler();
            }
        }

        WorkspaceDto workspace = appContext.getWorkspace();

        // checking whether welcome is enabled for persistent workspace.
        if (!workspace.isTemporary()
            && preferencesManager.getValue(ShowWelcomePreferencePagePresenter.SHOW_WELCOME_PREFERENCE_KEY) != null
            && !"true".equals(preferencesManager.getValue(ShowWelcomePreferencePagePresenter.SHOW_WELCOME_PREFERENCE_KEY))) {
            return;
        }

        String key = appContext.getCurrentUser().isUserPermanent() ? "authenticated" : "anonymous";

        key += workspace.isTemporary() ? "-workspace-temporary" : "";

        String url = findGreetingByKey(key);
        if (!isNullOrEmpty(url)) {
            createGreetingFrame(url);
        }
    }

    private void hideGreeting() {
        workspaceAgent.removePart(this);
        performed = false;
    }

    /**
     * Opens Greeting part and displays the URL in Frame.
     */
    private void showGreeting(@NotNull String title, String greetingContentURL, final String notification) {
        this.title = title;
        workspaceAgent.openPart(this, PartStackType.TOOLING, Constraints.FIRST);

        view.setTitle(title);
        view.showGreeting(greetingContentURL);

        if (notification != null) {
            new Timer() {
                @Override
                public void run() {
                    new TooltipHint(notification);
                }
            }.schedule(1000);
        }
    }

    /**
     * Returns greeting configuration element.
     */
    private static native String findGreetingByKey(String key) /*-{
        try {
            return $wnd.IDE.config.greetings[key];
        } catch (e) {
        }
        return null;
    }-*/;

    /**
     * Creates hidden Frame to fetch greeting parameters.
     */
    private void createGreetingFrame(final String url) {
        final Frame frame = new Frame(url);
        Style style = frame.getElement().getStyle();

        style.setPosition(Style.Position.ABSOLUTE);
        style.setLeft(-1000, Style.Unit.PX);
        style.setTop(-1000, Style.Unit.PX);
        style.setWidth(1, Style.Unit.PX);
        style.setHeight(1, Style.Unit.PX);
        style.setOverflow(Style.Overflow.HIDDEN);

        frame.addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent event) {
                fetchGreetingParamsFromIFrame(IFrameElement.as(frame.getElement()), url);
                frame.removeFromParent();
            }
        });

        RootPanel.get().add(frame);
    }

    /**
     * Fetches greeting parameters from frame.
     */
    private native void fetchGreetingParamsFromIFrame(Element element, String greetingContentURL)/*-{
        try {
            var frameDocument = element.contentWindow.document;
            var head = frameDocument.getElementsByTagName('head')[0];

            var title = null;
            var notification = null;

            var children = head.childNodes;
            for (var i = 0; i < children.length; i++) {
                var child = children[i];

                if (child.nodeType != 1) {
                    continue;
                }

                if ("title" == child.nodeName.toLowerCase()) {
                    title = child.innerHTML;
                    continue;
                }

                if ("meta" == child.nodeName.toLowerCase() &&
                    child.getAttribute("name") != null &&
                    "notification" == child.getAttribute("name").toLowerCase()) {
                    notification = child.getAttribute("content");
                }
            }

            this.@com.codenvy.ide.factory.client.welcome.GreetingPartPresenter::showGreeting(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(title, greetingContentURL, notification);
        } catch (e) {
            this.@com.codenvy.ide.factory.client.welcome.GreetingPartPresenter::fetchingGreetingParamsFailed(Ljava/lang/String;)(e.message);
        }
    }-*/;

    /**
     * Notifies the user when fetching greeting parameters are failed.
     *
     * @param message
     *         message to display
     */
    private void fetchingGreetingParamsFailed(String message) {
        notificationManager.notify(message);
        Log.error(GreetingPartPresenter.class, message);
    }

    private boolean isNullOrEmpty(String input) {
        return input == null || input.trim().isEmpty();
    }

    @Override
    public View getView() {
        return view;
    }

}

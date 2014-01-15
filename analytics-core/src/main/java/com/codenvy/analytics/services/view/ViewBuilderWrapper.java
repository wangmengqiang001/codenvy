/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.services.view;

import java.text.ParseException;
import java.util.Map;

import com.codenvy.analytics.services.Feature;
import com.google.inject.Guice;
import com.google.inject.Injector;

/** @author Dmytro Nochevnov */
public class ViewBuilderWrapper extends Feature {
    private static Injector injector = Guice.createInjector();
    
    public ViewBuilder getFeatureInstance() {
        return injector.getInstance(ViewBuilder.class);
    }

    @Override
    protected Map<String, String> initializeDefaultContext() throws ParseException {
        return getFeatureInstance().initializeDefaultContext();
    }

    @Override
    protected void putParametersInContext(Map<String, String> context) {
        getFeatureInstance().putParametersInContext(context);
    }

    @Override
    protected void doExecute(Map<String, String> context) throws Exception {
        getFeatureInstance().doExecute(context);
    }

    @Override
    public boolean isAvailable() {
        return getFeatureInstance().isAvailable();
    }
    
}

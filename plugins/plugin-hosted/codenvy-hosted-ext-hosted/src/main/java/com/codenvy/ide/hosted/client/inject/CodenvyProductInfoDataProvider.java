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
package com.codenvy.ide.hosted.client.inject;

import com.codenvy.ide.hosted.client.HostedLocalizationConstant;
import com.codenvy.ide.hosted.client.HostedResources;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.ProductInfoDataProvider;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Implementation of {@link ProductInfoDataProvider}
 *
 * @author Alexander Andrienko
 */
public class CodenvyProductInfoDataProvider implements ProductInfoDataProvider {

    private final HostedLocalizationConstant hostedLocalizationConstant;
    private final HostedResources            resources;

    @Inject
    public CodenvyProductInfoDataProvider(HostedLocalizationConstant hostedLocalizationConstant, HostedResources resources) {
        this.hostedLocalizationConstant = hostedLocalizationConstant;
        this.resources = resources;
    }

    @Override
    public String getName() {
        return hostedLocalizationConstant.getProductName();
    }

    @Override
    public String getSupportLink() {
        return hostedLocalizationConstant.getSupportLink();
    }

    @Override
    public String getDocumentTitle() {
        return hostedLocalizationConstant.codenvyTabTitle();
    }

    @Override
    public String getDocumentTitle(String workspaceName) {
        return hostedLocalizationConstant.codenvyTabTitle(workspaceName);
    }

    @Override
    public SVGResource getLogo() {
        return resources.logo();
    }

    @Override
    public String getSupportTitle() {
        return hostedLocalizationConstant.supportTitle();
    }
}

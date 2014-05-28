/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.service.factory;

import com.codenvy.api.account.server.SubscriptionService;

/**
 * Subscription of tracked factories
 *
 * @author Sergii Kabashniuk
 */
public class TrackedFactoryService extends SubscriptionService {

    public TrackedFactoryService() {
        super("TrackedFactory", "Tracked Factory");
   }
}
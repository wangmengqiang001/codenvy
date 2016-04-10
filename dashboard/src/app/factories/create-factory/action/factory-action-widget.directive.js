/*
 *  [2015] - [2016] Codenvy, S.A.
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
'use strict';

/**
 * Defines a directive for displaying action widget.
 * @author Florent Benoit
 */
export class FactoryAction {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor() {
    this.restrict = 'E';

    this.templateUrl = 'app/factories/create-factory/action/factory-action-widget.html';
    this.replace = false;

    this.controller = 'FactoryActionCtrl';
    this.controllerAs = 'factoryActionCtrl';

    this.bindToController = true;

    // scope values
    this.scope = {
      actionName: '@cdvyActionName',
      actionProperty: '@cdvyActionProperty',
      actionIcon: '@cdvyActionIcon',
      actionGroup: '@cdvyActionGroup',
      callbackController : '=cdvyCallbackController',
      action : '=cdvyAction'
    };
  }




}

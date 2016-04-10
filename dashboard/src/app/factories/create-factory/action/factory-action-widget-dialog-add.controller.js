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
 * @ngdoc controller
 * @name factory.directive:FactoryActionDialogAddController
 * @description This class is handling the controller for adding action to a factory
 * @author Florent Benoit
 */
export class FactoryActionDialogAddController {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($mdDialog) {
    this.$mdDialog = $mdDialog;

    this.actions = [];
    this.actions.push({name : 'RunCommand', id: 'runcommand'});
    this.actions.push({name : 'openFile', id: 'openfile'});
    this.selectedAction = this.actions[0].id;
  }

  /**
   * Callback of the add button of the dialog.
   */
  add() {
    this.$mdDialog.hide();
    this.callbackController.callbackAddAction(this.selectedAction, this.selectedParam);
  }


  /**
   * Callback of the cancel button of the dialog.
   */
  abort() {
    this.$mdDialog.hide();
  }
}

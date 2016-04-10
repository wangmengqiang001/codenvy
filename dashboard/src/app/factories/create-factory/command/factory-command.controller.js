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
 * Defines controller of directive for displaying factory command.
 * @ngdoc controller
 * @name factory.directive:FactoryCommandController
 * @author Florent Benoit
 */
export class FactoryCommandController {

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($mdDialog) {
    this.$mdDialog = $mdDialog;
  }

  /**
   * User clicked on the add button to add a new command
   * @param $event
   */
  addCommand() {
    if (!this.factoryObject) {
      this.factoryObject = {};
    }

    if (!this.factoryObject.workspace) {
      this.factoryObject.workspace = {};
    }

    if (!this.factoryObject.workspace.commands) {
      this.factoryObject.workspace.commands = [];
    }
    let command = {
      "commandLine": this.commandLine,
      "name": this.commandLineName,
      "attributes": {
        "previewUrl": ""
      },
      "type": "custom"
    };

    this.factoryObject.workspace.commands.push(command);
  }

  /**
   * Remove command based on the provided index
   * @param index the index in the array of workspace commands
   */
  removeCommand(index) {
    this.factoryObject.workspace.commands.splice(index, 1);
  }

  /**
   * Edit the command based on the provided index
   * @param $event the mouse event
   * @param index the index in the array of workspace commands
   */
  editCommand($event, index) {
    this.$mdDialog.show({
      targetEvent: $event,
      controller: 'FactoryCommandDialogEditController',
      controllerAs: 'factoryCommandDialogEditCtrl',
      bindToController: true,
      clickOutsideToClose: true,
      locals: {
        callbackController: this,
        index: index,
        selectedValue: this.factoryObject.workspace.commands[index].commandLine
      },
      templateUrl: 'app/factories/create-factory/command/factory-command-edit.html'
    });
  }

  callbackEditAction(index, newValue) {
    this.factoryObject.workspace.commands[index].commandLine = newValue;
  }
}

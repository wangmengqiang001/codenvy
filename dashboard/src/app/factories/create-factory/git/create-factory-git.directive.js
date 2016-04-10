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
 * Defines a directive for creating factory from git.
 * @author Florent Benoit
 */
export class CreateFactoryGit {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor() {

    this.controller = 'CreateFactoryGitController';
    this.controllerAs = 'createFactoryGitCtrl';
    this.bindToController = true;

    this.restrict = 'E';
    this.templateUrl = 'app/factories/create-factory/git/create-factory-git.html';


    // scope values
    this.scope = {
      location: '=cdvyGitLocation'
    };
  }

}

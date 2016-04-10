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
 * Controller for a create factory.
 * @author Oleksii Orel
 * @author Florent Benoit
 */
export class CreateFactoryCtrl {

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($location, cheAPI, $log, codenvyAPI, cheNotification, $scope, $filter, lodash, $document) {
    this.$location = $location;
    this.cheAPI = cheAPI;
    this.$log = $log;
    this.codenvyAPI = codenvyAPI;
    this.cheNotification = cheNotification;
    this.$filter = $filter;
    this.lodash = lodash;
    this.$document = $document;

    this.isLoading = false;
    this.isImporting = false;

    this.stackRecipeMode = 'current-recipe';

    // at first, we're in source mode
    this.title = 'New Factory';
    this.flow = 'source';

    this.factoryContent = null;

    this.factoryName = '';
    this.factoryMetadataForm = null;

    $scope.$watch('createFactoryCtrl.factoryContent', (newValue) => {
      this.factoryObject = angular.fromJson(newValue);

      // each time json is modified, toggle stack choice
      this.stackRecipeMode = 'current-recipe';

      // when factory content is updated, update the factory on the remote side
      if (this.factoryContent && this.factoryId) {
        this.updateFactory();
      }

    }, true);


    $scope.$watch('createFactoryCtrl.factoryObject', () => {
      this.factoryContent = this.$filter('json')(angular.fromJson(this.factoryObject));
    }, true);


    $scope.$watch('createFactoryCtrl.gitLocation', (newValue) => {
      // update underlying model
      // Updating first project item
      if (!this.factoryObject) {
        //fetch it !
        let templateName = 'git';
        let promise = this.codenvyAPI.getFactoryTemplate().fetchFactoryTemplate(templateName);

        promise.then(() => {
          let factoryContent = this.codenvyAPI.getFactoryTemplate().getFactoryTemplate(templateName);
          this.factoryObject = angular.fromJson(factoryContent);
          this.updateGitProjectLocation(newValue);
        });
      } else {
        this.updateGitProjectLocation(newValue);
      }

    }, true);
  }

  /**
   * Update the source project location for git
   * @param location the new location
   */
  updateGitProjectLocation(location) {
    let project = this.factoryObject.workspace.projects[0];
    project.source.type = 'git';
    project.source.location = location;
  }

  /**
   * Update the machine recipe URL
   * @param recipeURL
   */
  updateMachineRecipeLocation(recipeURL) {
    if (!this.factoryObject) {
      return;
    }
    let machineConfig = this.factoryObject.workspace.environments[0].machineConfigs[0];
    machineConfig.source.type = 'recipe';
    machineConfig.source.location = recipeURL;
  }

  /**
   * Create a new factory by factory content
   * @param factoryContent
   */
  createFactoryByContent(factoryContent) {
    if (!factoryContent) {
      return;
    }

    // go into configure mode
    this.flow = 'configure';

    this.title = 'Configure Factory';

    this.isImporting = true;

    let promise = this.codenvyAPI.getFactory().createFactoryByContent(factoryContent);

    promise.then((factory) => {
      this.isImporting = false;

      this.lodash.find(factory.links, (link) => {
        if (link.rel === 'accept' || link.rel === 'accept-named') {
          this.factoryLink = link.href;
        }
      });

      var parser = this.$document[0].createElement('a');
      parser.href = this.factoryLink;
      this.factoryId = factory.id;
      this.factoryBadgeUrl = parser.protocol + '//' + parser.hostname + '/factory/resources/codenvy-contribute.svg';

      this.markdown = '[![Contribute](' + this.factoryBadgeUrl + ')](' + this.factoryLink + ')';
    }, (error) => {
      this.isImporting = false;
      this.cheNotification.showError(error.data.message ? error.data.message : 'Create factory failed.');
      this.$log.error(error);
    });
  }


  /*
   * Flow of creating a factory is finished, we can redirect to details of factory
   */
  finishFlow() {
    this.$location.path('/factory/' + this.factoryId);
  }

  /**
   * Callback when changing stack tab
   */
  setStackTab() {
  }


  /**
   * Callback when stack has been set
   * @param stack  the selected stack
   */
  cheStackLibrarySelecter(stack) {
    this.stack = stack
  }


  /**
   * Callback when user ask to validate a stack
   * We need then to create (if required) recipe and update JSON factory configuration
   */
  validateStack() {
    //check predefined recipe location
    if (this.stack) {
      // needs to get recipe URL from stack
      let promise = this.computeRecipeForStack(this.stack);
      promise.then((recipe) => {
        this.createRecipe(recipe);
      }, (error) => {
        this.cheNotification.showError(error.data.message ? error.data.message : 'Error during recipe creation.');
      });
    } else if (this.recipeUrl) {
      this.updateMachineRecipeLocation(this.recipeUrl);
    } else if (this.recipeScript) {
      // create recipe from script
      let promise = this.submitRecipe('generated-script', this.recipeScript);
      promise.then((recipe) => {
        this.createRecipe(recipe);
      }, (error) => {
        this.cheNotification.showError(error.data.message ? error.data.message : 'Error during recipe creation.');
      });
    }
  }

  /**
   * Get recipe link from newly created recipe
   * @param recipe the recipe result
   */
  createRecipe(recipe) {
    let findLink = this.lodash.find(recipe.links, (link) => {
      return link.rel === 'get recipe script';
    });
    if (findLink) {
      this.updateMachineRecipeLocation(findLink.href);
    }
  }

  /**
   * User has selected a stack. needs to find or add recipe for that stack
   */
  computeRecipeForStack(stack) {
    // look at recipe
    let recipeSource = stack.source;

    let promise;

    // what is type of source ?
    if ('image' === recipeSource.type) {
      // needs to add recipe for that script
      promise = this.submitRecipe('generated-' + stack.name, 'FROM ' + recipeSource.origin);
    } else if ('recipe' === recipeSource.type) {
      promise = this.submitRecipe('generated-' + stack.name, recipeSource.origin);
    } else {
      throw 'Not implemented';
    }

    return promise;
  }

  /**
   * Create a new recipe based on a given name and a given script
   * @param recipeName the name of the recipe
   * @param recipeScript the content of the docker script for example
   * @returns {recipe} promise
   */
  submitRecipe(recipeName, recipeScript) {
    let recipe = {
      type: 'docker',
      name: recipeName,
      permissions: {
        groups: [
          {
            name: 'public',
            acl: [
              'read'
            ]
          }
        ],
        users: {}
      },
      script: recipeScript
    };

    return this.cheAPI.getRecipe().create(recipe);
  }

  /**
   * Update factory with current content
   */
  updateFactory() {
    let promise = this.codenvyAPI.getFactory().setFactoryContent(this.factoryId, this.factoryContent);

    promise.then((factory) => {
      this.factory = factory;
      this.cheNotification.showInfo('Factory information successfully updated.');
    }, (error) => {
      //this.factoryContent = this.$filter('json')(this.originFactoryContent, 2);
      this.cheNotification.showError(error.data.message ? error.data.message : 'Update factory failed.');
      this.$log.log('error', error);
    });
  }

  /**
   * Callback when a github repository is selected
   */
  selectGitHubRepository() {
    // update location
    this.gitLocation = this.selectedGitHubRepository.clone_url;
  }

  /**
   * Callback when action is gonna be removed
   * @param actionSection
   * @param action
   */
  removeAction(actionSection, action) {
    // search action in the section
    let section = this.factoryObject.ide[actionSection];
    if (section) {
      let actions = section.actions;
      if (actions) {
        var index = 0;
        actions.forEach((existingAction) => {
          if (existingAction === action) {
            actions = actions.splice(index, 1);
          }
          index++;
        });
      }
    }
  }


  /**
   * User clicked on the + button to add a new action. Show the dialog
   * @param $event
   */
  addAction($event) {
    this.$mdDialog.show({
      targetEvent: $event,
      controller: 'FactoryActionDialogAddCtrl',
      controllerAs: 'factoryActionDialogAddCtrl',
      bindToController: true,
      clickOutsideToClose: true,
      locals: {callbackController: this},
      templateUrl: 'app/factories/create-factory/action/factory-action-widget-dialog-add.html'
    });
  }

}

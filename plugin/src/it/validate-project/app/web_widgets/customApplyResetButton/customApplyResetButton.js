(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('customApplyResetButton', function() {
    return {
      controllerAs: 'ctrl',
      controller: function PbButtonCtrl($scope, $http, $location, $log, $window, localStorageService, modalService) {

  'use strict';


  this.action = function action() {
   $scope.properties.resetValue ++;
  };

 
}
,
      template: '<div class="text-{{ properties.alignment }}">\n    <button\n        ng-class="\'btn btn-\' + properties.buttonStyle"\n        ng-click="ctrl.action()"\n        type="button"\n        ng-disabled="properties.disabled || ctrl.busy" ng-bind-html="properties.label | uiTranslate"></button>\n</div>\n'
    };
  });

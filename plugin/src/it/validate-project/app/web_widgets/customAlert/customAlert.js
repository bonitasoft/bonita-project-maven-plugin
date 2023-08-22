(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('customAlert', function() {
    return {
      controllerAs: 'ctrl',
      controller: function WidgetalertController($scope, $sce) {
    
    this.getMessage = function () {
        let message = $scope.properties.message;
        if (angular.isObject(message) && message.message) {
            message = message.message;
        }
        return $sce.trustAsHtml(message);
    };
    
    this.getClasses = function () {
        let classes = 'alert '+ $scope.properties.style;
        if ($scope.properties.isDismissible)
            classes += ' alert-dismissible';
        return classes;
    };
    
    this.dismiss = function() {
        $scope.properties.message = null;
    };
},
      template: '<div ng-class="ctrl.getClasses()" ng-show="properties.message" role="alert">\n    <button ng-if="properties.isDismissible" ng-click="ctrl.dismiss()" type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>\n    <b>{{properties.label}}</b> <span ng-bind-html="ctrl.getMessage()"></span>\n</div>'
    };
  });

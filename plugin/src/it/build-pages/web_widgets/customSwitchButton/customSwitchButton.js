(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('customSwitchButton', function() {
    return {
      controllerAs: 'ctrl',
      controller: function SwitchButtonController($scope, $http) {
    
    var vm = this;
    
    this.changed = function(){
         doRequest($scope.properties.action, $scope.properties.url);
    };


  function doRequest(method, url, params) {
    vm.busy = true;
    var req = {
      method: method,
      url: url,
      data: angular.copy($scope.properties.dataToSend),
      params: params
    };

    return $http(req)
      .success(function(data, status) {
        $scope.properties.responseStatusCode = status;
      })
      .error(function(data, status) {
        $scope.properties.responseStatusCode = status;
      })
      .finally(function() {
        vm.busy = false;
      });
  }
    
    
},
      template: ' <div class="form-group">\n    <label\n        ng-if="!properties.labelHidden"\n        class="text-right control-label col-xs-{{ !properties.labelHidden ? properties.labelWidth : 12 }}" ng-bind-html="properties.label | uiTranslate">\n    </label>\n    <toggle ng-model="properties.value" \n           ng-change="ctrl.changed()" \n           on="{{ properties.onLabel }}" \n           off="{{ properties.offLabel }}"\n           ng-disabled="properties.disabled">\n    </toggle>\n</div>\n\n\n'
    };
  });

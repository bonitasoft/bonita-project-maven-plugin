function SwitchButtonController($scope, $http) {
    
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
    
    
}
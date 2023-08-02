(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('customToast', function() {
    return {
      controllerAs: 'ctrl',
      controller: function Ctrl($scope) {
    
    toastr.options = {
      "closeButton": true,
      "newestOnTop": true,
      "progressBar": true,
      "positionClass": "toast-top-right",
      "preventDuplicates": false,
      "timeOut": $scope.properties.displayTime,
      "extendedTimeOut": $scope.properties.displayTime,
      "showDuration": "300",
      "hideDuration": "300",
      "showEasing": "swing",
      "hideEasing": "linear",
      "showMethod": "fadeIn",
      "hideMethod": "fadeOut"
    };
    
    $scope.$watchCollection("properties.toastsToDisplay", () => {
        if (!$scope.properties.toastsToDisplay || !$scope.properties.toastsToDisplay.length) {
            return;
        }
        var elementToDisplay = $scope.properties.toastsToDisplay[0];
        
        switch(elementToDisplay.type) {
            case "success":
                toastr.success(elementToDisplay.message, elementToDisplay.title);
                break;
            case "info":
                toastr.info(elementToDisplay.message, elementToDisplay.title);
                break;
            case "warning":
                toastr.warning(elementToDisplay.message, elementToDisplay.title);
                break;
            case "error":
                toastr.error(elementToDisplay.message, elementToDisplay.title);
                break;
        }
        
        $scope.properties.toastsToDisplay.shift();
    });
},
      template: '<span ng-if="environment"><identicon name="{{environment.component.id}}" size="30" background-color="[255,255,255, 0]" foreground-color="[51,51,51]"></identicon> {{environment.component.name}}</span>'
    };
  });

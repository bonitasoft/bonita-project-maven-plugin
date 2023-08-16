function WidgetalertController($scope, $sce) {
    
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
}
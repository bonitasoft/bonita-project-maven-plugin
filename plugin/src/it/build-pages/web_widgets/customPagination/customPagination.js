(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('customPagination', function() {
    return {
      controllerAs: 'ctrl',
      controller: function PbDataTableCtrl($scope, $http, $log, $filter) {

  var vm = this;

  vm.pagination = $scope.properties.pagination;

}
,
      template: '<div class="text-{{ properties.alignment }}">\n    <div class="hidden-xs"\n         ng-repeat="options in [{\'maxSize\': 5, \'rotate\': false}]"\n         ng-include="\'/dataTable/pagination.html\'" >\n    </div>\n    <div class="visible-xs text-center"\n         ng-repeat="options in [{\'maxSize\': 3, \'rotate\': true}]"\n         ng-include="\'/dataTable/pagination.html\'" >\n    </div>\n    \n    <script type="text/ng-template" id="/dataTable/pagination.html">\n        <pagination ng-if="ctrl.pagination.total > properties.pageSize"\n                    total-items="ctrl.pagination.total"\n                    items-per-page="ctrl.pagination.pageSize"\n                    direction-links="true"\n                    boundary-links="true"\n                    previous-text="&lsaquo;" next-text="&rsaquo;" first-text="&laquo;" last-text="&raquo;"\n                    rotate="{{options.rotate}}"\n                    max-size="{{options.maxSize}}"\n                    ng-model="ctrl.pagination.currentPage">\n        </pagination>\n    </script>\n</div>\n'
    };
  });

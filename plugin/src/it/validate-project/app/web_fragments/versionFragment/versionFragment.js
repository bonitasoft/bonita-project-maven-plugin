let module;
try {
  module = angular.module('bonitasoft.ui.fragments');
} catch (e) {
  module = angular.module('bonitasoft.ui.fragments', []);
  angular.module('bonitasoft.ui').requires.push('bonitasoft.ui.fragments');
}
module.directive('pbFragmentVersionFragment', function() {
  return {
    template: '<div>    <div class="row">\n        <div pb-property-values=\'1d535a54-ec2a-44bd-81c9-be62b047c84e\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-10  col-lg-10" ng-class="properties.cssClasses">\n        <pb-text></pb-text>\n    </div>\n</div><div pb-property-values=\'85b88ce5-9ee0-4907-8b3c-efd94c3eb169\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-2  col-lg-2" ng-class="properties.cssClasses">\n        <pb-text></pb-text>\n    </div>\n</div>\n    </div>\n</div>'
  };
});

var module;
try {
  module = angular.module('bonitasoft.ui.fragments');
} catch (e) {
  module = angular.module('bonitasoft.ui.fragments', []);
  angular.module('bonitasoft.ui').requires.push('bonitasoft.ui.fragments');
}
module.directive('pbFragmentVersionFragment', function() {
  return {
    template: '<div>    <div class="row">\n        <div pb-property-values=\'2fe74d3f-a7ec-4a85-b159-e04c1fb21316\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-10  col-lg-6" ng-class="properties.cssClasses">\n        <pb-text></pb-text>\n    </div>\n</div><div pb-property-values=\'044f2e36-222c-47b2-ad49-b8f029d645d6\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-2  col-lg-6" ng-class="properties.cssClasses">\n        <pb-button></pb-button>\n    </div>\n</div>\n    </div>\n</div>'
  };
});

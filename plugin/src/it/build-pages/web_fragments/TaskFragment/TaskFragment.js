var module;
try {
  module = angular.module('bonitasoft.ui.fragments');
} catch (e) {
  module = angular.module('bonitasoft.ui.fragments', []);
  angular.module('bonitasoft.ui').requires.push('bonitasoft.ui.fragments');
}
module.directive('pbFragmentTaskFragment', function() {
  return {
    template: '<div>    <div class="row">\n        <div pb-property-values=\'75fb2a8c-b217-498f-901c-1f7b2a814689\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-12  col-lg-12" ng-class="properties.cssClasses">\n        <pb-text></pb-text>\n    </div>\n</div>\n    </div>\n    <div class="row">\n        <div pb-property-values=\'851f5692-7b5e-497b-8080-2aa06ad06c06\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-12  col-lg-12" ng-class="properties.cssClasses">\n        <pb-link></pb-link>\n    </div>\n</div><div pb-property-values=\'506cda6d-4840-4baa-bc16-3b0c6e97eb6d\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-12  col-lg-12" ng-class="properties.cssClasses">\n        <custom-button-with-timeout></custom-button-with-timeout>\n    </div>\n</div>\n    </div>\n    <div class="row">\n        <div pb-property-values=\'b06679ab-815e-46a8-a227-83b086e1b3e8\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-12  col-lg-12" ng-class="properties.cssClasses">\n        <custom-button-with-timeout></custom-button-with-timeout>\n    </div>\n</div>\n    </div>\n</div>'
  };
});

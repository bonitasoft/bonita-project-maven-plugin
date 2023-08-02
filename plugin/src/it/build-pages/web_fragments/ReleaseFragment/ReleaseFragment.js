var module;
try {
  module = angular.module('bonitasoft.ui.fragments');
} catch (e) {
  module = angular.module('bonitasoft.ui.fragments', []);
  angular.module('bonitasoft.ui').requires.push('bonitasoft.ui.fragments');
}
module.directive('pbFragmentReleaseFragment', function() {
  return {
    template: '<div>    <div class="row">\n        <div pb-property-values=\'b9de9683-13dc-4912-a035-19c087fc7c00\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-6  col-lg-6" ng-class="properties.cssClasses">\n        <pb-link></pb-link>\n    </div>\n</div><div pb-property-values=\'5e962bf7-9b71-4059-956e-be51673b6fb9\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-6  col-lg-6" ng-class="properties.cssClasses">\n        <pb-text></pb-text>\n    </div>\n</div>\n    </div>\n    <div class="row">\n        <div pb-property-values=\'a5db87b2-be46-4b24-89c1-2dd53cc9ffe3\'>\n\n  <div class="col-xs-12  col-sm-12  col-md-12  col-lg-12"\n       ng-class="properties.cssClasses"\n       ng-if="!properties.hidden"\n>\n\n        <div class="row">\n        <div pb-property-values=\'945facd7-008f-466d-83c4-9acd9af2c068\'>\n\n  <div class="col-xs-12  col-sm-12  col-md-12  col-lg-12"\n       ng-class="properties.cssClasses"\n       ng-if="!properties.hidden"\n         ng-repeat="$item in ($collection = properties.repeatedCollection) track by $index"\n       >\n\n        <div class="row">\n        <div pb-property-values=\'e20e862a-dea6-40e6-a77a-56860bd45e57\'>\n    <div pb-model=\'VersionFragment\' pb-model-properties=\'e20e862a-dea6-40e6-a77a-56860bd45e57\'>\n        <pb-fragment-version-fragment class="col-xs-12  col-sm-12  col-md-12  col-lg-12" ng-class="properties.cssClasses" ng-if="!properties.hidden">\n        </pb-fragment-version-fragment>\n    </div>\n</div>\n\n    </div>\n\n\n  </div>\n</div>\n\n    </div>\n\n\n  </div>\n</div>\n\n    </div>\n</div>'
  };
});

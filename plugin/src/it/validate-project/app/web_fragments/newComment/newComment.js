let module;
try {
  module = angular.module('bonitasoft.ui.fragments');
} catch (e) {
  module = angular.module('bonitasoft.ui.fragments', []);
  angular.module('bonitasoft.ui').requires.push('bonitasoft.ui.fragments');
}
module.directive('pbFragmentNewComment', function() {
  return {
    template: '<div>    <div class="row">\n        <div pb-property-values=\'1e8609c4-9b03-44e5-a8ca-4b8f340533ed\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-12  col-lg-12" ng-class="properties.cssClasses">\n        <pb-title></pb-title>\n    </div>\n</div>\n    </div>\n    <div class="row">\n        <div pb-property-values=\'fcf716d4-2ac1-435c-88b2-73b0b782544a\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-12  col-lg-12" ng-class="properties.cssClasses">\n        <pb-textarea></pb-textarea>\n    </div>\n</div>\n    </div>\n    <div class="row">\n        <div pb-property-values=\'88b9bb22-7219-4ee5-8999-814df2c928a2\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-6  col-lg-6" ng-class="properties.cssClasses">\n        <pb-button></pb-button>\n    </div>\n</div><div pb-property-values=\'46e35cb3-2ddb-4b7d-ba7e-7cff0f25f27c\'>\n    <div ng-if="!properties.hidden" class="component col-xs-12  col-sm-12  col-md-6  col-lg-6" ng-class="properties.cssClasses">\n        <pb-button></pb-button>\n    </div>\n</div>\n    </div>\n</div>'
  };
});

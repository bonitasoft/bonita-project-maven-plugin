(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('customTaskList', function() {
    return {
      controllerAs: 'ctrl',
      controller: function TaskListCtrl($scope, $http, $location, $log, $window, $timeout,$interval, localStorageService, modalService,  $sce) {

  'use strict';

  var vm = this;
  
  const compare = r => l => (typeof l === "object" ? contains(r)(l) : l === r);
  const contains = r => l => Object.keys(r).every(k => l.hasOwnProperty(k) && compare(r[k])(l[k]));
  
  const converter = new showdown.Converter()
  converter.setOption('emoji',true)
  converter.setOption('openLinksInNewWindow',true)
  converter.setOption('underline', true)
  converter.setFlavor('github');
  
    // store the interval promise in this variable
    var promise;
  
    // simulated items array
    $scope.items = [];
    poll($scope.properties.pollURL, true);
    
    // starts the interval
    $scope.start = function() {
      // stops any running interval to avoid two intervals running at the same time
      $scope.stop(); 
      // store the interval promise
      promise = $interval(() => poll($scope.properties.pollURL, false), 1500);
    };
  
    // stops the interval
    $scope.stop = function() {
      $scope.ignorePollingResult = true;
      $interval.cancel(promise);
    };
  
    // starting the interval by default
    $scope.start();
 
    // stops the interval when the scope is destroyed,
    // this usually happens when a route is changed and 
    // the ItemsController $scope gets destroyed. The
    // destruction of the ItemsController scope does not
    // guarantee the stopping of any intervals, you must
    // be responsible for stopping it when the scope is
    // is destroyed.
    $scope.$on('$destroy', function() {
      $scope.stop();
    });

  this.noPendingTasks = function noPendingTasks(){
      return $scope.items.length === 0 && $scope.polled;
  };

  this.getTaskFormHref = function getTaskFormHref(taskId) {
      var appToken = getAppToken('app');
      let href = getPortalUrl() + '/portal/form/taskInstance/' + taskId;
      if(appToken){
          href = href + "?app=" + appToken;
      }
      return href;
  };
  
  $scope.timeTo = function timeTo(date){
      dayjs.extend(window.dayjs_plugin_relativeTime);
      return dayjs().to(dayjs(date));
  }

  this.mdToHtml = function mdToHtml(mkdwn) {
     return $sce.getTrustedHtml(converter.makeHtml(mkdwn));
  };
  
  this.submitTask = function(item){
       doRequest(item, 'POST', `../API/bpm/userTask/${item.id}/execution`, {assign:true}, {});
  };
  
  this.triggerEvent = function(item){
      doRequest(item, 'PUT', `../API/bpm/timerEventTrigger/${item.id}`,{}, {executionDate: Date.now()});
  }
  
  function poll(url, force) {
     var req = {
      method: 'GET',
      url: url,
      data: {},
      params: {}
    };

    $scope.ignorePollingResult = false;
    return $http(req)
      .success(function(data, status) {
          if(!$scope.ignorePollingResult || force){
            $scope.items = filter(data);
            $scope.polled = true;
          }
      });
  }
  
  function filter(data){
      if($scope.properties.filter && $scope.properties.filter !== 'All'){
          return data.filter(contains($scope.properties.filter));
      }
      return data;
  }

  /**
   * Execute a get/post request to an URL
   * It also bind custom data from success|error to a data
   * @return {void}
   */
  function doRequest(item, method, url, params, data) {
    $scope.stop();
    item.busy = true;
  
    var req = {
      method: method,
      url: url,
      data: data,
      params: params
    };
    
    return $http(req)
      .success(function(data, status) {
        $timeout(() => {
            item.done = true;
            item.busy = false;
             $scope.properties.responseStatusCode = status;
             $scope.polled = false;
             $scope.start();
             $timeout(() => {
                  var index = $scope.items.indexOf(item);
                  if(index !== -1){
                    $scope.items.splice(index, 1);     
                  }
             }
             , 500);
          
        },  2000);
      
      })
      .error(function(data, status) {
        $scope.properties.dataFromError = data;
        $scope.properties.responseStatusCode = status;
        $scope.properties.dataFromSuccess = undefined;
        notifyParentFrame({ message: 'error', status: status, dataFromError: data, dataFromSuccess: undefined, responseStatusCode: status});
        item.busy = false;
        $scope.start();
      });
  }
  
  function notifyParentFrame(additionalProperties) {
    if ($window.parent !== $window.self) {
      var dataToSend = angular.extend({}, $scope.properties, additionalProperties);
      $window.parent.postMessage(JSON.stringify(dataToSend), '*');
    }
  }


  function getUserParam() {
    var userId = getUrlParam('user');
    if (userId) {
      return { 'user': userId };
    }
    return {};
  }

  /**
   * Extract the param value from a URL query
   * e.g. if param = "id", it extracts the id value in the following cases:
   *  1. http://localhost/bonita/portal/resource/process/ProcName/1.0/content/?id=8880000
   *  2. http://localhost/bonita/portal/resource/process/ProcName/1.0/content/?param=value&id=8880000&locale=en
   *  3. http://localhost/bonita/portal/resource/process/ProcName/1.0/content/?param=value&id=8880000&locale=en#hash=value
   * @returns {id}
   */
  function getUrlParam(param) {
    var paramValue = $location.absUrl().match('[//?&]' + param + '=([^&#]*)($|[&#])');
    if (paramValue) {
      return paramValue[1];
    }
    return '';
  }
  
   function getPortalUrl() {
    var locationHref = $location.absUrl();
    var indexOfPortal = locationHref.indexOf('/portal/');
    if (indexOfPortal >= 0) {
      return locationHref.substring(0, indexOfPortal);
    } else {
      //in case of a layout instead of a page/form, the servlet mapping is /apps/* instead of /portal/*
      var indexOfApps = locationHref.indexOf('/apps/');
      if (indexOfApps >= 0) {
        return locationHref.substring(0, indexOfApps);
      } else {
        //Make the link work in case we are in the preview and the target process is deployed in the portal
        return '/bonita';
      }
    }
  }
  
   function getAppToken(paramName) {
    if ($scope.properties.appToken) {
      return $scope.properties.appToken;
    }
    var appTokenParam = getUrlParam(paramName);
    if (appTokenParam) {
      return appTokenParam;
    }
    var appsURLPattern = '\/apps\/([^/]*)\/';
    var urlMatches = $location.absUrl().match(appsURLPattern) || $window.top.location.href.match(appsURLPattern);
    if (urlMatches) {
      return urlMatches[1];
    }
    return null;
  }
  

}
,
      template: '<span ng-if="environment"><identicon name="{{environment.component.id}}" size="30" background-color="[255,255,255, 0]" foreground-color="[51,51,51]"></identicon> {{environment.component.name}}</span>\n<div class="card-container fade-in fade-out">\n    <div class="text-center mt-5" ng-if="ctrl.noPendingTasks()">\n        <h3><span class="fa fa-info-circle"></span> You have no pending tasks {{properties.filter !== "All" ? "for current filtered version" : undefined }}</h3>\n    </div>\n    <div class="card primary-border" ng-repeat="item in items track by item.id">\n        <h5>\n            <span ng-if="item.version " class="label label-danger"> <span class="fa fa-tag"></span> &nbsp;{{item.version}}</span>\n            <span ng-if="item.versionStage" class="label label-info"><em>{{item.versionStage}}</em></span>\n            <span ng-if="item.releaseType" class="label label-info"><em>{{item.releaseType}}</em></span>\n            <span ng-if="item.process" class="label label-default"><em>{{item.process}}</em></span>\n        \n            <a ng-if="item.mdFileLocation" target="_blank" class="pull-right" ng-href="{{ item.mdFileLocation }}" title="Edit me in Github"><i class="black fa fa-github"></i></a>\n       </h5>\n\n       <strong>{{item.displayName}}</strong>\n       <p ng-if="item.displayDescription " class="text-muted"  ng-bind-html="item.displayDescription"></p>\n       <p ng-if="item.content" ng-bind-html="ctrl.mdToHtml(item.content)"></p>\n       <p ng-if="item.executionDate"><i class="fa fa-hourglass-half"></i> Triggered {{timeTo(item.executionDate)}}</p>\n  \n       <div ng-if="item.hasForm"  class="text-right">\n           <a class="btn btn-sm btn-primary" ng-href="{{ctrl.getTaskFormHref(item.id)}}" target="_self">\n           <span class="fa fa-lg fa-external-link pt-xs"></span>\n         </a>\n        </div>\n        <div ng-if="!item.hasForm" class="text-right">\n             <button\n                id="submit"\n                class="btn btn-sm btn-primary no-animate"\n                ng-click="ctrl.submitTask(item)"\n                type="button"\n                ng-if="!item.done && !item.busy && !item.executionDate">\n                <i class="fa fa-check pt-xs"></i>\n             </button>\n             <button\n                id="trigger-event"\n                class="btn btn-sm btn-danger no-animate"\n                ng-click="ctrl.triggerEvent(item)"\n                type="button"\n                ng-if="!item.done && !item.busy && item.executionDate">\n                <i class="fa fa-lg fa-exclamation-circle" aria-hidden="true"></i> Trigger now\n             </button>\n             <div class="no-animate" ng-if="item.busy && !item.done">\n                 <svg class="loading" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">\n                    <circle cx="50" cy="50" r="45"/>\n                </svg>\n            </div>\n            <i ng-if="!item.busy && item.done" class="fa fa-check-circle-o text-success task-success" aria-hidden="true"></i>\n        </div>\n    </div>\n</div>\n'
    };
  });

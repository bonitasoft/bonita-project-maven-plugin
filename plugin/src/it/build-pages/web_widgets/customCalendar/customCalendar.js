(function () {
  try {
    return angular.module('bonitasoft.ui.widgets');
  } catch(e) {
    return angular.module('bonitasoft.ui.widgets', []);
  }
})().directive('customCalendar', function() {
    return {
      controllerAs: 'ctrl',
      controller: 
function ($scope) {
    //https://github.com/year-calendar/js-year-calendar
    const ctrl = this;
    const calendar = new Calendar('.calendar');

    calendar.setStyle('background');
    
    document.querySelector('.calendar').addEventListener('mouseOnDay', function(e) {
        ctrl.handleDayEnter(e)
    })
    
    document.querySelector('.calendar').addEventListener('mouseOutDay', function(e) {
        ctrl.handleDayLeave()
    })
    
    ctrl.handleDayEnter = function(e) {
    if (e.events.length > 0) {
      var content = '';
                
      for (var i in e.events) {
        content += '<div class="event-tooltip-content">'
          + '<div class="event-name" style="color:' + e.events[i].color + '">' + e.events[i].name + '</div>'
          + '<div class="event-details">' + e.events[i].details + '</div>'
          + '</div>';
      }
      
      if (ctrl.tooltip != null) {
        ctrl.tooltip.destroy();
        ctrl.tooltip = null;
      }
      
      ctrl.tooltip = tippy(e.element, {
          placement: 'right',
          content: content,
          animateFill: false,
          animation: 'shift-away',
          arrow: true
      });
      ctrl.tooltip.show();
    }
  }
    
  ctrl.handleDayLeave = function() {
    if (ctrl.tooltip !== null) {
      ctrl.tooltip.destroy();
      ctrl.tooltip = null;
    }
  }
    
    $scope.$watch('properties.startDate', function(startDate) {
        calendar.setMinDate(new Date(startDate));
    });
    
    $scope.$watch('properties.endDate', function(endDate) {
        calendar.setMaxDate(new Date(endDate));
    });
    
    $scope.$watch('properties.events', function(events) {
        calendar.setDataSource(events);
    });
    
    //calendar.setMinDate($scope.properties.startDate);
    //calendar.setMaxDate($scope.properties.endDate);
    //calendar.setDataSource($scope.properties.events)
    }
,
      template: '<div class="calendar"></div>'
    };
  });

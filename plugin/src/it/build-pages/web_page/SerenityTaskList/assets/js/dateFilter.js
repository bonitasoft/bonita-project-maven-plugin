angular.module('bonitasoft.ui.extensions')
 .filter('timeTo', [function () {
   return function timeTo(date) {
     dayjs.extend(window.dayjs_plugin_relativeTime);
     return dayjs().to(dayjs(date));
   };
}]).filter('statusToCss', [function () {
   return function statusToCss(status) {
     switch(status){
         case 'In progress': return 'warning';
         case 'Out of engineering': return 'primary';
         case 'Released': return 'success';
         case 'Completed': return 'success';
         case 'Late': return 'danger';
         case 'At risk': return 'danger';
         case 'Requested': return 'default';
         case 'Validating': return 'warning';
         default: return 'default';
     }
   };
}]);

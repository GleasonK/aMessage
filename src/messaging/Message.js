(function(){
  'use strict';

  // Prepare the 'users' module for subsequent registration of controllers and delegates
  angular.module('messaging', [ 'ngMaterial'])
  .directive('enterSubmit', function () {
    return {
      restrict: 'A',
      link: function (scope, elem, attrs) {
        elem.bind('keydown', function(event) {
          var code = event.keyCode || event.which;
          if (code === 13) {
            if (!event.shiftKey) {
              event.preventDefault();
              scope.$apply(attrs.enterSubmit);
            }
          }
        });
      }
    }
  });


})();

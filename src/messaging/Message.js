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

  if(!String.linkify) {
    String.prototype.linkify = function() {

      // http://, https://, ftp://
      var urlPattern = /\b(?:https?|ftp):\/\/[a-z0-9-+&@#\/%?=~_|!:,.;]*[a-z0-9-+&@#\/%=~_|]/gim;

      // www. sans http:// or https://
      var pseudoUrlPattern = /(^|[^\/])(www\.[\S]+(\b|$))/gim;

      // Email addresses
      var emailAddressPattern = /[\w.]+@[a-zA-Z_-]+?(?:\.[a-zA-Z]{2,6})+/gim;

      return this
          .replace(urlPattern, '<a target="_blank" href="$&">$&</a>')
          .replace(pseudoUrlPattern, '$1<a target="_blank" href="http://$2">$2</a>')
          .replace(emailAddressPattern, '<a href="mailto:$&">$&</a>');
    };
  }

  if (!String.fix){
    var entityMap = {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': '&quot;',
      "'": '&#39;',
      "/": '&#x2F;'
    };
    String.prototype.fix = function(){
      return this.replace(/[&<>"'\/]/g, function (s) {
        return entityMap[s];
      });
    }
  }

})();

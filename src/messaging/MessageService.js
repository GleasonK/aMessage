(function(){
  'use strict';

  angular.module('messaging')
         .service('userService', ['$q', UserService]);

  /**
   * Users DataService
   * Uses embedded, hard-coded data model; acts asynchronously to simulate
   * remote data service call(s).
   *
   * @returns {{loadAll: Function}}
   * @constructor
   */
  function UserService($q, Pubnub){
    
    var getUrlParameter = function getUrlParameter(sParam) {
        var sPageURL = decodeURIComponent(window.location.search.substring(1)),
            sURLVariables = sPageURL.split('&'),
            sParameterName,
            i;

        for (i = 0; i < sURLVariables.length; i++) {
            sParameterName = sURLVariables[i].split('=');

            if (sParameterName[0] === sParam) {
                return sParameterName[1] === undefined ? true : sParameterName[1];
            }
        }
    };

    function threadIndex(threads, tId) {
      for (var i = 0; i < threads.length; i++) {
        if (threads[i].thread === tId) {
          return i;
        }
      }
      return -1;
    }

    function getAvatar(thread) {
        var idx = 1 + Math.abs(thread.hashCode() % 13);
        console.log("Index " + idx);
        return 'svg-' + idx;
    }

    function updateSent(msg, idx, threads){
      if (idx==-1) return;
      var user = threads[idx];
      var msgs = user.messages;
      for(var i=0; i<msgs.length; i++){
        var m = msgs[i];
        if (m.timestamp == msg.timestamp){
          m.isSent = true;
          console.log(m);
          return;
        }
      }
    }

    function processUsers(msgs, resolve){
      var threads = [];
      for (var i = 0; i < msgs.length; i++) {
        var msg = msgs[i];
        console.log(msg);
        var thread = msg.sender ? msg.sender : msg.number;
        if (!thread) continue;
        var idx = threadIndex(threads,thread);
        
        if (msg.type == "receipt") updateSent(msg,idx,threads);
        if (msg.type != "incoming" && msg.type != "outgoing") continue;

        if (idx != -1){
          var user = threads.splice(idx,1)[0];
          if (msg.sender) user.name = msg.name;
          user.messages.push(msg);
          threads.unshift(user);
        } else {
          var name = msg.sender ? msg.name : thread;
          var avatar = getAvatar(thread);
          var messages = [msg];
          threads.unshift({name:name, avatar:avatar, messages:messages, thread:thread});
        }
      }
      resolve(threads);
    }

    function notify(msg){
      if(! ('Notification' in window) ){
        console.log('Web Notification not supported');
        return;
      }   

      Notification.requestPermission(function(permission){
          var notification = new Notification(msg.name,{body:msg.message,icon:'http://icons.iconarchive.com/icons/dtafalonso/android-lollipop/512/Messenger-icon.png', dir:'auto'});
          setTimeout(function(){
              notification.close();
          },3500);
      });
    }

    String.prototype.hashCode = function() {
      var hash = 0, i, chr, len;
      if (this.length === 0) return hash;
      for (i = 0, len = this.length; i < len; i++) {
        chr   = this.charCodeAt(i);
        hash  = ((hash << 5) - hash) + chr;
        hash |= 0; // Convert to 32bit integer
      }
      return hash;
    };

    // Promise-based API
    return {
      loadAll : function() {
        var number = getUrlParameter("n");
        var cipher = getUrlParameter("c");
        if (!number || !cipher) {
          window.location.replace("login.html");
        }
        console.log(cipher);
        // Simulate async nature of real remote calls
        var pubnub = PUBNUB.init({
            publish_key: 'pub-c-0181d825-aa47-448a-bd4c-bcf1ba2a8623',
            subscribe_key: 'sub-c-e4f06386-ec67-11e5-be6a-02ee2ddab7fe',
            cipher_key : cipher,
            ssl: true
        });

        return $q(function(resolve,reject){
          pubnub.history({
            channel : number,
            count : 100,
            callback : function(m){
              if (m[0]){
                processUsers(m[0], resolve);
              } else {
                reject([]);
              }
            }
          });
          setTimeout(function(){
            reject();
          },5000);
        });
      },
      getAvatar : getAvatar,
      getUrlParameter : getUrlParameter,
      notify : notify
    };
  }

})();

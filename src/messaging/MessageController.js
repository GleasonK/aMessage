(function(){

  angular
       .module('messaging')
       .controller('UserController', [
          'userService', '$mdSidenav', '$mdBottomSheet', '$log', '$q', '$scope', '$timeout',
          UserController
       ]);

  /**
   * Main Controller for the Angular Material Starter App
   * @param $scope
   * @param $mdSidenav
   * @param avatarsService
   * @constructor
   */
  function UserController( userService, $mdSidenav, $mdBottomSheet, $log, $q, $scope, $timeout, Pubnub) {
    var self = this;
    var myNumber = userService.getUrlParameter("n");
    var cipher   = userService.getUrlParameter("c");
    var authkey  = userService.getUrlParameter("a");
    var myName   = "Me"; // Edit my name 

    console.log($scope);

    self.selected     = null;
    self.selectUser   = selectUser;
    self.toggleList   = toggleUsersList;
    self.sendMessage  = sendMessage;
    self.newMessage   = newMessage;
    self.openPhone    = openPhone;
    self.message      = "";

    $scope.users      = [ ];

    function scrollBottom(){
      $timeout(function() {
        var scroller = document.getElementById("content");
        scroller.scrollTop = scroller.scrollHeight;
      }, 0, false);
    }

    var pubnub = PUBNUB.init({
            publish_key: 'pub-c-0181d825-aa47-448a-bd4c-bcf1ba2a8623',
            subscribe_key: 'sub-c-e4f06386-ec67-11e5-be6a-02ee2ddab7fe',
            auth_key: authkey,
            cipher_key : cipher,
            ssl: true
        });

    function threadIndex(threads, tId) {
      for (var i = 0; i < threads.length; i++) {
        if (threads[i].thread === tId) {
          return i;
        }
      }
      return -1;
    }

    function updateSent(msg){
      var thread = msg.sender ? msg.sender : msg.number;
      var idx = threadIndex($scope.users, thread)
      if (idx==-1) return;
      var user = $scope.users[idx];
      var msgs = user.messages;
      for(var i=0; i<msgs.length; i++){
        var m = msgs[i];
        if (m.timestamp == msg.timestamp){
          m.isSent = true;
          $scope.$apply();
          console.log(m);
          return;
        }
      }
    }

    pubnub.subscribe({
      channel: myNumber,
      message: function(msg){
        var copy = JSON.parse(JSON.stringify(msg));
        if (msg.type == "incoming") userService.notify(copy);
        console.log(msg);
        if (msg.type == "receipt") updateSent(msg);
        if (msg.type != "incoming" && msg.type != "outgoing" && msg.type != "command") return;
        addMessage(msg);
        scrollBottom();
        $scope.$apply();
      },
      error: function(err){ console.log(err);}
    });

    // Load all registered users

    userService
          .loadAll()
          .then( function( users ) {
            console.log(users);
            $scope.users    = [].concat(users);
            self.selected = users[0];
            scrollBottom();
          });

    // *********************************
    // Internal methods
    // *********************************

    /**
     * Hide or Show the 'left' sideNav area
     */
    function toggleUsersList() {
      $mdSidenav('left').toggle();
    }

    /**
     * Select the current avatars
     * @param menuId
     */
    function selectUser ( user ) {
      self.selected = angular.isNumber(user) ? $scope.users[user] : user;
      scrollBottom();
    }

    function sendMessage() {
      if (!self.selected) return;
      if (!self.message) return;
      
      var data = {
        type:"outgoing",
        number:self.selected.thread,
        message:self.message,
        name:myName,
        timestamp:Date.now()
      };

      self.message="";

      if (self.selected.thread == myNumber && data.message.charAt(0)=="/"){
        data.type = "command";
        if (data.message=="/help"){
          data.message = displayHelp();
          return addMessage(data);
        }
      }

      // self.users[0].messages.push(data);
      // return;
      pubnub.publish({
         channel: myNumber,        
         message: data,
         callback : function(m){
          console.log(m);
        }
      });
    }

    function newMessage(){
      var number = prompt("Enter phone number...");
      messageTo(number);
    }

    function openPhone(){
      messageTo(myNumber);
    }

    function displayHelp(){
      return "/vup => Volume Up; "+
             "/vdown => Volume Down; "+
             "/play => Play Media; " +
             "/pause => Pause Media; " +
             "/next => Next Song; " +
             "/prev => Previous Song;"
    }

    function addMessage(msg){
      var thread = msg.sender ? msg.sender : msg.number;
      var idx = threadIndex($scope.users, thread);
      msg.message = userService.displayString(msg.message);
      if (idx==-1){ // New thread
          var name = msg.sender ? msg.name : thread;
          var avatar = userService.getAvatar(thread);
          var messages = [msg];
          $scope.users.unshift({name:name, avatar:avatar, messages:messages, thread:thread});
        } else {
          var user = $scope.users.splice(idx,1)[0];
          if (msg.sender) user.name = msg.name;
          user.messages.push(msg);
          $scope.users.unshift(user);
        }
    }

    function messageTo(thread){
      var idx = threadIndex($scope.users, thread);
      console.log(idx);
      if (idx==-1){ // New thread
        var name = thread;
        var avatar = userService.getAvatar(thread);
        $scope.users.unshift({name:name, avatar:avatar, messages:[], thread:thread});
      } else {
        var user = $scope.users.splice(idx,1)[0];
        $scope.users.unshift(user);
        console.log(user);
      }
      self.selectUser(0);
      scrollBottom();
    }

  }

})();

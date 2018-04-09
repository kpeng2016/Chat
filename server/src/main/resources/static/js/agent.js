'use strict';

var registerPage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var maxCountClient = document.querySelector(
    '#set-maximum-number-of-clients-page');
var registerForm = document.querySelector('#usernameForm');
var signInForm = document.querySelector('#signInForm');
var numberClientForm = document.querySelector('#numberClientForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var checkRegister = document.querySelector('.checkRegister');
var connectingElement = document.querySelector('.connecting');
var username = document.querySelector('#nameSignUpAgent').value.trim();

var stompClient = null;
var userId = null;
var to = null;
var userMessage = null;
var subscription = null;
var message = null;
var nameInterlocutor = null;
var mapName = new Map();

var colors = [
  '#2196F3', '#32c787', '#00BCD4', '#ff5652',
  '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function registerConnect(event) {
  if (username) {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, onConnectedRegister, onError);
  }
  event.preventDefault();
}

function signInConnect(event) {
  username = document.querySelector('#nameSignInAgent').value.trim();
  if (username) {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, onConnectedSignIn, onError);
  }
  event.preventDefault();
}

function onConnectedRegister() {
  subscription = stompClient.subscribe('/topic/start', onMessageReceived);
  stompClient.send("/app/chat.addUser",
      {},
      JSON.stringify({
        senderId: null,
        text: '/register agent ' + username,
        typeOfMessage: 'MESSAGE_CHAT'
      })
  );
}

function onConnectedSignIn() {
  subscription = stompClient.subscribe('/topic/start', onMessageReceived);
  stompClient.send("/app/chat.signInUser",
      {},
      JSON.stringify({
        senderId: null,
        text: '/sign in agent ' + username,
        typeOfMessage: 'MESSAGE_CHAT'
      })
  );
}

function setPassword() {
  var password = document.querySelector('#passwordSignUp').value.trim();
  var encodedString = btoa(password);
  stompClient.send("/app/chat.setPassword",
      {},
      JSON.stringify({
        senderId: userId,
        text: encodedString,
        typeOfMessage: 'MESSAGE_CHAT'
      })
  );
}

function checkPassword() {
  var password = document.querySelector('#passwordSignIn').value.trim();
  var encodedString = btoa(password);
  stompClient.send("/app/chat.checkPassword",
      {},
      JSON.stringify({
        senderId: userId,
        text: encodedString,
        typeOfMessage: 'MESSAGE_CHAT'
      })
  );
}

function setMaxClientsNumber() {
  var maxClients = document.querySelector(
      '#maximum-number-of-clients').value.trim();
  if (maxClients) {
    stompClient.send("/app/chat.setMaxClients",
        {},
        JSON.stringify({
          senderId: userId,
          text: maxClients,
          typeOfMessage: 'MESSAGE_CHAT'
        })
    );
  }
  event.preventDefault();
}

function onError(error) {
  connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
  connectingElement.style.color = 'red';
}

function sendMessage(event) {
  var messageContent = messageInput.value.trim();
  var messageElement = document.createElement('li');
  var activeTab = document.querySelector('.tab-pane.fade.active.show');
  if(activeTab){
    to = $(activeTab).closest("div").prop("id").substring(3);
  }
  if (messageContent && stompClient) {
    userMessage = {
      senderId: userId,
      text: messageInput.value,
      typeOfMessage: 'MESSAGE_CHAT',
      to: to,
      senderName: username
    };
    if (!to) {
      messageElement.classList.add('chat-message');
      settingMessageUser(messageElement);
      printMessage(messageElement, userMessage);
      stompClient.send("/app/chat.search", {}, JSON.stringify(userMessage));
    } else {
      messageElement.classList.add('chat-message');
      settingMessageUser(messageElement);
      printMessage(messageElement, userMessage);
      stompClient.send("/app/chat.sendMessageInterlocutor", {},
          JSON.stringify(userMessage));
    }
    messageInput.value = '';
  }
  event.preventDefault();
}

function onMessageReceived(payload) {
  var serverMessage = JSON.parse(payload.body);
  var messageElement = document.createElement('li');
  switch (serverMessage.typeOfMessage) {
    case 'INCORRECT_REGISTRATION_DATA':
      subscription.unsubscribe('/topic/start', onMessageReceived);
      checkRegister.textContent = 'Wrong data, maybe this name is already taken, please refresh this page to try again!';
      checkRegister.style.color = 'red';
      break;
    case 'INCORRECT_LOGIN_NAME':
      subscription.unsubscribe('/topic/start', onMessageReceived);
      checkRegister.textContent = 'Wrong data, this name is not registered yet, please refresh this page to try again!';
      checkRegister.style.color = 'red';
      break;
    case 'INCORRECT_LOGIN_PASSWORD':
      subscription.unsubscribe('/topic/start', onMessageReceived);
      checkRegister.textContent = 'Wrong password, please refresh this page to try again!';
      checkRegister.style.color = 'red';
      break;
    case 'CORRECT_REGISTRATION':
      subscription.unsubscribe('/topic/start', onMessageReceived);
      userId = serverMessage.senderId;
      subscription = stompClient.subscribe('/topic/' + userId,
          onMessageReceived);
      setPassword();
      registerPage.classList.add('hidden');
      maxCountClient.classList.remove('hidden');
      break;
    case 'CORRECT_LOGIN_NAME':
      subscription.unsubscribe('/topic/start', onMessageReceived);
      userId = serverMessage.senderId;
      subscription = stompClient.subscribe('/topic/' + userId,
          onMessageReceived);
      checkPassword();
      break;
    case 'CORRECT_LOGIN_PASSWORD':
      registerPage.classList.add('hidden');
      chatPage.classList.remove('hidden');
      connectingElement.classList.add('hidden');
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'CONNECTED_CLIENT':
      nameInterlocutor = serverMessage.senderName;
      to = serverMessage.senderId;
      mapName.set(to, nameInterlocutor);
      $('#tab-list').append($('<li><a href="#tab' + to + '" role="tab" data-toggle="tab">' + nameInterlocutor + '<button id = "button'
          + to + '" disabled="disabled" class = "close"  type="button" title="Remove this page">&#10006</button></a></li>'));
      $('#tab-content').append($('<div class="tab-pane fade" id="tab' + to + '"><ul id="messageArea' + to + '"></ul></div>'));
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'MESSAGE_CHAT':
      to = serverMessage.senderId;
      messageElement.classList.add('chat-message');
      settingMessageInterlocutor(messageElement, serverMessage);
      printMessage(messageElement, serverMessage);
      break;
    case 'INCORRECT_DATA_MAX_COUNT_CLIENTS':
    case 'CORRECT_DATA_MAX_COUNT_CLIENTS':
    case 'NO_FREE_AGENT':
    case 'FIRST_MESSAGE_AGENT':
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'END_DIALOGUE':
      to = serverMessage.senderId;
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      var remove = document.querySelector('#button' + to);
      remove.removeAttribute('disabled');
      break;
    case 'LEAVE_CLIENT':
    case 'DISCONNECTION_OF_THE_CLIENT':
      message = {
        senderId: userId,
        text: 'The client left the chat, wait until the new client connects or closes the page to exit the network',
        typeOfMessage: serverMessage.typeOfMessage
      };
      messageElement.classList.add('event-message');
      printMessage(messageElement, message);
      mapName.delete(to);
      break;
    case 'noClientInQueue':
      break;
    default:
      alert('error');
  }
}

function settingMessageInterlocutor(messageElement) {
  nameInterlocutor = mapName.get(to);
  var avatarElement = document.createElement('i');
  var avatarText = document.createTextNode(nameInterlocutor[0]);
  avatarElement.appendChild(avatarText);
  avatarElement.style['background-color'] = getAvatarColor(to);
  messageElement.appendChild(avatarElement);
  var usernameElement = document.createElement('span');
  var usernameText = document.createTextNode(nameInterlocutor);
  usernameElement.appendChild(usernameText);
  messageElement.appendChild(usernameElement);
}

function settingMessageUser(messageElement) {
  var avatarElement = document.createElement('i');
  var avatarText = document.createTextNode(username[0]);
  avatarElement.appendChild(avatarText);
  avatarElement.style['background-color'] = getAvatarColor(userId);
  messageElement.appendChild(avatarElement);
  var usernameElement = document.createElement('span');
  var usernameText = document.createTextNode(username);
  usernameElement.appendChild(usernameText);
  messageElement.appendChild(usernameElement);
}

function printMessage(messageElement, message) {
  var messageArea = document.querySelector('#messageArea' + to);
  var textElement = document.createElement('p');
  var messageText = document.createTextNode(message.text);
  textElement.appendChild(messageText);
  messageElement.appendChild(textElement);
  messageArea.appendChild(messageElement);
  messageArea.scrollTop = messageArea.scrollHeight;
}

function getAvatarColor(messageSender) {
  var hash = 0;
  for (var i = 0; i < messageSender.length; i++) {
    hash = 31 * hash + messageSender.charCodeAt(i);
  }
  var index = Math.abs(hash % colors.length);
  return colors[index];
}

$(document).ready(function () {
  $('#tab-list').on('click', '.close', function () {
    var tabID = $(this).parents('a').attr('href');
    to = tabID;
    $(this).parents('li').remove();
    $(tabID).remove();


  });
});

registerForm.addEventListener('submit', registerConnect, true);
signInForm.addEventListener('submit', signInConnect, true);
numberClientForm.addEventListener('submit', setMaxClientsNumber, true);
messageForm.addEventListener('submit', sendMessage, true);
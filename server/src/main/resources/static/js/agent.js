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
var messageArea = document.querySelector('#messageArea');
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
var listIdClients = [];

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
  if (messageContent && stompClient) {
    userMessage = {
      senderId: userId,
      text: messageInput.value,
      typeOfMessage: 'MESSAGE_CHAT',
      to: to,
      nameTo: nameInterlocutor
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
    case 'INCORRECT_DATA_MAX_COUNT_CLIENTS':
      connectingElement.textContent = 'Wrong data, please to try again!';
      connectingElement.style.color = 'red';
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
    case 'CORRECT_DATA_MAX_COUNT_CLIENTS':
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'CONNECTED_CLIENT':
      nameInterlocutor = serverMessage.nameTo;
      to = serverMessage.to;
      $('<li><a href="#tab'+to+'" data-toggle="tab">'+nameInterlocutor+'</a></li>').appendTo('#tabs');
      $('#tabs a:last').tab('show');
      listIdClients.push(to);
      messageElement.classList.add('event-message');
      $('<div class="tab-pane" id="tab'+to+'">'+printMessage(messageElement, serverMessage) +'</div>').appendTo('.tab-content');
      //printMessage(messageElement, serverMessage);
      break;
    case 'END_DIALOGUE':
      messageElement.classList.add('event-message');
      $('<div class="tab-pane" id="tab'+to+'">'+printMessage(messageElement, serverMessage) +'</div>').appendTo('.tab-content');
      //printMessage(messageElement, serverMessage);
      to = serverMessage.to;
      listIdClients.remove(listIdClients.indexOf(to));
      nameInterlocutor = serverMessage.nameTo;
      break;
    case 'MESSAGE_CHAT':
      to = serverMessage.to;
      nameInterlocutor = serverMessage.nameTo;
      messageElement.classList.add('chat-message');
      var avatarElement = document.createElement('i');
      var avatarText = document.createTextNode(nameInterlocutor[0]);
      avatarElement.appendChild(avatarText);
      avatarElement.style['background-color'] = getAvatarColor(userId);
      messageElement.appendChild(avatarElement);
      var usernameElement = document.createElement('span');
      var usernameText = document.createTextNode(nameInterlocutor);
      usernameElement.appendChild(usernameText);
      messageElement.appendChild(usernameElement);
      printMessage(messageElement, serverMessage);
      break;
    case 'NO_FREE_AGENT':
    case 'FIRST_MESSAGE_AGENT':
    case 'AGENT_CANT_LEAVE':
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'LEAVE_CLIENT':
    case 'DISCONNECTION_OF_THE_CLIENT':
      message = {
        senderId: serverMessage.senderId,
        text: 'The client left the chat, wait until the new client connects or closes the page to exit the network',
        typeOfMessage: serverMessage.typeOfMessage
      };
      messageElement.classList.add('event-message');
      printMessage(messageElement, message);
      break;
    case 'noClientInQueue':
      break;
    default:
      alert('error');
  }
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

registerForm.addEventListener('submit', registerConnect, true);
signInForm.addEventListener('submit', signInConnect, true);
numberClientForm.addEventListener('submit', setMaxClientsNumber, true);
messageForm.addEventListener('submit', sendMessage, true);
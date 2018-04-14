'use strict';

var registerPage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var registerForm = document.querySelector('#usernameForm');
var signInForm = document.querySelector('#signInForm');
var messageForm = document.querySelector('#messageForm');
var messageLeave = document.querySelector('#userLeave');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var checkRegister = document.querySelector('.checkRegister');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
var username = null;
var userId = null;
var to = null;
var messageList = [];
var userMessage = null;
var subscription = null;
var message = null;
var nameInterlocutor = null;

var colors = [
  '#2196F3', '#32c787', '#00BCD4', '#ff5652',
  '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function registerConnect(event) {
  username = document.querySelector('#nameSignUp').value.trim();
  if (username) {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, onConnectedRegister, onError);
  }
  event.preventDefault();
}

function signInConnect(event) {
  username = document.querySelector('#nameSignIn').value.trim();
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
        text: '/register client ' + username,
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
        text: '/sign in client ' + username,
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

function onError(error) {
  connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
  connectingElement.style.color = 'red';
}

function leave(event) {
  userMessage = {
    senderId: userId,
    text: 'leave',
    typeOfMessage: 'MESSAGE_CHAT',
    to: to,
    senderName: username
  };
  stompClient.send("/app/chat.leave", {}, JSON.stringify(userMessage));
  event.preventDefault();
}

function sendMessage(event) {
  var messageElement = document.createElement('li');
  var messageContent = messageInput.value.trim();
  if (messageContent && stompClient) {
    userMessage = {
      senderId: userId,
      text: messageInput.value,
      typeOfMessage: 'MESSAGE_CHAT',
      to: to,
      senderName: username
    };
    if (!to) {
      messageList.push(userMessage.text);
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

function sendMessageClientBeforeStartedDialogue() {
  for (var i = 0; i < messageList.length; i++) {
    userMessage = {
      senderId: userId,
      text: messageList[i],
      typeOfMessage: 'MESSAGE_CHAT',
      to: to,
      senderName: username
    };
    stompClient.send("/app/chat.sendMessageInterlocutor", {},
        JSON.stringify(userMessage));
  }
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
      subscription = stompClient.subscribe('/topic/' + userId, onMessageReceived);
      setPassword();
      registerPage.classList.add('hidden');
      chatPage.classList.remove('hidden');
      connectingElement.classList.add('hidden');
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'CORRECT_LOGIN_NAME':
      subscription.unsubscribe('/topic/start', onMessageReceived);
      userId = serverMessage.senderId;
      subscription = stompClient.subscribe('/topic/' + userId, onMessageReceived);
      checkPassword();
      break;
    case 'CORRECT_LOGIN_PASSWORD':
      registerPage.classList.add('hidden');
      chatPage.classList.remove('hidden');
      connectingElement.classList.add('hidden');
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'CONNECTED_AGENT':
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      to = serverMessage.senderId;
      nameInterlocutor = serverMessage.senderName;
      sendMessageClientBeforeStartedDialogue();
      messageList = [];
      break;
    case 'END_DIALOGUE':
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      to = null;
      nameInterlocutor = null;
      break;
    case 'MESSAGE_CHAT':
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
      messageElement.classList.add('event-message');
      printMessage(messageElement, serverMessage);
      break;
    case 'DISCONNECTION_OF_THE_AGENT':
      message = {
        senderId: userId,
        text: 'The agent left the network, write a message to connect to the new agent or close the page, to exit the network',
        typeOfMessage: serverMessage.typeOfMessage
      };
      messageElement.classList.add('event-message');
      printMessage(messageElement, message);
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
messageForm.addEventListener('submit', sendMessage, true);
messageLeave.addEventListener('submit', leave, true);
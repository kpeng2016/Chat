'use strict';

var registerPage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageLeave = document.querySelector('#userLeave');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var checkRegister = document.querySelector('.checkRegister');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
var userName = null;
var userId = null;
var userRole = null;
var to = null;
var messageList = [];
var userMessage = null;
var subscription = null;
var message = null;
var nameInterlocutor = null;
var intervalID = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
    userName = document.querySelector('#name').value.trim();
    userRole = document.querySelector('#role').value.trim();
    if (userName && userRole) {
        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    subscription = stompClient.subscribe('/topic/0', onMessageReceived);
    stompClient.send("/app/chat.addUser",
        {},
        JSON.stringify({
            senderId: null, text: '/register ' + userRole + ' ' + userName, typeOfMessage: 'Register', to: null
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
        typeOfMessage: 'MessageChat',
        to: to
    };
    stompClient.send("/app/chat.leave", {}, JSON.stringify(userMessage));
    event.preventDefault();
}

function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        userMessage = {
            senderId: userId,
            text: messageInput.value,
            typeOfMessage: 'MessageChat',
            to: to,
            nameTo: nameInterlocutor
        };
        if (!to) {
            messageList.push(userMessage.text);
            stompClient.send("/app/chat.sendMessageUser", {}, JSON.stringify(userMessage));
            stompClient.send("/app/chat.search", {}, JSON.stringify(userMessage));
            if (userRole === 'client' && !intervalID) {
                CallSearchFreeAgent();
            }
        } else {
            stompClient.send("/app/chat.sendMessageUser", {}, JSON.stringify(userMessage));
            stompClient.send("/app/chat.sendMessageInterlocutor", {}, JSON.stringify(userMessage));
        }
        messageInput.value = '';
    }
    event.preventDefault();
}

function CallSearchFreeAgent() {
    var messageForServer = {
        senderId: userId,
        text: 'CallSearchFreeAgent',
        typeOfMessage: 'CallSearchFreeAgent',
        to: null,
        nameTo: null
    };
    intervalID = setInterval(function () {
        stompClient.send("/app/chat.search", {}, JSON.stringify(messageForServer));
    }, 10000);
}

function printMessageClientBeforeStartedDialogue() {
    for (var i = 0; i < messageList.length; i++) {
        userMessage = {
            senderId: userId,
            text: messageList[i],
            typeOfMessage: 'MessageChat',
            to: to,
            nameTo: nameInterlocutor
        };
        stompClient.send("/app/chat.sendMessageInterlocutor", {}, JSON.stringify(userMessage));
    }
}

function onMessageReceived(payload) {
    var serverMessage = JSON.parse(payload.body);
    var messageElement = document.createElement('li');
    switch (serverMessage.typeOfMessage) {
        case 'NotValidRegistrationData':
            subscription.unsubscribe('/topic/0', onMessageReceived);
            checkRegister.textContent = 'Not valid registration data, please refresh this page to try again!';
            checkRegister.style.color = 'red';
            break;
        case 'CorrectRegistration':
            registerPage.classList.add('hidden');
            chatPage.classList.remove('hidden');
            connectingElement.classList.add('hidden');
            userId = serverMessage.senderId;
            subscription.unsubscribe('/topic/0', onMessageReceived);
            subscription = stompClient.subscribe('/topic/' + userId, onMessageReceived);
            messageElement.classList.add('event-message');
            printMessage(messageElement, serverMessage);
            break;
        case 'ConnectedAgent':
            clearInterval(intervalID);
            messageElement.classList.add('event-message');
            printMessage(messageElement, serverMessage);
            to = serverMessage.to;
            nameInterlocutor = serverMessage.nameTo;
            printMessageClientBeforeStartedDialogue();
            break;
        case 'ConnectedClient':
        case 'EndDialogue':
            messageElement.classList.add('event-message');
            printMessage(messageElement, serverMessage);
            to = serverMessage.to;
            nameInterlocutor = serverMessage.nameTo;
            break;
        case 'MessageChat':
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
        case 'YourMessages':
            messageElement.classList.add('chat-message');
            settingMessageUser(messageElement);
            printMessage(messageElement, serverMessage);
            break;
        case 'NoFreeAgent':
        case 'FirstMessageAgent':
        case 'AgentCantLeave':
            messageElement.classList.add('event-message');
            printMessage(messageElement, serverMessage);
            break;
        case 'LeaveClient':
        case 'DisconnectionOfTheClient':
            message = {
                senderId: serverMessage.userId,
                text: 'The client left the chat, wait until the new client connects or closes the page to exit the network',
                typeOfMessage: serverMessage.typeOfMessage,
                to: null,
                nameTo: null
            };
            messageElement.classList.add('event-message');
            printMessage(messageElement, message);
            break;
        case 'DisconnectionOfTheAgent':
            message = {
                senderId: serverMessage.userId,
                text: 'The agent left the network, write a message to connect to the new agent or close the page, to exit the network',
                typeOfMessage: serverMessage.typeOfMessage,
                to: null,
                nameTo: null
            };
            messageElement.classList.add('event-message');
            printMessage(messageElement, message);
            break;
        case 'CallSearchFreeAgent':
            break;
        default:
            alert('error');
    }
}

function settingMessageUser(messageElement) {
    var avatarElement = document.createElement('i');
    var avatarText = document.createTextNode(userName[0]);
    avatarElement.appendChild(avatarText);
    avatarElement.style['background-color'] = getAvatarColor(userId);
    messageElement.appendChild(avatarElement);
    var usernameElement = document.createElement('span');
    var usernameText = document.createTextNode(userName);
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

usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
messageLeave.addEventListener('submit', leave, true);
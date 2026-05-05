'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');
var usersList = document.querySelector('#users-list');      // new
var usersCount = document.querySelector('#users-count');    // new

var stompClient = null;
var username = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
    username = document.querySelector('#name').value.trim();
    if (username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    // Subscribe to public messages
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Subscribe to online users list updates
    stompClient.subscribe('/topic/users', onUsersUpdated);

    // Subscribe to personal history feed
    stompClient.subscribe('/topic/history/' + username, onHistoryReceived);

    // Tell server we joined
    stompClient.send("/app/chat.addUser", {}, JSON.stringify({sender: username, type: 'JOIN'}));

    connectingElement.classList.add('hidden');
}

function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}

function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        var chatMessage = {sender: username, content: messageInput.value, type: 'CHAT'};
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

// Loads history when user first joins
function onHistoryReceived(payload) {
    var history = JSON.parse(payload.body);
    history.forEach(function(message) {
        renderMessage(message);
    });
}

// Updates the online users sidebar
function onUsersUpdated(payload) {
    var users = JSON.parse(payload.body);

    if (usersCount) usersCount.textContent = users.length;

    if (usersList) {
        usersList.innerHTML = '';
        users.forEach(function(user) {
            var li = document.createElement('li');

            var dot = document.createElement('span');
            dot.classList.add('online-dot');

            var nameSpan = document.createElement('span');
            var isYou = user === username;
            nameSpan.textContent = user + (isYou ? ' (you)' : '');
            if (isYou) nameSpan.style.fontWeight = 'bold';

            li.appendChild(dot);
            li.appendChild(nameSpan);
            usersList.appendChild(li);
        });
    }
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    renderMessage(message);
}

// Separated into its own function so history can reuse it
function renderMessage(message) {
    var messageElement = document.createElement('li');
    var isOwnMessage = message.sender === username;
    var youTag = isOwnMessage ? ' (you)' : '';

    if (message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + youTag + ' joined!';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + youTag + ' left!';
    } else {
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        avatarElement.appendChild(document.createTextNode(message.sender[0]));
        avatarElement.style['background-color'] = getAvatarColor(message.sender);
        messageElement.appendChild(avatarElement);

        var usernameElement = document.createElement('span');
        usernameElement.appendChild(document.createTextNode(message.sender + youTag));
        messageElement.appendChild(usernameElement);
    }

    var textElement = document.createElement('p');
    textElement.appendChild(document.createTextNode(message.content));
    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }
    return colors[Math.abs(hash % colors.length)];
}

usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
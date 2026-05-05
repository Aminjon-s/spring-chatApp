package com.pavel.chatapp.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;

    // Stores online usernames
    private final Set<String> onlineUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Stores chat history (max 100 messages)
    private final List<ChatMessage> chatHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY = 100;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        // Save to history
        addToHistory(chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();

        // Save username in WebSocket session
        headerAccessor.getSessionAttributes().put("username", username);

        // Add to online users
        onlineUsers.add(username);

        // Save JOIN message to history
        addToHistory(chatMessage);

        // Broadcast JOIN message to everyone
        messagingTemplate.convertAndSend("/topic/public", chatMessage);

        // Send chat history only to the new user
        messagingTemplate.convertAndSend("/topic/history/" + username,
                new ArrayList<>(chatHistory.subList(0, Math.max(0, chatHistory.size() - 1))));

        // Broadcast updated online users list to everyone
        messagingTemplate.convertAndSend("/topic/users", new ArrayList<>(onlineUsers));
    }

    // Called from WebSocketEventListener when user disconnects
    public void removeUser(String username) {
        onlineUsers.remove(username);
        messagingTemplate.convertAndSend("/topic/users", new ArrayList<>(onlineUsers));
    }

    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    private void addToHistory(ChatMessage message) {
        if (chatHistory.size() >= MAX_HISTORY) {
            chatHistory.remove(0);
        }
        chatHistory.add(message);
    }
}

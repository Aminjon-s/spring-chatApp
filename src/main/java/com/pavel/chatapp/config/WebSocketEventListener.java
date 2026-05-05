package com.pavel.chatapp.config;

import com.pavel.chatapp.chat.ChatController;
import com.pavel.chatapp.chat.ChatMessage;
import com.pavel.chatapp.chat.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener implements ApplicationListener<SessionDisconnectEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatController chatController;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, ChatController chatController) {
        this.messagingTemplate = messagingTemplate;
        this.chatController = chatController;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        var headerAccessor = org.springframework.messaging.simp.stomp.StompHeaderAccessor.wrap(event.getMessage());
        var sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String username = (String) sessionAttributes.get("username");
            if (username != null) {
                // Broadcast LEAVE message
                var leaveMessage = new ChatMessage();
                leaveMessage.setSender(username);
                leaveMessage.setType(MessageType.LEAVE);
                leaveMessage.setContent(username + " left!");
                messagingTemplate.convertAndSend("/topic/public", leaveMessage);

                // Remove from online users and broadcast updated list
                chatController.removeUser(username);
            }
        }
    }
}

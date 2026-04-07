package com.campus.resourcesharing.service;

import com.campus.resourcesharing.model.Message;
import com.campus.resourcesharing.model.Request;
import com.campus.resourcesharing.model.User;
import com.campus.resourcesharing.repository.MessageRepository;
import com.campus.resourcesharing.repository.RequestRepository;
import com.campus.resourcesharing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public Message sendMessage(Long requestId, Long senderId, String content) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        // Verify request is approved
        if (request.getStatus() != Request.RequestStatus.APPROVED) {
            throw new RuntimeException("Chat is only available for approved requests");
        }
        
        // Verify sender is either owner or requester
        Long ownerId = request.getOwner().getId();
        Long requesterId = request.getRequester().getId();
        
        if (!senderId.equals(ownerId) && !senderId.equals(requesterId)) {
            throw new RuntimeException("Unauthorized: You are not part of this conversation");
        }
        
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Message message = new Message();
        message.setRequest(request);
        message.setSender(sender);
        message.setContent(content);
        
        Message savedMessage = messageRepository.save(message);
        
        // Force initialization of lazy-loaded sender relationship
        savedMessage.getSender().getId();
        savedMessage.getSender().getUsername();
        savedMessage.getSender().getFullName();
        
        return savedMessage;
    }
    
    @Transactional
    public List<Message> getMessages(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        // Verify user is either owner or requester
        Long ownerId = request.getOwner().getId();
        Long requesterId = request.getRequester().getId();
        
        if (!userId.equals(ownerId) && !userId.equals(requesterId)) {
            throw new RuntimeException("Unauthorized: You are not part of this conversation");
        }
        
        // Mark messages as read for the other user
        markMessagesAsRead(requestId, userId);
        
        List<Message> messages = messageRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
        
        // Force initialization of lazy-loaded sender relationships
        for (Message msg : messages) {
            msg.getSender().getId();
            msg.getSender().getUsername();
            msg.getSender().getFullName();
        }
        
        return messages;
    }
    
    public void markMessagesAsRead(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        List<Message> unreadMessages = messageRepository.findByRequestIdAndIsReadFalse(requestId);
        for (Message message : unreadMessages) {
            // Mark as read if message is not from current user
            if (!message.getSender().getId().equals(userId)) {
                message.setIsRead(true);
                messageRepository.save(message);
            }
        }
    }
    
    public Long getUnreadMessageCount(Long requestId, Long userId) {
        if (!requestRepository.existsById(requestId)) {
            throw new RuntimeException("Request not found");
        }
        return messageRepository.countByRequestIdAndIsReadFalseAndSenderIdNot(requestId, userId);
    }
    
    public Map<Long, Long> getAllUnreadMessageCounts(Long userId) {
        Map<Long, Long> allCounts = new HashMap<>();
        
        // Get all requests where user is either owner or requester
        List<Request> userRequests = requestRepository.findByOwnerIdOrRequesterId(userId);
        
        for (Request request : userRequests) {
            Long unreadCount = getUnreadMessageCount(request.getId(), userId);
            if (unreadCount > 0) {
                allCounts.put(request.getId(), unreadCount);
            }
        }
        
        return allCounts;
    }
}

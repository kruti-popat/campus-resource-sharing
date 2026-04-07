package com.campus.resourcesharing.controller;

import com.campus.resourcesharing.dto.ApiResponse;
import com.campus.resourcesharing.model.Message;
import com.campus.resourcesharing.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @PostMapping("/send")
    public ResponseEntity<ApiResponse> sendMessage(@RequestParam Long requestId,
                                                   @RequestParam Long senderId,
                                                   @RequestParam String content) {
        try {
            Message message = chatService.sendMessage(requestId, senderId, content);
            return ResponseEntity.ok(new ApiResponse(true, "Message sent successfully", 
                    convertToMap(message)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse> getMessages(@RequestParam Long requestId,
                                                   @RequestParam Long userId) {
        try {
            List<Message> messages = chatService.getMessages(requestId, userId);
            List<Map<String, Object>> messageList = messages.stream()
                    .map(this::convertToMap)
                    .toList();
            
            return ResponseEntity.ok(new ApiResponse(true, "Messages retrieved successfully", messageList));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse> getUnreadCount(@RequestParam Long requestId,
                                                      @RequestParam Long userId) {
        try {
            Long count = chatService.getUnreadMessageCount(requestId, userId);
            Map<String, Object> data = new HashMap<>();
            data.put("unreadCount", count);
            return ResponseEntity.ok(new ApiResponse(true, "Unread count retrieved", data));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/all-unread-counts")
    public ResponseEntity<ApiResponse> getAllUnreadCounts(@RequestParam Long userId) {
        try {
            Map<Long, Long> allCounts = chatService.getAllUnreadMessageCounts(userId);
            return ResponseEntity.ok(new ApiResponse(true, "All unread counts retrieved", allCounts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    private Map<String, Object> convertToMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("content", message.getContent());
        map.put("createdAt", message.getCreatedAt());
        map.put("isRead", message.getIsRead());
        
        Map<String, Object> senderMap = new HashMap<>();
        senderMap.put("id", message.getSender().getId());
        senderMap.put("username", message.getSender().getUsername());
        senderMap.put("fullName", message.getSender().getFullName());
        map.put("sender", senderMap);
        
        return map;
    }
}

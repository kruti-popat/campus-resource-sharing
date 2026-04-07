package com.campus.resourcesharing.controller;

import com.campus.resourcesharing.dto.ApiResponse;
import com.campus.resourcesharing.model.Request;
import com.campus.resourcesharing.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class RequestController {
    
    @Autowired
    private RequestService requestService;
    
    @PostMapping
    public ResponseEntity<ApiResponse> createRequest(@RequestParam Long resourceId,
                                                    @RequestParam Long requesterId,
                                                    @RequestParam(required = false) String message) {
        try {
            Request request = requestService.createRequest(resourceId, requesterId, 
                    message != null ? message : "");
            return ResponseEntity.ok(new ApiResponse(true, "Request created successfully", 
                    convertToMap(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse> approveRequest(@PathVariable Long id, 
                                                     @RequestParam Long ownerId) {
        try {
            Request request = requestService.approveRequest(id, ownerId);
            return ResponseEntity.ok(new ApiResponse(true, "Request approved successfully", 
                    convertToMap(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse> rejectRequest(@PathVariable Long id, 
                                                     @RequestParam Long ownerId) {
        try {
            Request request = requestService.rejectRequest(id, ownerId);
            return ResponseEntity.ok(new ApiResponse(true, "Request rejected successfully", 
                    convertToMap(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelRequest(@PathVariable Long id, 
                                                     @RequestParam Long requesterId) {
        try {
            Request request = requestService.cancelRequest(id, requesterId);
            return ResponseEntity.ok(new ApiResponse(true, "Request cancelled successfully", 
                    convertToMap(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<ApiResponse> getRequestsByRequester(@PathVariable Long requesterId) {
        try {
            List<Request> requests = requestService.getRequestsByRequester(requesterId);
            List<Map<String, Object>> requestList = requests.stream()
                    .map(this::convertToMap)
                    .toList();
            
            return ResponseEntity.ok(new ApiResponse(true, "Requests retrieved successfully", requestList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse> getRequestsByOwner(@PathVariable Long ownerId) {
        try {
            List<Request> requests = requestService.getRequestsByOwner(ownerId);
            List<Map<String, Object>> requestList = requests.stream()
                    .map(this::convertToMap)
                    .toList();
            
            return ResponseEntity.ok(new ApiResponse(true, "Requests retrieved successfully", requestList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRequestById(@PathVariable Long id) {
        try {
            Request request = requestService.getRequestById(id);
            return ResponseEntity.ok(new ApiResponse(true, "Request found", convertToMap(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/verify-handover")
    public ResponseEntity<ApiResponse> verifyHandover(@PathVariable Long id, 
                                                      @RequestParam Long ownerId) {
        try {
            Request request = requestService.verifyHandover(id, ownerId);
            return ResponseEntity.ok(new ApiResponse(true, "Handover verified successfully", 
                    convertToMap(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/verify-receipt")
    public ResponseEntity<ApiResponse> verifyReceipt(@PathVariable Long id, 
                                                      @RequestParam Long requesterId) {
        try {
            Request request = requestService.verifyReceipt(id, requesterId);
            return ResponseEntity.ok(new ApiResponse(true, "Receipt verified successfully", 
                    convertToMap(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    private Map<String, Object> convertToMap(Request request) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("status", request.getStatus().toString());
        map.put("message", request.getMessage());
        map.put("createdAt", request.getCreatedAt());
        map.put("updatedAt", request.getUpdatedAt());
        
        Map<String, Object> resourceMap = new HashMap<>();
        resourceMap.put("id", request.getResource().getId());
        resourceMap.put("title", request.getResource().getTitle());
        resourceMap.put("category", request.getResource().getCategory());
        map.put("resource", resourceMap);
        
        Map<String, Object> requesterMap = new HashMap<>();
        requesterMap.put("id", request.getRequester().getId());
        requesterMap.put("username", request.getRequester().getUsername());
        requesterMap.put("fullName", request.getRequester().getFullName());
        map.put("requester", requesterMap);
        
        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put("id", request.getOwner().getId());
        ownerMap.put("username", request.getOwner().getUsername());
        ownerMap.put("fullName", request.getOwner().getFullName());
        map.put("owner", ownerMap);
        
        map.put("ownerVerified", request.getOwnerVerified());
        map.put("requesterVerified", request.getRequesterVerified());
        map.put("isFullyVerified", request.isFullyVerified());
        
        return map;
    }
}

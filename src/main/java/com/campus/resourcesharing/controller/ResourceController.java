package com.campus.resourcesharing.controller;

import com.campus.resourcesharing.dto.ApiResponse;
import com.campus.resourcesharing.model.Resource;
import com.campus.resourcesharing.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class ResourceController {
    
    @Autowired
    private ResourceService resourceService;
    
    @PostMapping
    public ResponseEntity<ApiResponse> addResource(@Valid @RequestBody Resource resource, 
                                                   @RequestParam Long ownerId) {
        try {
            Resource savedResource = resourceService.addResource(resource, ownerId);
            return ResponseEntity.ok(new ApiResponse(true, "Resource added successfully", 
                    convertToMap(savedResource)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse> getAllResources(@RequestParam(required = false) String category) {
        try {
            List<Resource> resources;
            if (category != null && !category.isEmpty()) {
                resources = resourceService.getResourcesByCategory(category);
            } else {
                resources = resourceService.getAllAvailableResources();
            }
            
            List<Map<String, Object>> resourceList = resources.stream()
                    .map(this::convertToMap)
                    .toList();
            
            return ResponseEntity.ok(new ApiResponse(true, "Resources retrieved successfully", resourceList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse> getResourcesByOwner(@PathVariable Long ownerId) {
        try {
            List<Resource> resources = resourceService.getResourcesByOwner(ownerId);
            List<Map<String, Object>> resourceList = resources.stream()
                    .map(this::convertToMap)
                    .toList();
            
            return ResponseEntity.ok(new ApiResponse(true, "Resources retrieved successfully", resourceList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getResourceById(@PathVariable Long id) {
        try {
            Resource resource = resourceService.getResourceById(id);
            return ResponseEntity.ok(new ApiResponse(true, "Resource found", convertToMap(resource)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateResource(@PathVariable Long id, 
                                                      @RequestBody Resource resource) {
        try {
            Resource updatedResource = resourceService.updateResource(id, resource);
            return ResponseEntity.ok(new ApiResponse(true, "Resource updated successfully", 
                    convertToMap(updatedResource)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteResource(@PathVariable Long id) {
        try {
            resourceService.deleteResource(id);
            return ResponseEntity.ok(new ApiResponse(true, "Resource deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    private Map<String, Object> convertToMap(Resource resource) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", resource.getId());
        map.put("title", resource.getTitle());
        map.put("description", resource.getDescription());
        map.put("category", resource.getCategory());
        map.put("conditionStatus", resource.getConditionStatus());
        map.put("isAvailable", resource.getIsAvailable());
        map.put("transactionType", resource.getTransactionType());
        map.put("price", resource.getPrice());
        map.put("rentalDuration", resource.getRentalDuration());
        map.put("exchangeDescription", resource.getExchangeDescription());
        map.put("createdAt", resource.getCreatedAt());
        
        // Check if resource has pending approved request (not fully verified)
        boolean alreadyRequested = resourceService.hasPendingApprovedRequest(resource.getId());
        map.put("alreadyRequested", alreadyRequested);
        
        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put("id", resource.getOwner().getId());
        ownerMap.put("username", resource.getOwner().getUsername());
        ownerMap.put("fullName", resource.getOwner().getFullName());
        ownerMap.put("averageRating", resource.getOwner().getAverageRating());
        ownerMap.put("totalRatings", resource.getOwner().getTotalRatings());
        map.put("owner", ownerMap);
        
        return map;
    }
}

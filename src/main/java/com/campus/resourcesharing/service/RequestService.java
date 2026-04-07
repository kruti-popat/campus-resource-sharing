package com.campus.resourcesharing.service;

import com.campus.resourcesharing.model.Request;
import com.campus.resourcesharing.model.Resource;
import com.campus.resourcesharing.model.User;
import com.campus.resourcesharing.repository.RequestRepository;
import com.campus.resourcesharing.repository.ResourceRepository;
import com.campus.resourcesharing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RequestService {
    
    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private ResourceRepository resourceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public Request createRequest(Long resourceId, Long requesterId, String message) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        
        // Make sure owner is fully initialized (avoid lazy proxy outside transaction)
        User owner = userRepository.findById(resource.getOwner().getId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));
        
        // Check if requester is the owner
        if (requesterId.equals(owner.getId())) {
            throw new RuntimeException("Cannot request your own resource");
        }
        
        // Check if resource is available
        if (!resource.getIsAvailable()) {
            throw new RuntimeException("Resource is not available");
        }
        
        // Check if there's already a pending or approved request (not fully verified)
        List<Request> existingRequests = requestRepository.findByResourceId(resourceId);
        for (Request req : existingRequests) {
            // Check for pending request from same user
            if (req.getRequester().getId().equals(requesterId) && 
                req.getStatus() == Request.RequestStatus.PENDING) {
                throw new RuntimeException("You already have a pending request for this resource");
            }
            
            // Check if there's an approved request that's not fully verified
            if (req.getStatus() == Request.RequestStatus.APPROVED && !req.isFullyVerified()) {
                throw new RuntimeException("This resource is already requested and awaiting verification");
            }
        }
        
        Request request = new Request();
        request.setResource(resource);
        request.setRequester(requester);
        request.setOwner(owner);
        request.setMessage(message);
        request.setStatus(Request.RequestStatus.PENDING);
        
        Request saved = requestRepository.save(request);
        return requestRepository.findDetailedById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }
    
    @Transactional
    public Request approveRequest(Long requestId, Long ownerId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        // Verify owner
        if (!request.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: You are not the owner of this resource");
        }
        
        if (request.getStatus() != Request.RequestStatus.PENDING) {
            throw new RuntimeException("Request is not in pending status");
        }
        
        request.setStatus(Request.RequestStatus.APPROVED);
        
        // Don't mark resource as unavailable yet - wait for verification
        // Resource will be marked unavailable only after both users verify
        
        Request savedRequest = requestRepository.save(request);
        
        // Force initialization of lazy-loaded relationships
        savedRequest.getResource().getId();
        savedRequest.getResource().getTitle();
        savedRequest.getRequester().getId();
        savedRequest.getRequester().getFullName();
        savedRequest.getOwner().getId();
        savedRequest.getOwner().getFullName();
        
        return savedRequest;
    }
    
    @Transactional
    public Request rejectRequest(Long requestId, Long ownerId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        // Verify owner
        if (!request.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: You are not the owner of this resource");
        }
        
        if (request.getStatus() != Request.RequestStatus.PENDING) {
            throw new RuntimeException("Request is not in pending status");
        }
        
        request.setStatus(Request.RequestStatus.REJECTED);
        Request savedRequest = requestRepository.save(request);
        
        // Force initialization of lazy-loaded relationships
        savedRequest.getResource().getId();
        savedRequest.getResource().getTitle();
        savedRequest.getRequester().getId();
        savedRequest.getRequester().getFullName();
        savedRequest.getOwner().getId();
        savedRequest.getOwner().getFullName();
        
        return savedRequest;
    }
    
    @Transactional
    public Request cancelRequest(Long requestId, Long requesterId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        // Verify requester
        if (!request.getRequester().getId().equals(requesterId)) {
            throw new RuntimeException("Unauthorized: You are not the requester");
        }
        
        if (request.getStatus() != Request.RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be cancelled");
        }
        
        request.setStatus(Request.RequestStatus.CANCELLED);
        Request savedRequest = requestRepository.save(request);
        
        // Force initialization of lazy-loaded relationships
        savedRequest.getResource().getId();
        savedRequest.getResource().getTitle();
        savedRequest.getRequester().getId();
        savedRequest.getRequester().getFullName();
        savedRequest.getOwner().getId();
        savedRequest.getOwner().getFullName();
        
        return savedRequest;
    }
    
    public List<Request> getRequestsByRequester(Long requesterId) {
        return requestRepository.findByRequesterId(requesterId);
    }
    
    public List<Request> getRequestsByOwner(Long ownerId) {
        return requestRepository.findByOwnerId(ownerId);
    }
    
    @Transactional(readOnly = true)
    public Request getRequestById(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        // Force initialization of lazy-loaded relationships within transaction
        request.getResource().getId(); // Initialize resource
        request.getRequester().getId(); // Initialize requester
        request.getOwner().getId(); // Initialize owner
        return request;
    }
    
    @Transactional
    public Request verifyHandover(Long requestId, Long ownerId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!request.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: Only the owner can verify handover");
        }
        
        if (request.getStatus() != Request.RequestStatus.APPROVED) {
            throw new RuntimeException("Request must be approved before verification");
        }
        
        request.setOwnerVerified(true);
        Request updatedRequest = requestRepository.save(request);
        
        // If both verified, mark resource as unavailable
        if (updatedRequest.isFullyVerified()) {
            Resource resource = updatedRequest.getResource();
            resource.setIsAvailable(false);
            resourceRepository.save(resource);
        }
        
        // Force initialization of lazy-loaded relationships
        updatedRequest.getResource().getId();
        updatedRequest.getResource().getTitle();
        updatedRequest.getRequester().getId();
        updatedRequest.getRequester().getFullName();
        updatedRequest.getOwner().getId();
        updatedRequest.getOwner().getFullName();
        
        return updatedRequest;
    }
    
    @Transactional
    public Request verifyReceipt(Long requestId, Long requesterId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!request.getRequester().getId().equals(requesterId)) {
            throw new RuntimeException("Unauthorized: Only the requester can verify receipt");
        }
        
        if (request.getStatus() != Request.RequestStatus.APPROVED) {
            throw new RuntimeException("Request must be approved before verification");
        }
        
        request.setRequesterVerified(true);
        Request updatedRequest = requestRepository.save(request);
        
        // If both verified, mark resource as unavailable
        if (updatedRequest.isFullyVerified()) {
            Resource resource = updatedRequest.getResource();
            resource.setIsAvailable(false);
            resourceRepository.save(resource);
        }
        
        // Force initialization of lazy-loaded relationships
        updatedRequest.getResource().getId();
        updatedRequest.getResource().getTitle();
        updatedRequest.getRequester().getId();
        updatedRequest.getRequester().getFullName();
        updatedRequest.getOwner().getId();
        updatedRequest.getOwner().getFullName();
        
        return updatedRequest;
    }
    
    public List<Request> getApprovedRequestsForResource(Long resourceId) {
        return requestRepository.findByResourceId(resourceId).stream()
                .filter(req -> req.getStatus() == Request.RequestStatus.APPROVED)
                .filter(req -> !req.isFullyVerified())
                .toList();
    }
}

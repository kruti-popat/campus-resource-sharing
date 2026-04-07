package com.campus.resourcesharing.service;

import com.campus.resourcesharing.model.Resource;
import com.campus.resourcesharing.model.Request;
import com.campus.resourcesharing.model.User;
import com.campus.resourcesharing.repository.ResourceRepository;
import com.campus.resourcesharing.repository.RequestRepository;
import com.campus.resourcesharing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResourceService {
    
    @Autowired
    private ResourceRepository resourceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RequestRepository requestRepository;
    
    public Resource addResource(Resource resource, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        resource.setOwner(owner);
        resource.setIsAvailable(true);
        return resourceRepository.save(resource);
    }
    
    public List<Resource> getAllAvailableResources() {
        return resourceRepository.findByIsAvailableTrue();
    }
    
    public boolean hasPendingApprovedRequest(Long resourceId) {
        List<Request> requests = requestRepository.findByResourceId(resourceId);
        return requests.stream()
                .anyMatch(req -> req.getStatus() == Request.RequestStatus.APPROVED && !req.isFullyVerified());
    }
    
    public List<Resource> getResourcesByOwner(Long ownerId) {
        return resourceRepository.findByOwnerId(ownerId);
    }
    
    public List<Resource> getResourcesByCategory(String category) {
        return resourceRepository.findByCategoryAndIsAvailableTrue(category);
    }
    
    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
    }
    
    public Resource updateResource(Long id, Resource updatedResource) {
        Resource resource = getResourceById(id);
        resource.setTitle(updatedResource.getTitle());
        resource.setDescription(updatedResource.getDescription());
        resource.setCategory(updatedResource.getCategory());
        resource.setConditionStatus(updatedResource.getConditionStatus());
        resource.setIsAvailable(updatedResource.getIsAvailable());
        resource.setTransactionType(updatedResource.getTransactionType());
        resource.setPrice(updatedResource.getPrice());
        resource.setRentalDuration(updatedResource.getRentalDuration());
        resource.setExchangeDescription(updatedResource.getExchangeDescription());
        return resourceRepository.save(resource);
    }
    
    public void deleteResource(Long id) {
        resourceRepository.deleteById(id);
    }
}

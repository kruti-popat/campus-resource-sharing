package com.campus.resourcesharing.repository;

import com.campus.resourcesharing.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    @Query("select r from Resource r join fetch r.owner where r.isAvailable = true")
    List<Resource> findByIsAvailableTrue();

    @Query("select r from Resource r join fetch r.owner where r.owner.id = :ownerId")
    List<Resource> findByOwnerId(Long ownerId);

    List<Resource> findByCategory(String category);

    @Query("select r from Resource r join fetch r.owner where r.category = :category and r.isAvailable = true")
    List<Resource> findByCategoryAndIsAvailableTrue(String category);
}

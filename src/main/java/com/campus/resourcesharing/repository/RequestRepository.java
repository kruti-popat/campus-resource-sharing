package com.campus.resourcesharing.repository;

import com.campus.resourcesharing.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    @Query("""
            select r from Request r
            join fetch r.resource res
            join fetch r.requester req
            join fetch r.owner own
            where r.id = :id
            """)
    Optional<Request> findDetailedById(Long id);

    @Query("""
            select r from Request r
            join fetch r.resource res
            join fetch r.requester req
            join fetch r.owner own
            where req.id = :requesterId
            """)
    List<Request> findByRequesterId(Long requesterId);

    @Query("""
            select r from Request r
            join fetch r.resource res
            join fetch r.requester req
            join fetch r.owner own
            where own.id = :ownerId
            """)
    List<Request> findByOwnerId(Long ownerId);

    @Query("""
            select r from Request r
            join fetch r.resource res
            join fetch r.requester req
            join fetch r.owner own
            where res.id = :resourceId
            """)
    List<Request> findByResourceId(Long resourceId);
    List<Request> findByRequesterIdAndStatus(Long requesterId, Request.RequestStatus status);
    List<Request> findByOwnerIdAndStatus(Long ownerId, Request.RequestStatus status);
    
    @Query("""
            select r from Request r
            where r.requester.id = :userId or r.owner.id = :userId
            """)
    List<Request> findByOwnerIdOrRequesterId(Long userId);
}

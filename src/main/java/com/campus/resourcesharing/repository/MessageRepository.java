package com.campus.resourcesharing.repository;

import com.campus.resourcesharing.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRequestIdOrderByCreatedAtAsc(Long requestId);
    List<Message> findByRequestIdAndIsReadFalse(Long requestId);
    Long countByRequestIdAndIsReadFalseAndSenderIdNot(Long requestId, Long senderId);
}

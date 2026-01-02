package com.healthdata.approval.repository;

import com.healthdata.approval.domain.entity.ApprovalHistory;
import com.healthdata.approval.domain.entity.ApprovalHistory.HistoryAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    List<ApprovalHistory> findByApprovalRequestIdOrderByCreatedAtAsc(UUID approvalRequestId);

    Page<ApprovalHistory> findByApprovalRequestId(UUID approvalRequestId, Pageable pageable);

    List<ApprovalHistory> findByApprovalRequestIdAndAction(UUID approvalRequestId, HistoryAction action);

    long countByApprovalRequestId(UUID approvalRequestId);
}

package com.docprocess.repository;

import com.docprocess.model.DocumentGenerateQueueData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface DocumentDataRepository extends JpaRepository<DocumentGenerateQueueData, Integer> {

    List<DocumentGenerateQueueData> findTop40ByCreateddateLessThanAndRenderedDateAndPriorityAndFailedAttemptLessThanEqualOrderByCreateddateAsc(Timestamp createdDate, Timestamp renderedDate, Double priority, Double maxFailedAttempt);

    List<DocumentGenerateQueueData> findTop10ByCreateddateLessThanAndRenderedDateAndPriorityNotNullAndFailedAttemptLessThanEqualOrderByPriorityAsc(Timestamp createdDate, Timestamp renderedDate, Double maxFailedAttempt);


    DocumentGenerateQueueData findBySfid(String sfid);

    List<DocumentGenerateQueueData> findAllByReferenceNumberAndAttachmentGroupCodeAndFlagSendEmailAndFlagEmailAttachmentReady(String referenceNumber, String attachmentGroupCode, Boolean falgSendEmail, Boolean flagReadyToSendEmail);

    List<DocumentGenerateQueueData> findAllByReferenceNumber(String referenceNum);


}

package com.docprocess.repository;

import com.docprocess.model.DocumentProcessPending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DocumentProcessPendingRepository extends JpaRepository<DocumentProcessPending, Integer> {

    List<DocumentProcessPending> findTop100ByReferenceNumberNotNullOrderByCreateddateAscReferenceNumberAsc();
}

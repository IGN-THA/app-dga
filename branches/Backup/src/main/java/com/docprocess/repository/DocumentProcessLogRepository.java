package com.docprocess.repository;

import com.docprocess.model.DocumentProcessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DocumentProcessLogRepository extends JpaRepository<DocumentProcessLog, Integer> {


}

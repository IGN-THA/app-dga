package com.docprocess.repository;

import com.docprocess.model.DocumentTypeData;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DocumentTypeDataRepository extends JpaRepository<DocumentTypeData, Integer> {

    @Cacheable(value="DocumentTypeData", key="#documentType")
    DocumentTypeData findByDocumentType(String documentType);
}

package com.docprocess.model;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "document_type_data", schema = "fmsapp")
@EntityListeners(AuditingEntityListener.class)

public class DocumentTypeData  implements Serializable{

    public DocumentTypeData() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Document_Type")
    private String documentType;

    @Column(name = "Signature_Card_Name")
    private String signattureCardName;

    @Column(name ="Created_Date")
    private Timestamp createdDate;

    @Column(name ="Template_Name")
    private String templateName;

    @Column(name ="Query_Name")
    private String queryName;

    @Column(name = "is_for_validation")
    private Boolean isForValidation;

    @Column(name ="sign_owner")
    private String signOwner;

    @Column(name ="upload_tag_name")
    private String uploadTagName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getSignattureCardName() {
        return signattureCardName;
    }

    public void setSignattureCardName(String signattureCardName) {
        this.signattureCardName = signattureCardName;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public Boolean getForValidation() {
        return isForValidation;
    }

    public void setForValidation(Boolean forValidation) {
        isForValidation = forValidation;
    }

    public String getSignOwner() {
        return signOwner;
    }

    public void setSignOwner(String signOwner) {
        this.signOwner = signOwner;
    }

    public String getUploadTagName() {
        return uploadTagName;
    }

    public void setUploadTagName(String uploadTagName) {
        this.uploadTagName = uploadTagName;
    }
}

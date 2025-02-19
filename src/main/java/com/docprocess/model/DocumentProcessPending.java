package com.docprocess.model;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "document_process_pending", schema = "fmsapp")
@EntityListeners(AuditingEntityListener.class)

public class DocumentProcessPending implements Serializable{

    public DocumentProcessPending() {
    }

    @Id
    @Column(name = "docid")
    private Integer docId;

    @Column(name = "sfid")
    private String sfid;

    @Column(name = "logid")
    private Integer logId;


    @Column(name = "flag_notify_email")
    private Boolean flagNotifyEmail;

    @Column(name = "upload_tag_update")
    private Boolean uploadTagUpate;

    @Column(name = "parent_id__c")
    private String referenceNumber;

    @Column(name = "createddate")
    private Timestamp createddate;


    public Integer getDocId() {
        return docId;
    }

    public void setDocId(Integer docId) {
        this.docId = docId;
    }

    public String getSfid() {
        return sfid;
    }

    public void setSfid(String sfid) {
        this.sfid = sfid;
    }

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public Boolean getFlagNotifyEmail() {
        return flagNotifyEmail;
    }

    public void setFlagNotifyEmail(Boolean flagNotifyEmail) {
        this.flagNotifyEmail = flagNotifyEmail;
    }

    public Boolean getUploadTagUpate() {
        return uploadTagUpate;
    }

    public void setUploadTagUpate(Boolean uploadTagUpate) {
        this.uploadTagUpate = uploadTagUpate;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Timestamp getCreateddate() {
        return createddate;
    }

    public void setCreateddate(Timestamp createddate) {
        this.createddate = createddate;
    }
}

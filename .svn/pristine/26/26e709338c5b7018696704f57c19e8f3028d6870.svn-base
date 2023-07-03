package com.docprocess.model;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import software.amazon.ion.Decimal;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "document_data", schema = "fmsapp")
@EntityListeners(AuditingEntityListener.class)
public class DocumentGenerateQueueData implements Serializable{

    public DocumentGenerateQueueData() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "failed_attempt__c")
    private Double failedAttempt;

    @Column(name = "priority__c")
    private Double priority;

     @Column(name = "sfid")
    private String sfid;

    @Column(name = "document_type__c")
    private String documentType;

    @Column(name = "document_name__c")
    private String documentName;

    @Column(name = "name")
    private String name;

    @Column(name = "parent_id__c")
    private String referenceNumber;

    @Column(name = "flag_document_signed__c")
    private Boolean flagDocumentSigned;

    @Column(name = "document_signed_date__c")
    private Timestamp documentSignedDate;

    @Column(name = "createddate")
    private Timestamp createddate;

    @Column(name = "rendered_date__c")
    private Timestamp renderedDate;

    @Column(name = "doc_printing_date__c")
    private Timestamp docPrintingDate;

    @Column(name = "cloud_upload_date__c")
    private Timestamp cloudUploadDate;

    @Column(name = "flag_require_sign__c")
    private Boolean flagRequireSign;

    @Column(name = "flag_printing_required__c")
    private Boolean flagPrintingRequired;

    @Column(name = "flag_password_protect__c")
    private Boolean flagPasswordProtect;

    @Column(name = "pdf_password__c")
    private String pdfPassword;

    @Column(name = "pdf_generated_date__c")
    private Timestamp pdfGeneratedDate;

    @Column(name = "error_message__c")
    private String errorMessage;

    @Column(name = "attachment_group_code__c")
    private String attachmentGroupCode;

    @Column(name = "flag_email_attachment_ready__c")
    private Boolean flagEmailAttachmentReady;

    @Column(name = "flag_send_email__c")
    private Boolean flagSendEmail;

    @Column(name = "record_id__c")
    private String queryRecordId;

    public Boolean getFlagSendEmail() {
        return flagSendEmail;
    }

    public void setFlagSendEmail(Boolean flagSendEmail) {
        this.flagSendEmail = flagSendEmail;
    }

    public String getAttachmentGroupCode() {
        return attachmentGroupCode;
    }

    public void setAttachmentGroupCode(String attachmentGroupCode) {
        this.attachmentGroupCode = attachmentGroupCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

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

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Boolean getFlagDocumentSigned() {
        return flagDocumentSigned;
    }

    public void setFlagDocumentSigned(Boolean flagDocumentSigned) {
        this.flagDocumentSigned = flagDocumentSigned;
    }

    public Timestamp getDocumentSignedDate() {
        return documentSignedDate;
    }

    public void setDocumentSignedDate(Timestamp documentSignedDate) {
        this.documentSignedDate = documentSignedDate;
    }

    public Timestamp getRenderedDate() {
        return renderedDate;
    }

    public void setRenderedDate(Timestamp renderedDate) {
        this.renderedDate = renderedDate;
    }

    public Boolean getFlagRequireSign() {
        return flagRequireSign;
    }

    public void setFlagRequireSign(Boolean flagRequireSign) {
        this.flagRequireSign = flagRequireSign;
    }

    public Boolean getFlagPrintingRequired() {
        return flagPrintingRequired;
    }

    public void setFlagPrintingRequired(Boolean flagPrintingRequired) {
        this.flagPrintingRequired = flagPrintingRequired;
    }

    public String getSfid() {
        return sfid;
    }

    public void setSfid(String sfid) {
        this.sfid = sfid;
    }

    public Timestamp getPdfGeneratedDate() {
        return pdfGeneratedDate;
    }

    public void setPdfGeneratedDate(Timestamp pdfGeneratedDate) {
        this.pdfGeneratedDate = pdfGeneratedDate;
    }

    public Timestamp getDocPrintingDate() {
        return docPrintingDate;
    }

    public void setDocPrintingDate(Timestamp docPrintingDate) {
        this.docPrintingDate = docPrintingDate;
    }

    public Timestamp getCloudUploadDate() {
        return cloudUploadDate;
    }

    public void setCloudUploadDate(Timestamp cloudUploadDate) {
        this.cloudUploadDate = cloudUploadDate;
    }

    public Double getFailedAttempt() {
        return failedAttempt;
    }

    public void setFailedAttempt(Double failedAttempt) {
        this.failedAttempt = failedAttempt;
    }

    public Boolean getFlagPasswordProtect() {
        return flagPasswordProtect;
    }

    public void setFlagPasswordProtect(Boolean flagPasswordProtect) {
        this.flagPasswordProtect = flagPasswordProtect;
    }

    public String getPdfPassword() {
        return pdfPassword;
    }

    public void setPdfPassword(String pdfPassword) {
        this.pdfPassword = pdfPassword;
    }

    public Boolean getFlagEmailAttachmentReady() {
        return flagEmailAttachmentReady;
    }

    public void setFlagEmailAttachmentReady(Boolean flagEmailAttachmentReady) {
        this.flagEmailAttachmentReady = flagEmailAttachmentReady;
    }

    public Double getPriority() {
        return priority;
    }

    public void setPriority(Double priority) {
        this.priority = priority;
    }

    public String getQueryRecordId() {
        return queryRecordId;
    }

    public void setQueryRecordId(String queryRecordId) {
        this.queryRecordId = queryRecordId;
    }

    public Timestamp getCreateddate() {
        return createddate;
    }

    public void setCreateddate(Timestamp createddate) {
        this.createddate = createddate;
    }
}

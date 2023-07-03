package com.docprocess.model;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "document_process_log", schema = "fmsapp")
@EntityListeners(AuditingEntityListener.class)

public class DocumentProcessLog implements Serializable{

    public DocumentProcessLog() {
    }

    @Id
    private Integer id;

    @Column(name = "sfid")
    private String sfid;

    @Column(name = "flag_notify_email")
    private Boolean flagNotifyEmail;

    @Column(name = "upload_tag_update")
    private Boolean uploadTagUpate;

    @Column(name ="process_complete")
    private Boolean processComplete;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSfid() {
        return sfid;
    }

    public void setSfid(String sfid) {
        this.sfid = sfid;
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

    public Boolean getProcessComplete() {
        return processComplete;
    }

    public void setProcessComplete(Boolean processComplete) {
        this.processComplete = processComplete;
    }
}

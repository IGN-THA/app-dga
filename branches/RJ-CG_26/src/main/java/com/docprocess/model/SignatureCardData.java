package com.docprocess.model;


import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "signature_card_data", schema = "fmsapp")
@EntityListeners(AuditingEntityListener.class)

public class SignatureCardData  implements Serializable{
    public SignatureCardData() {
    }

    public SignatureCardData(String signatureCardName, Integer signatureCardSlot, Timestamp createdDate) {
        this.signatureCardName = signatureCardName;
        this.signatureCardSlot = signatureCardSlot;
        this.createdDate = createdDate;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Signature_Card_Name")
    private String signatureCardName;

    @Column(name = "Signature_Card_Slot")
    private Integer signatureCardSlot;

    @Column(name = "Signature_Card_Key")
    private String signatureCardKey;

    @Column(name = "Signature_Card_Password")
    private String signatureCardPassword;

    @Column(name ="Created_Date")
    private Timestamp createdDate;

    @Column(name ="sign_owner")
    private String signOwner;

    @Column(name ="is_active")
    private boolean flagActive;



    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSignatureCardName() {
        return signatureCardName;
    }

    public void setSignatureCardName(String signatureCardName) {
        this.signatureCardName = signatureCardName;
    }

    public Integer getSignatureCardSlot() {
        return signatureCardSlot;
    }

    public void setSignatureCardSlot(Integer signatureCardSlot) {
        this.signatureCardSlot = signatureCardSlot;
    }

    public String getSignatureCardPassword() {
        return signatureCardPassword;
    }

    public void setSignatureCardPassword(String signatureCardPassword) {
        this.signatureCardPassword = signatureCardPassword;
    }

    public String getSignatureCardKey() {
        return signatureCardKey;
    }

    public void setSignatureCardKey(String signatureCardKey) {
        this.signatureCardKey = signatureCardKey;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getSignOwner() {
        return signOwner;
    }

    public void setSignOwner(String signOwner) {
        this.signOwner = signOwner;
    }

    public boolean isFlagActive() {
        return flagActive;
    }

    public void setFlagActive(boolean flagActive) {
        this.flagActive = flagActive;
    }
}

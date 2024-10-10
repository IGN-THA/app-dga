package com.docprocess.model;


import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

@Entity
@Table(name = "signature_card_data", schema = "fmsapp")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class SignatureCardData implements Serializable {
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

    @Column(name = "Created_Date")
    private Timestamp createdDate;

    @Column(name = "sign_owner")
    private String signOwner;

    @Column(name = "is_active")
    private boolean flagActive;

    @Column(name = "flag_skip_signing_doc")
    private Boolean flagSkipSigningDoc = false;

    @Column(name = "flag_signing_using_api")
    private Boolean flagSigningUsingAPI = false;

    @Column(name = "api_config_info")
    @Type(type = "jsonb")
    private Map<String, Object> apiConfigInfo;

    @Column(name = "api_request_info")
    @Type(type = "jsonb")
    private Map<String, Object> apiRequestInfo;
}

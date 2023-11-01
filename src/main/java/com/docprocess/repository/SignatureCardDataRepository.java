package com.docprocess.repository;

import com.docprocess.model.SignatureCardData;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignatureCardDataRepository extends JpaRepository<SignatureCardData, Integer> {

    @Cacheable(value="SignatureCardData", key="#signatureCardName")
    SignatureCardData findBySignatureCardName(String signatureCardName);

    List<SignatureCardData> findBySignOwnerAndFlagActive(String signOwner, Boolean isFlagActive);

    List<SignatureCardData> findAll();

    List<SignatureCardData> findByFlagSigningUsingAPIAndFlagSoftToken(Boolean isFlagSigning, Boolean flagSoftToken);
}

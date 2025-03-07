package com.docprocess.repository;

import com.docprocess.model.ExternalApiInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalApiInfoRepository extends JpaRepository<ExternalApiInfo, Integer> {

    Optional<ExternalApiInfo> findByApiKey(String apiKey);
}

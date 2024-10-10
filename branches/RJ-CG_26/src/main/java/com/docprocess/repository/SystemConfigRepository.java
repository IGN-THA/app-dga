package com.docprocess.repository;

import com.docprocess.model.SystemConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Integer> {

    @Cacheable(value="SystemConfig", key="#configName")
    SystemConfig findByConfigKey(String configName);

    @Query(nativeQuery = true, value = "SELECT 1")
    Integer extractByNativeQuery();
}

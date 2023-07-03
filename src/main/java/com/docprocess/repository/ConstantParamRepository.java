package com.docprocess.repository;

import com.docprocess.model.ConstantParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ConstantParamRepository extends JpaRepository<ConstantParam, Integer> {

    @Cacheable(value="ConstantParam", key="#constName")
    ConstantParam findByConstKey(String constName);

}

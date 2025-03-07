package com.docprocess.repository;

import com.docprocess.model.AppLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppLoginRepostiroy extends JpaRepository<AppLogin, Integer> {

    AppLogin findByAppKey(String secretKey);
}

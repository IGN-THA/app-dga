package com.ignite.batch.repository;

import com.ignite.batch.model.BatchScheduleInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulerRepository extends JpaRepository<BatchScheduleInfo, Long> {

}

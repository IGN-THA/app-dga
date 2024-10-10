package com.ignite.batch.service;

import com.ignite.batch.model.BatchScheduleInfo;

public interface SchedulerServiceImpl  {
    void startAllSchedulers();

        void scheduleNewJob(BatchScheduleInfo jobInfo);

        void updateScheduleJob(BatchScheduleInfo jobInfo);

        boolean unScheduleJob(String jobName);

        boolean deleteJob(BatchScheduleInfo jobInfo);

        boolean pauseJob(BatchScheduleInfo jobInfo);

        boolean resumeJob(BatchScheduleInfo jobInfo);

        boolean startJobNow(BatchScheduleInfo jobInfo);

}
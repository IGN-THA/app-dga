package com.ignite.batch.component;

import com.ignite.batch.service.SchedulerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class SchedulerStartUpHandler implements ApplicationRunner {

    @Autowired
    private SchedulerService schedulerService;

    Logger logger = LogManager.getLogger(SchedulerStartUpHandler.class);
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Schedule all new scheduler jobs at app startup - starting");
        try {
            schedulerService.startAllSchedulers();
            logger.info("Schedule all new scheduler jobs at app startup - complete");
        } catch (Exception ex) {
            logger.error("Schedule all new scheduler jobs at app startup - error");
        }
    }
}

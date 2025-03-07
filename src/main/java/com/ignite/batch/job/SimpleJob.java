package com.ignite.batch.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.stream.IntStream;

@DisallowConcurrentExecution
public class SimpleJob extends QuartzJobBean {

    Logger logger = LogManager.getLogger(SimpleJob.class);
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        logger.info("Counting - {1}");
        IntStream.range(0, 5).forEach(i -> {
            try {
                Thread.sleep(1000);
                logger.info("Counting - {}"+ i);
            } catch (InterruptedException e) {
                logger.error("Exception occured in class SimpleJob" + e.getMessage());
            }
        });
        logger.info("Counting - {2}");
    }
}

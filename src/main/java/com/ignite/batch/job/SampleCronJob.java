package com.ignite.batch.job;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.stream.IntStream;

@DisallowConcurrentExecution
public class SampleCronJob extends QuartzJobBean {

    Logger logger = LogManager.getLogger(SampleCronJob.class);
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        logger.info("SampleCronJob Start................");
        IntStream.range(0, 10).forEach(i -> {
            logger.info("Counting - {}"+ i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage()+ e);
            }
        });
        logger.info("SampleCronJob End................");
    }
}

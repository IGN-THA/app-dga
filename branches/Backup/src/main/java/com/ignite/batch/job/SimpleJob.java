package com.ignite.batch.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.stream.IntStream;

@DisallowConcurrentExecution
public class SimpleJob extends QuartzJobBean {
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Counting - {1}");
        IntStream.range(0, 5).forEach(i -> {
            try {
                Thread.sleep(1000);
                System.out.println("Counting - {}"+ i);
            } catch (InterruptedException e) {

            }
        });
        System.out.println("Counting - {2}");
    }
}

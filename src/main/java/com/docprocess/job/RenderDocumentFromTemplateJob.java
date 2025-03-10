package com.docprocess.job;

import com.docprocess.manager.docx.RenderDocumentManager;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@DisallowConcurrentExecution
public class RenderDocumentFromTemplateJob extends QuartzJobBean {

    @Autowired
    private EntityManagerFactory sessionFactory;

    @Autowired
    DocumentDataRepository documentDataRepository;

    @Autowired
    DocumentTypeDataRepository documentTypeDataRepository;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Autowired
    ConstantParamRepository constantParamRepository;

    @Autowired
    ExternalApiInfoRepository externalApiInfoRepository;

    @Autowired
    String rootFilePath;

    @Autowired
    String renderedFilePath;

    @Autowired
    @Qualifier("s3TemplateFolder")
    String bucketFolderPath;

    @Autowired
    String folderPathOnServer;

    Logger logger = LogManager.getLogger(RenderDocumentFromTemplateJob.class);
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long batchStart = System.currentTimeMillis();
        Date currDate = new Date();

        String pattern = "ddMMyyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String newFolderName = simpleDateFormat.format(currDate);

        logger.info("Render Batch started "+ batchStart);
        Timestamp nonPriorityTime = Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusMinutes(5));
        Timestamp priorityTime = Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusMinutes(2));

        //Dev


        List<DocumentGenerateQueueData> documentGenerateQueueDataList = documentDataRepository.findTop40ByCreateddateLessThanAndRenderedDateAndPriorityAndFailedAttemptLessThanEqualOrderByCreateddateAsc(nonPriorityTime, null, null,3.0);
        List<DocumentGenerateQueueData> documentGenerateQueueDataPriorityList = documentDataRepository.findTop10ByCreateddateLessThanAndRenderedDateAndPriorityNotNullAndFailedAttemptLessThanEqualOrderByPriorityAsc(priorityTime, null, 3.0);
//        DocumentGenerateQueueData documentGenerateQueueDataPriority = documentDataRepository.findBySfid("a116D0000038OqiQAE");
        //documentGenerateQueueDataList.add(documentGenerateQueueDataPriority);
        if(documentGenerateQueueDataPriorityList!=null && !documentGenerateQueueDataPriorityList.isEmpty()){
            documentGenerateQueueDataList.addAll(documentGenerateQueueDataPriorityList);
        }
        File rootFolder = null;
        String renderedFilePathInPorgress = renderedFilePath+"_InProgress";
        if (!(rootFolder = new File(renderedFilePathInPorgress)).exists())
            rootFolder.mkdir();

        if (!(rootFolder = new File(renderedFilePath+"\\"+newFolderName)).exists())
            new File(renderedFilePath+"\\"+newFolderName).mkdir();

        if(documentGenerateQueueDataList.size()>0) {


            String renderedFilePathForJob = renderedFilePathInPorgress + "\\InProgress" + batchStart;

            File batchFolder = null;
            if (!(batchFolder = new File(renderedFilePathForJob)).exists())
                batchFolder.mkdirs();

            Integer batchSize = 30;

            for (DocumentGenerateQueueData docData : documentGenerateQueueDataList) {
                logger.info(renderedFilePath);
                logger.info(docData.getSfid());

                batchSize--;
                DocumentTypeData docTypeData = documentTypeDataRepository.findByDocumentType(docData.getDocumentType());
                if(docTypeData==null) continue;
                String tempName = docTypeData.getTemplateName();
                String tableName = docTypeData.getQueryName();

                boolean rederedDoc = false;
                try {
                    RenderDocumentManager mgr = new RenderDocumentManager();
                    rederedDoc = mgr.renderDocFromTemplate(renderedFilePathForJob, folderPathOnServer + "/"+bucketFolderPath,folderPathOnServer + "/"+bucketFolderPath+"/" + tempName, tableName, sessionFactory, docData, constantParamRepository, externalApiInfoRepository);
                } catch (Exception e) {
                    String eMsg = (e.getMessage() == null) ? e.toString() : e.getMessage();
                    eMsg = (eMsg.length() > 250) ? eMsg.substring(0, 250) : eMsg;
                    docData.setErrorMessage(eMsg);
                }
                if (rederedDoc) {
                    docData.setRenderedDate(Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)));
                } else {
                    docData.setFailedAttempt((docData.getFailedAttempt() == null) ? 1 : docData.getFailedAttempt() + 1);
                }
                documentDataRepository.saveAndFlush(docData);
                if (batchSize <= 0) break;
            }
            if (batchFolder != null && batchFolder.exists() && renderedFilePathForJob != null) {
                for(File temoFObj : batchFolder.listFiles()) {
                    temoFObj.renameTo(new File(renderedFilePath + "\\" + newFolderName +"\\"+temoFObj.getName()));
                }
            }
        }
        for(File fileObj : new File(renderedFilePathInPorgress).listFiles()){
            if(fileObj.isDirectory()){
                if(fileObj.listFiles().length>0) {
                    for(File temoFObj : fileObj.listFiles()) {
                        temoFObj.renameTo(new File(renderedFilePath + "\\" + newFolderName +"\\"+temoFObj.getName()));
                    }
                }
            }
        }

        for(File fileObj : new File(renderedFilePathInPorgress).listFiles()){
            if(fileObj.isDirectory()){
                if(fileObj.listFiles().length==0) {
                    fileObj.delete();
                }
            }
        }
/*
        for(File fileObj : new File(renderedFilePath).listFiles()){
            if(fileObj.isDirectory() && fileObj.listFiles().length==0){
                fileObj.delete();
            }
        }*/

        logger.info("Render Batch End "+ System.currentTimeMillis() + " within " +(System.currentTimeMillis()-batchStart));
    }

}

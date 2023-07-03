package com.docprocess.job;

import com.docprocess.config.ConfigConstant;
import com.docprocess.manager.docx.RenderDocumentManager;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.model.DocumentTypeData;
import com.docprocess.repository.ConstantParamRepository;
import com.docprocess.repository.DocumentDataRepository;
import com.docprocess.repository.DocumentTypeDataRepository;
import com.docprocess.repository.SystemConfigRepository;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

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



    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long batchStart = System.currentTimeMillis();
        Date currDate = new Date();

        String pattern = "ddMMyyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String newFolderName = simpleDateFormat.format(currDate);

        System.out.println("Render Batch started "+ batchStart);
        Timestamp nonPriorityTime = Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusMinutes(5));
        Timestamp priorityTime = Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusMinutes(2));
        List<DocumentGenerateQueueData> documentGenerateQueueDataList = documentDataRepository.findTop40ByCreateddateLessThanAndRenderedDateAndPriorityAndFailedAttemptLessThanEqualOrderByCreateddateAsc(nonPriorityTime, null, null,3.0);
        List<DocumentGenerateQueueData> documentGenerateQueueDataPriorityList = documentDataRepository.findTop10ByCreateddateLessThanAndRenderedDateAndPriorityNotNullAndFailedAttemptLessThanEqualOrderByPriorityAsc(priorityTime, null, 3.0);
        if(documentGenerateQueueDataPriorityList!=null && !documentGenerateQueueDataPriorityList.isEmpty()){
            documentGenerateQueueDataList.addAll(documentGenerateQueueDataPriorityList);
        }
        File rootFolder = null;
        String rootFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
        String renderedFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.RENDERED_FILE_PATH).getConfigValue();
        String bucketFolderPath = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_TEMPLATE_FOLDER_PATH).getConfigValue();
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
                System.out.println(renderedFilePath);
                System.out.println(docData.getSfid());

                batchSize--;
                DocumentTypeData docTypeData = documentTypeDataRepository.findByDocumentType(docData.getDocumentType());
                if(docTypeData==null) continue;
                String tempName = docTypeData.getTemplateName();
                String tableName = docTypeData.getQueryName();


                String folderPathOnServer = rootFilePath + "\\" + systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_TEMPLATE_PATH_ON_SERVER).getConfigValue();
                boolean rederedDoc = false;
                try {
                    RenderDocumentManager mgr = new RenderDocumentManager();
                    rederedDoc = mgr.renderDocFromTemplate(renderedFilePathForJob, folderPathOnServer + "/"+bucketFolderPath,folderPathOnServer + "/"+bucketFolderPath+"/" + tempName, tableName, sessionFactory, docData, constantParamRepository);
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

        System.out.println("Render Batch End "+ System.currentTimeMillis() + " within " +(System.currentTimeMillis()-batchStart));
    }

}

package com.docprocess.job;

import com.docprocess.config.ErrorConfig;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.repository.DocumentDataRepository;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@DisallowConcurrentExecution
public class PDFConversionJob extends QuartzJobBean {
    @Autowired
    DocumentDataRepository documentDataRepository;

    @Autowired
    String rootFilePath;

    @Autowired
    String renderedFilePath;

    @Autowired
    String pdfFilePath;

    Logger logger = LogManager.getLogger(PDFConversionJob.class);
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long batchStart = System.currentTimeMillis();
        logger.info("PDF Conv Batch started " + batchStart);

        File renderedFileFolder = new File(renderedFilePath);


        if (renderedFileFolder.listFiles().length > 0) {

            File renderedPathForCoversion = null;
            List<File> deleteFileList = new ArrayList<File>();
            for (File fileObj : renderedFileFolder.listFiles()) {
                if (!fileObj.getName().startsWith("InProgress") && fileObj.isDirectory()) {
                    if (fileObj.listFiles().length == 0) {
                        deleteFileList.add(fileObj);
                    } else {
                        renderedPathForCoversion = fileObj;
                        break;
                    }
                }
            }

            for (File fObj : deleteFileList) {
                fObj.delete();
            }

            if (renderedPathForCoversion != null && renderedPathForCoversion.listFiles().length > 0) {

                File pdfFileFolder = new File(pdfFilePath);
                String pdfFolderName = null;
                if (!pdfFileFolder.exists()) {
                    pdfFileFolder.mkdir();
                }
                pdfFolderName = pdfFilePath + "\\InProgress" + renderedPathForCoversion.getName();
                pdfFileFolder = new File(pdfFilePath + "\\InProgress" + renderedPathForCoversion.getName());
                if (!pdfFileFolder.exists()) pdfFileFolder.mkdir();


//                String line = "python PDFConversion.py \"" + renderedPathForCoversion.getAbsolutePath() + "\" \"" + pdfFileFolder.getAbsolutePath() + "\"";
//                System.out.println(line);
//                CommandLine cmdLine = CommandLine.parse(line);
//                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
//                DefaultExecutor executor = new DefaultExecutor();
//                executor.setWorkingDirectory(new File("./PythonScript"));
//                executor.setStreamHandler(streamHandler);
//                try {
//                    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
//                    executor.execute(cmdLine);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }


                Boolean coversionStatus = covertToPDF(renderedPathForCoversion.getAbsolutePath(), pdfFileFolder.getAbsolutePath());
                if(!coversionStatus)
                    handlePDFConversionOnIndividualFile(renderedPathForCoversion, pdfFileFolder);


                for (File fileObj : pdfFileFolder.listFiles()) {
                    String fileName = fileObj.getName();
                    fileName = fileName.substring(0, fileName.indexOf(".")) + ".docx";
                    new File(renderedPathForCoversion + "\\" + fileName).delete();
                }

                if (renderedPathForCoversion.listFiles().length == 0) {
                    //renderedPathForCoversion.delete();
                    pdfFileFolder.renameTo(new File(pdfFolderName.replace("InProgress", "")));
                }
            }
        }
        for (File fileObj : new File(pdfFilePath).listFiles()) {
            if (fileObj.isDirectory() && fileObj.getName().startsWith("InProgress")) {
                fileObj.renameTo(new File(fileObj.getAbsolutePath().replace("InProgress", "")));
            }
        }


        logger.info("PDF Conv Batch End "+ System.currentTimeMillis() + " within " +(System.currentTimeMillis()-batchStart));

    }

    private void handlePDFConversionOnIndividualFile(File renderedPathForCoversion, File pdfFileFolder){
        if(renderedPathForCoversion.listFiles().length>0){
            List<File> deleteFileList = new ArrayList<File>();
            for (File fileObj : renderedPathForCoversion.listFiles()) {
                String fileName = fileObj.getName().substring(0,fileObj.getName().lastIndexOf("."))+".pdf";
                Boolean status = covertToPDF(fileObj.getAbsolutePath(), pdfFileFolder.getAbsolutePath()+"\\"+fileName);
                if(!status){
                    String recordId = fileName.substring(0, fileName.indexOf("_"));
                    DocumentGenerateQueueData docDataObj = documentDataRepository.findBySfid(recordId);
                    docDataObj.setErrorMessage("PDF Conversion Failed");
                    documentDataRepository.saveAndFlush(docDataObj);
                    deleteFileList.add(fileObj);
                }
            }
            /*for (File fObj : deleteFileList) {
                fObj.delete();
            }*/
        }

    }


    private Boolean covertToPDF(String wordFilePath, String pdfFilePath){
        String line = "python PDFConversion.py \"" + wordFilePath + "\" \"" + pdfFilePath + "\"";
        logger.info(line);
        CommandLine cmdLine = CommandLine.parse(line);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File("./PythonScript"));
        executor.setStreamHandler(streamHandler);
        try {
            executor.execute(cmdLine);
        } catch (Exception e) {

            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "covertToPDF", e);
            logger.error(errorMessage);
            return false;
        }
        //System.out.println(outputStream.toString());
        return true;
    }
}

package com.docprocess.job;

import com.docprocess.config.ConfigConstant;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.repository.DocumentDataRepository;
import com.docprocess.repository.SystemConfigRepository;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@DisallowConcurrentExecution
public class PDFConversionJob extends QuartzJobBean {
    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Autowired
    DocumentDataRepository documentDataRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long batchStart = System.currentTimeMillis();
        System.out.println("PDF Conv Batch started "+ batchStart);

        String rootFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
        String renderedFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.RENDERED_FILE_PATH).getConfigValue();
        String pdfFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue();

        File renderedFileFolder = new File(renderedFilePath);


        if(renderedFileFolder.listFiles().length>0) {

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


        System.out.println("PDF Conv Batch End "+ System.currentTimeMillis() + " within " +(System.currentTimeMillis()-batchStart));

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
        System.out.println(line);
        CommandLine cmdLine = CommandLine.parse(line);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File("./PythonScript"));
        executor.setStreamHandler(streamHandler);
        try {
            executor.execute(cmdLine);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Script Failed: "+outputStream.toString());
            return false;
        }
        //System.out.println(outputStream.toString());
        return true;
    }
}

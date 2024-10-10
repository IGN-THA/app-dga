package com.docprocess.manager;

import com.docprocess.config.ConfigConstant;
import com.docprocess.model.SignatureCardData;
import com.docprocess.repository.SignatureCardDataRepository;
import com.docprocess.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

@Component
public class InitializeApplicationConfig implements CommandLineRunner {

    @Autowired
    SignatureCardDataRepository signatureCardDataRepository;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    @Override
    public void run(String...args) throws  Exception{
        DigiSignDocManager docMgr = new DigiSignDocManager(true);
        HashMap<String, Long> mapKeySlot = docMgr.mapKeySlot;

        List<SignatureCardData> signatureCardDataList = signatureCardDataRepository.findAll();
        for(SignatureCardData cardData : signatureCardDataList){
            cardData.setFlagActive(false);
        }
        signatureCardDataRepository.saveAll(signatureCardDataList);
        signatureCardDataRepository.flush();

        for(String key : mapKeySlot.keySet()){
            SignatureCardData signCardData = signatureCardDataRepository.findBySignatureCardName(key);
            if(signCardData==null) signCardData = new SignatureCardData(key,Integer.valueOf(mapKeySlot.get(key).toString()), new Timestamp(System.currentTimeMillis()));
            signCardData.setSignatureCardSlot(Integer.valueOf(mapKeySlot.get(key).toString()));
            signCardData.setFlagActive(true);
            signatureCardDataRepository.saveAndFlush(signCardData);
        }


        String rootFilePath = systemConfigRepository.findByConfigKey(ConfigConstant.ROOT_FOLDER_TO_STORE_LOCAL_FILE).getConfigValue();
        File f = new File(rootFilePath);
        if(!f.exists()) f.mkdir();
        String renderedFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.RENDERED_FILE_PATH).getConfigValue();
        f = new File(renderedFilePath);
        if(!f.exists()) f.mkdir();
        String pdfFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue();
        f = new File(pdfFilePath);
        if(!f.exists()) f.mkdir();
        String tempFilePath = rootFilePath+"\\"+systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_STORAGE_PATH_FOR_PRINTING).getConfigValue()+"\\Temp";
        f = new File(tempFilePath);
        if(!f.exists()) f.mkdir();

        String accessKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_ACCESS_KEY).getConfigValue();
        String secretKey = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_SECRET_KEY).getConfigValue();
        String bucketName = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_HOME_BUCKET_NAME).getConfigValue();
        String bucketFolderPath = systemConfigRepository.findByConfigKey(ConfigConstant.AWS_S3_TEMPLATE_FOLDER_PATH).getConfigValue();
        String folderPathOnServer = rootFilePath+"\\" +systemConfigRepository.findByConfigKey(ConfigConstant.DOCUMENT_TEMPLATE_PATH_ON_SERVER).getConfigValue();
        S3BucketManager s3Mgr = new S3BucketManager(accessKey, secretKey);

        s3Mgr.synchronizeDocumentToServer(bucketName, bucketFolderPath, folderPathOnServer, true);
    }

}

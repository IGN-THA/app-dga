package com.docprocess.manager;

import com.docprocess.model.SignatureCardData;
import com.docprocess.repository.SignatureCardDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

@Component
public class InitializeApplicationConfig implements CommandLineRunner {

    @Autowired
    private SignatureCardDataRepository signatureCardDataRepository;

    @Autowired
    @Qualifier("s3HomeBucketName")
    private String bucketName;

    @Autowired
    @Qualifier("s3TemplateFolder")
    private String bucketFolderPath;

    @Autowired
    private String rootFilePath;

    @Autowired
    private String renderedFilePath;

    @Autowired
    private String pdfFilePath;

    @Autowired
    private String tempFilePath;

    @Autowired
    private String folderPathOnServer;

    @Autowired
    private S3BucketManager s3Mgr;

    @Value("${fmsapp.env}")
    private String currentEnv;

    @Value("${spring.job.start}")
    private Boolean isDocGenBatch;

    @Override
    public void run(String... args) throws Exception {
        DigiSignDocManager docMgr = new DigiSignDocManager(true);
        HashMap<String, Long> mapKeySlot = DigiSignDocManager.mapKeySlot;

        //List<SignatureCardData> signatureCardDataList = signatureCardDataRepository.findAll();
        List<SignatureCardData> signatureCardDataList = signatureCardDataRepository.findByFlagSigningUsingAPIAndFlagSoftToken(false, false);
        for (SignatureCardData cardData : signatureCardDataList) {
            if(currentEnv.equalsIgnoreCase("PROD") && isDocGenBatch)
                cardData.setFlagActive(false);
        }
        signatureCardDataRepository.saveAll(signatureCardDataList);
        signatureCardDataRepository.flush();

        for (String key : mapKeySlot.keySet()) {
            SignatureCardData signCardData = signatureCardDataRepository.findBySignatureCardName(key);
            if (signCardData == null)
                signCardData = new SignatureCardData(key, Integer.valueOf(mapKeySlot.get(key).toString()), new Timestamp(System.currentTimeMillis()));
            signCardData.setSignatureCardSlot(Integer.valueOf(mapKeySlot.get(key).toString()));
            signCardData.setFlagActive(true);
            signatureCardDataRepository.saveAndFlush(signCardData);
        }

        Stream.of(rootFilePath, renderedFilePath, pdfFilePath, tempFilePath)
                .map(File::new)
                .filter(f -> !f.exists())
                .forEach(File::mkdir);

        s3Mgr.synchronizeDocumentToServer(bucketName, bucketFolderPath, folderPathOnServer, true);
    }
}

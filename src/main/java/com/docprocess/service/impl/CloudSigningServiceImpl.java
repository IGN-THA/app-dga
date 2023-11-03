package com.docprocess.service.impl;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.certificates.CertificateClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificateWithPolicy;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.docprocess.service.CloudSigningService;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfSmartCopy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CloudSigningServiceImpl implements CloudSigningService {
    public static final String KEYVAULTNAME = "roojai-az-keyvault";
    public static final String KEYVAULTURI = "https://" + KEYVAULTNAME + ".vault.azure.net";
    private final String certificateName;

    Logger LOG = LogManager.getLogger(CloudSigningServiceImpl.class);
    public CloudSigningServiceImpl(String certificateName) {
        this.certificateName = certificateName;
    }

    public static CertificateClient createClient() {
        CertificateClient certificateClient = new CertificateClientBuilder()
                .vaultUrl(KEYVAULTURI)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        return certificateClient;
    }

    public static SecretClient createSecretClient() {
        SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(KEYVAULTURI)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        return secretClient;
    }

    public String GetSecret(KeyVaultCertificateWithPolicy certificateWithPolicy){
        KeyVaultSecret secret = createSecretClient().getSecret(certificateName, certificateWithPolicy.getProperties().getVersion());
        String downloadedPK = secret.getValue();
        return downloadedPK;
    }

    @Override
    public String getCertValue(InputStream srcStream, String fileName, String pdfPassword) {
        BouncyCastleProvider providerBC = new BouncyCastleProvider();
        Security.addProvider(providerBC);
        try {
            LOG.info("Retrive Certificate....");
            KeyVaultCertificateWithPolicy certificateWithPolicy = createClient().getCertificate(certificateName);
            byte[] byteCertificate = certificateWithPolicy.getCer();
            InputStream inputStreamCert = new ByteArrayInputStream(byteCertificate);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate[] chain = new Certificate[1];
            chain[0] = certificateFactory.generateCertificate(inputStreamCert);
            LOG.info("Certificate Complete" + chain[0]);
            LOG.info("Retrive Secret....");
            String downloadedPK = GetSecret(certificateWithPolicy);
            byte[] privateKeyBytes = Base64.getDecoder().decode(downloadedPK);
            InputStream bais = new ByteArrayInputStream(privateKeyBytes);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(bais, null);
            char[] pass = "".toCharArray();
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias,pass);
            LOG.info("Secret Complete" + privateKey);
            SignDoc(chain,privateKey,srcStream,fileName);
            LOG.info("Sign Complete");
            return "Sign Complete";
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return "error";
        }
    }
    public OutputStream SignDoc(Certificate[] chain,PrivateKey privateKey, InputStream srcStream,String fileName ) throws Exception{
        LOG.info("Start Sign Document...");
        BouncyCastleProvider providerBC = new BouncyCastleProvider();
        Security.addProvider(providerBC);
        List<ICrlClient> crlList = new ArrayList<>();
        crlList.add(new CrlClientOnline(chain));
        FileOutputStream tempFS = null;
        PdfReader reader=null;
        InputStream srcStream1=null;
        try{
            Document document = new Document();
            tempFS = new FileOutputStream(new File(fileName.replace(".pdf", "_copy.pdf")));
            PdfCopy copy = new PdfSmartCopy(document, tempFS);
            document.open();
            IOcspClient ocspClient = new OcspClientBouncyCastle(null);
            com.itextpdf.text.pdf.PdfReader pdfReader = new com.itextpdf.text.pdf.PdfReader(srcStream);
            copy.addDocument(pdfReader);
            pdfReader.close();
            document.close();
            tempFS.close();
            srcStream1 = new FileInputStream(fileName.replace(".pdf", "_copy.pdf"));
            reader = new PdfReader(srcStream1);
            tempFS = new FileOutputStream(fileName);
            PdfSigner signer = new PdfSigner(reader, tempFS, true);
            IExternalDigest digest = new BouncyCastleDigest();
            IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, providerBC.getName());
            signer.signDetached(digest, pks, chain, crlList, ocspClient,null, 0, PdfSigner.CryptoStandard.CMS);
        }catch(Exception e){
            if(tempFS!=null)tempFS.close();
            if(srcStream1!=null) srcStream1.close();
            if(new File(fileName.replace(".pdf", "_copy.pdf")).exists()) new File(fileName.replace(".pdf", "_copy.pdf")).delete();
            throw e;
        }finally {
            if(tempFS!=null)tempFS.close();
            if(srcStream1!=null) srcStream1.close();
            if(new File(fileName.replace(".pdf", "_copy.pdf")).exists()) new File(fileName.replace(".pdf", "_copy.pdf")).delete();
        }
        return tempFS;
    }
}
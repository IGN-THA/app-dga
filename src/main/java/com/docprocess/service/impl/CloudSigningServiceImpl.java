package com.docprocess.service.impl;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.certificates.CertificateClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificate;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificateWithPolicy;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.docprocess.model.SignatureCardData;
import com.docprocess.service.CloudSigningService;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.ReaderProperties;
import com.itextpdf.signatures.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfSmartCopy;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class CloudSigningServiceImpl implements CloudSigningService {

    Logger LOG = LogManager.getLogger(CloudSigningServiceImpl.class);

    String keyVaultUri;
    String certificateName;

    public CertificateClient createClient() {
        CertificateClient certificateClient = new CertificateClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        return certificateClient;
    }

    public SecretClient createSecretClient() {
        SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        return secretClient;
    }

    public String GetSecret(KeyVaultCertificate certificateWithPolicy){

        KeyVaultSecret secret = createSecretClient().getSecret(certificateName, certificateWithPolicy.getProperties().getVersion());
        String downloadedPK = secret.getValue();
        return downloadedPK;
    }

    @Override
    public String signWithVaultCertificate(InputStream srcStream, String fileName, String pdfPassword, String pdfPasswordOwner, SignatureCardData signatureCardData) {
        BouncyCastleProvider providerBC = new BouncyCastleProvider();
        Security.addProvider(providerBC);
        try {
            String certificateVersion = null;
            if(signatureCardData != null){
                keyVaultUri = (String) signatureCardData.getApiConfigInfo().get("host_name");
                certificateName = (String) signatureCardData.getApiConfigInfo().get("certificate_name");
                certificateVersion = (String) signatureCardData.getApiConfigInfo().get("version");
            }
            LOG.info("Retrive Certificate....");

            KeyVaultCertificate certificateWithPolicy = createClient().getCertificateVersion(certificateName, certificateVersion);
            byte[] byteCertificate = certificateWithPolicy.getCer();
            InputStream inputStreamCert = new ByteArrayInputStream(byteCertificate);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate[] chain = new Certificate[1];
            chain[0] = certificateFactory.generateCertificate(inputStreamCert);
            LOG.info("Certificate Complete");
            LOG.info("Retrive Secret....");
            String downloadedPK = GetSecret(certificateWithPolicy);
            if (downloadedPK == null || downloadedPK.isEmpty()) {
                throw new IllegalStateException("Secret is not available or is disabled.");
            }
            byte[] privateKeyBytes = Base64.getDecoder().decode(downloadedPK);
            InputStream bais = new ByteArrayInputStream(privateKeyBytes);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(bais, null);
            char[] pass = "".toCharArray();
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias,pass);
            LOG.info("Secret Complete");
            SignDoc(chain,privateKey,srcStream,fileName, pdfPassword, pdfPasswordOwner);
            LOG.info("Sign Complete");
            return "Sign Complete";
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            return "error";
        }
    }
    public OutputStream SignDoc(Certificate[] chain,PrivateKey privateKey, InputStream srcStream,String fileName, String pdfPassword, String pdfPasswordOwner ) throws Exception{
        LOG.info("Start Sign Document...");
        BouncyCastleProvider providerBC = new BouncyCastleProvider();
        Security.addProvider(providerBC);
        List<ICrlClient> crlList = new ArrayList<>();
        crlList.add(new CrlClientOnline(chain));
        FileOutputStream tempFS = null;
        PdfReader reader=null;
        InputStream srcStream1=null;
        try{
            if(pdfPassword != null && pdfPassword.length() > 0){
                passwordProtectDocument(srcStream, pdfPassword.getBytes(), pdfPasswordOwner.getBytes(), fileName.replace(".pdf", "_copy.pdf"));
                srcStream1 = new FileInputStream(fileName.replace(".pdf", "_copy.pdf"));
                reader = new PdfReader(srcStream1, new ReaderProperties().setPassword(pdfPasswordOwner.getBytes()));
                LOG.info("Cloud Signing with password protect----");
            }else {
                Document document = new Document();
                tempFS = new FileOutputStream(new File(fileName.replace(".pdf", "_copy.pdf")));
                PdfCopy copy = new PdfSmartCopy(document, tempFS);
                document.open();
                com.itextpdf.text.pdf.PdfReader pdfReader = new com.itextpdf.text.pdf.PdfReader(srcStream);
                copy.addDocument(pdfReader);
                pdfReader.close();
                document.close();
                tempFS.close();
                srcStream1 = new FileInputStream(fileName.replace(".pdf", "_copy.pdf"));
                reader = new PdfReader(srcStream1);
            }

            IOcspClient ocspClient = new OcspClientBouncyCastle(null);
            tempFS = new FileOutputStream(fileName);
            PdfSigner signer = new PdfSigner(reader, tempFS, true);
            IExternalDigest digest = new BouncyCastleDigest();
            IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, providerBC.getName());
            signer.signDetached(digest, pks, chain, crlList, ocspClient, null, 0, PdfSigner.CryptoStandard.CMS);

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

    public OutputStream passwordProtectDocument(InputStream srcStream,  byte[] userPassword, byte[] ownerPassword, String fileDestination) throws IOException, DocumentException {
        com.itextpdf.text.pdf.PdfReader reader = new com.itextpdf.text.pdf.PdfReader(srcStream);
        FileOutputStream fout = new FileOutputStream(fileDestination);
        PdfStamper stamper = new PdfStamper(reader, fout);
        stamper.setEncryption(userPassword, ownerPassword,
                PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128 | PdfWriter.DO_NOT_ENCRYPT_METADATA);
        stamper.close();
        reader.close();
        fout.close();
        return fout;
    }
}
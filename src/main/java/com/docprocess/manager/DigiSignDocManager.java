package com.docprocess.manager;



import com.docprocess.config.ErrorConfig;
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
import sun.security.pkcs11.SunPKCS11;
import sun.security.pkcs11.wrapper.CK_C_INITIALIZE_ARGS;
import sun.security.pkcs11.wrapper.CK_TOKEN_INFO;
import sun.security.pkcs11.wrapper.PKCS11;
import sun.security.pkcs11.wrapper.PKCS11Exception;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.util.*;
import java.security.*;
import java.security.cert.Certificate;

public class DigiSignDocManager {
    public static long[] slots=null;
    public static String dllPath=null;
    public static HashMap<String, Long> mapKeySlot = new HashMap<String, Long>();
    public String pdfPasswordOwner = null;

    Logger logger = LogManager.getLogger(DigiSignDocManager.class);

    public DigiSignDocManager(Boolean initToken) throws Exception {
        initToken(initToken);
    }

    public DigiSignDocManager() throws Exception {
        initToken(false);
    }

    public DigiSignDocManager(String pdfPasswordOwner) throws Exception {
        this.pdfPasswordOwner = pdfPasswordOwner;
        initToken(false);
    }

    private void initToken(Boolean updateToken) throws Exception{
        dllPath = "C:/Windows/System32/eTPKCS11.dll";
        if (!Files.exists(Paths.get(dllPath))) {
            return;
        }
        slots = getSlotsWithTokens(dllPath, updateToken);
    }

    public OutputStream signDocument(String keyName, Integer slot, String password, InputStream srcStream, String fileName, String pdfPassword) throws Exception {

        if (slots != null && slots.length>=1) {

            String config = "name=eToken\n" +
                    "library=" + dllPath + "\n" +
                    "slotListIndex = " + slot;
            ByteArrayInputStream bais = new ByteArrayInputStream(config.getBytes());
            Provider providerPKCS11 = new SunPKCS11(bais);
            Security.addProvider(providerPKCS11);
            KeyStore ks = KeyStore.getInstance("PKCS11");
            ks.load(bais, password.toCharArray());

//            Enumeration<String> aliases = ks.aliases();
//            while (aliases.hasMoreElements()) {
//                System.out.println(aliases.nextElement());
//            }

            return smartCardSign("SunPKCS11-eToken", ks, password, keyName, srcStream, fileName, pdfPassword);
        } else {
            throw new SignatureFailureException("Token is not available for reading");
        }
    }

    public OutputStream smartCardSign(String provider, KeyStore ks, String password, String alias, InputStream srcStream, String fileName, String pdfPassword)
            throws GeneralSecurityException, IOException, DocumentException {
        PrivateKey pk = (PrivateKey) ks.getKey(alias, password.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);
        IOcspClient ocspClient = new OcspClientBouncyCastle(null);
        List<ICrlClient> crlList = new ArrayList<>();
        crlList.add(new CrlClientOnline(chain));

        return sign(srcStream, chain, pk, DigestAlgorithms.SHA256, provider,
                PdfSigner.CryptoStandard.CMS,
                crlList, ocspClient, null, 0, fileName, pdfPassword);
    }

    public OutputStream sign(InputStream srcStream, Certificate[] chain, PrivateKey pk,
                             String digestAlgorithm, String provider, PdfSigner.CryptoStandard subfilter,
                             Collection<ICrlClient> crlList, IOcspClient ocspClient, ITSAClient tsaClient,
                             int estimatedSize, String fileName, String pdfPassword)
            throws GeneralSecurityException, IOException, DocumentException {




        //FileInputStream tempFisStream = new FileInputStream(new File(fileName));
        PdfReader reader=null;
        InputStream srcStream1=null;
        FileOutputStream tempFS = null;
        try {
            if (pdfPassword != null && pdfPassword.length() > 0) {
                passwordProtectDocument(srcStream, pdfPassword.getBytes(), pdfPasswordOwner.getBytes(), fileName.replace(".pdf", "_copy.pdf"));
                srcStream1 = new FileInputStream(fileName.replace(".pdf", "_copy.pdf"));
                reader = new PdfReader(srcStream1, new ReaderProperties().setPassword(pdfPasswordOwner.getBytes()));
            } else {
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
            //File signedFile = new File(fileName.replace(".pdf", "_Signed.pdf"));
            tempFS = new FileOutputStream(fileName);
            //ByteArrayOutputStream destStream = new ByteArrayOutputStream();
            PdfSigner signer = new PdfSigner(reader, tempFS, true);


            IExternalSignature pks = new PrivateKeySignature(pk, digestAlgorithm, provider);
            IExternalDigest digest = new BouncyCastleDigest();

            signer.signDetached(digest, pks, chain, crlList, ocspClient, tsaClient, estimatedSize, subfilter);
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
        //srcStream.close();

        //new File(fileName).delete();
        return tempFS;
    }

    // Method returns a list of token slot's indexes
    public long[] getSlotsWithTokens(String libraryPath, Boolean updateToken) {
        CK_C_INITIALIZE_ARGS initArgs = new CK_C_INITIALIZE_ARGS();
        String functionList = "C_GetFunctionList";

        initArgs.flags = 0;
        PKCS11 tmpPKCS11 = null;
        long[] slotList = null;
        try {
            try {
                tmpPKCS11 = PKCS11.getInstance(libraryPath, functionList, initArgs, false);
            } catch (IOException ex) {
                String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "getSlotsWithTokens", ex);
                logger.error(errorMessage);
                return null;
            }
        } catch (PKCS11Exception e) {
            try {
                initArgs = null;
                tmpPKCS11 = PKCS11.getInstance(libraryPath, functionList, initArgs, true);
            } catch (IOException ex){
                String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "getSlotsWithTokens", ex);
                logger.error(errorMessage);
                return null;
            } catch (PKCS11Exception ex){
                String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "getSlotsWithTokens", ex);
                logger.error(errorMessage);
                return null;
            }
        }

        try {
            slotList = tmpPKCS11.C_GetSlotList(true);
            logger.info("Number of slots available: " + slotList.length);
            for (long slot : slotList) {
                if(updateToken) {
                    CK_TOKEN_INFO tokenInfo = tmpPKCS11.C_GetTokenInfo(slot);
                    mapKeySlot.put(String.valueOf(tokenInfo.label),slot);

                    logger.info("slot: " + slot + "\nmanufacturerID: "
                            + String.valueOf(tokenInfo.manufacturerID) + "\nmodel: "
                            + String.valueOf(tokenInfo.model)+ "\nLabel: "
                            + String.valueOf(tokenInfo.label));
                }
            }
        } catch (Throwable ex) {
            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "getSlotsWithTokens", (Exception) ex);
            logger.error(errorMessage);
            return null;
        }

        return slotList;
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

    public void storeDocumentForPrinting(InputStream srcStream, String filePath) throws IOException, DocumentException {
        Document document = new Document();
        PdfCopy copy = new PdfSmartCopy(document, new FileOutputStream(new File(filePath)));
        document.open();
        com.itextpdf.text.pdf.PdfReader pdfReader = new com.itextpdf.text.pdf.PdfReader(srcStream);
        copy.addDocument(pdfReader);
        pdfReader.close();
        document.close();
    }

}

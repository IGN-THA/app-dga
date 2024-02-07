package com.docprocess.service;

import com.docprocess.manager.DocumentRenderException;
import com.docprocess.model.SignatureCardData;
import org.codehaus.jettison.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public interface CloudSigningService {
    String getCertValue(InputStream srcStream, String fileName, String pdfPassword, String pdfOwnerPassword, SignatureCardData signatureCardData)throws IOException, DocumentRenderException, JSONException;
}
package com.docprocess.service;

import com.docprocess.manager.DocumentRenderException;
import org.codehaus.jettison.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public interface CloudSigningService {
    String getCertValue(InputStream srcStream, String fileName, String pdfPassword)throws IOException, DocumentRenderException, JSONException;
}
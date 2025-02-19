package com.docprocess.service;

import com.docprocess.manager.DocumentRenderException;
import com.docprocess.model.SignatureCardData;
import jakarta.persistence.EntityManagerFactory;
import org.codehaus.jettison.json.JSONException;

import java.io.IOException;

public interface ApiSigningService {
    byte[] signDocument(byte[] input, String documentSfid, EntityManagerFactory sessionFactory, SignatureCardData signatureCardData) throws IOException, DocumentRenderException, JSONException;
}
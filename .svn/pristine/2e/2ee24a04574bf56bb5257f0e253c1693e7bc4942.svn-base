package com.docprocess.service;

import com.docprocess.manager.DocumentRenderException;
import com.docprocess.model.SignatureCardData;
import org.codehaus.jettison.json.JSONException;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;

public interface ApiSigningService {
    byte[] signDocument(byte[] input, String documentSfid, EntityManagerFactory sessionFactory, SignatureCardData signatureCardData) throws IOException, DocumentRenderException, JSONException;
}
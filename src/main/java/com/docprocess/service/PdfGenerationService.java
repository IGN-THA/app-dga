package com.docprocess.service;

import com.docprocess.manager.DocumentRenderException;
import com.docprocess.pojo.PdfGenerationQueueResponse;
import com.docprocess.pojo.PdfGenerationRequest;
import io.reactivex.rxjava3.core.Single;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public interface PdfGenerationService {
    Single<PdfGenerationQueueResponse> queueRequest(PdfGenerationRequest request);

    File generatePdfFromTemplate(File template, String requestId, LinkedHashMap<String, Object> context, Locale locale, LinkedHashMap<String,Object> localeMap) throws IOException, DocumentRenderException;

    List<File> getPendingTemplate(String requestId);

    void cleanupIntermediaryFolder(String requestId) throws IOException;

    PdfGenerationQueueResponse checkStatus(String requestId);

    File finalizeRenderedFolder(String requestId) throws IOException;

    File finalizeUpdatedFolder(String requestId) throws IOException;

    File getRenderedTemplate(String requestId, String locale);
}

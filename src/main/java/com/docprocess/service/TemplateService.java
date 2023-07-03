package com.docprocess.service;

import io.reactivex.rxjava3.core.Single;

import java.io.File;


public interface TemplateService {
    Single<File> getTemplate(String templateType);
    String getMessageType(String messageType);
}

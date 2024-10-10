package com.docprocess.service;

import com.google.zxing.WriterException;

import java.io.IOException;

public interface QRCodeService {
    byte[] generateQRData(String text, Integer width, Integer height) throws WriterException, IOException;

    String encodeBase64(byte[] data);
}

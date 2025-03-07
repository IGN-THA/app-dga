package com.docprocess.service;

import com.google.zxing.WriterException;

import java.io.IOException;
import java.io.InputStream;

public interface QRCodeService {
    InputStream generateQRCode(String text, int width, int height);

    String encodeBase64(byte[] data);
}

package com.docprocess.service.impl;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.File;
import java.nio.file.Paths;

public class PdfPlayRight {
    private static File htmlFile;
    static String Path;
    public PdfPlayRight(File htmlFile) {
        this.htmlFile = htmlFile;
    }
    public static void set(String absolutePath) {
        Path  = absolutePath;
    }
    public static void get(){
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("file://" + htmlFile.getAbsolutePath());
            page.waitForTimeout(1000);
//            page.pdf(new Page.PdfOptions().setFormat("A4").setPreferCSSPageSize(true).setPath(Paths.get(Path)));
//            page.pdf(new Page.PdfOptions().setFormat("A4").setPath(Paths.get(Path)));
            page.pdf(new Page.PdfOptions().setPreferCSSPageSize(true).setPath(Paths.get(Path)));
            browser.close();
        }
    }
    public static void main(String[] args) throws Exception {
        PdfPlayRight.get();
    }
}
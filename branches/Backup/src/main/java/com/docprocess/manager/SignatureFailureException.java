package com.docprocess.manager;

public class SignatureFailureException extends Exception {
    String msg=null;
    public SignatureFailureException(){

    }

    public SignatureFailureException(String msg){
        super(msg);
    }

    public SignatureFailureException(Exception e, String msg){
        super(e);
        this.msg= msg;
    }

    public String getMessageInHtmlFormat(){
        return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                "                <html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "                    <head>\n" +
                "                        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "                        <title>Notification:  Sign-on to Roojai My account</title>\n" +
                "                        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n" +
                "                   </head>\n" +
                "                    <body style=\"margin:0; padding:0; background:#eeeeee; font-family: Helvetica, Arial, sans-serif !important; font-size: 14px;\">"+
                "Document signing failed due to "+this.getMessage() + ((this.msg==null)?"": this.msg)+
                "</body>\n" +
                "                </html>";
    }

}

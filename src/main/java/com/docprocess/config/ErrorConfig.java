package com.docprocess.config;

public class ErrorConfig {

      public static String getErrorMessages(String className, String methodName, Exception e){
        int lineNumber = -1;
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().equals(className) && element.getMethodName().equals(methodName)) {
                lineNumber = element.getLineNumber();
                break;
            }
        }
        String errorMessage = "Exception occurred in class '" + className + "', method '" + methodName + "', line " + lineNumber + ": " + e.getMessage();
        return errorMessage;
    }
}

package com.docprocess.manager.docx;

import com.docprocess.config.ErrorConfig;
import com.docprocess.manager.DocumentRenderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.ion.Decimal;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;

public class TemplateFormatter {

    static Logger logger = LogManager.getLogger(TemplateFormatter.class);
    public static String[] formatValue(String attr, HashMap<String, Object> variables) throws DocumentRenderException{
        String formattedString[]= new String[3];
        try {
            String formatType = attr.substring(attr.indexOf("_") + 1, attr.indexOf("("));

            switch (formatType) {
                case "NUM":
                    formattedString = formatNumber(attr.substring(attr.indexOf("(") + 1, attr.lastIndexOf(")")), variables);
                    break;
                case "DATE":
                    break;
                case "DATETIME":
                    formattedString = formatDatetime(attr.substring(attr.indexOf("(") + 1, attr.lastIndexOf(")")), variables);
                    break;
                case "CURRENCY":
                    break;
                default:
                    break;
            }
        }catch(Exception e){
            logger.info("Syntax is incorrect for Format function " + attr );
            String errorMessage = ErrorConfig.getErrorMessages(TemplateFormatter.class.getName(), "formatValue", e);
            throw new DocumentRenderException(errorMessage);
        }
        return formattedString;
    }

    public static String[] formatDatetime(String formatNum, HashMap<String, Object> variables) throws DocumentRenderException {
        String[] strArr = new String[3];
        Integer seperator = formatNum.indexOf(",");
        String pattern = "dd-MM-yyyy HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String formatedValue = "";
        try {
            String key = formatNum.substring(0, seperator);
            if(key.indexOf(".")>0){
                strArr[0] = "";
                strArr[1] = key.substring(0, formatNum.indexOf('.'));
                strArr[2] = key.substring(formatNum.indexOf('.') + 1);
                return strArr;
            }
            if(variables.get(formatNum.substring(0, seperator))!=null) {
                Date date = simpleDateFormat.parse(variables.get(formatNum.substring(0, seperator)).toString());
                SimpleDateFormat df = new SimpleDateFormat(formatNum.substring(seperator + 1, formatNum.length()));
                formatedValue = df.format(date);
            }else{
                formatedValue = "";
            }
        } catch (ParseException e) {
            String errorMessage = ErrorConfig.getErrorMessages(TemplateFormatter.class.getName(), "formatDatetime", e);
            logger.error(errorMessage);
        }
        return new String[]{formatedValue,"",""};
    }

    public static String[] formatNumber(String formatNum, HashMap<String, Object> variables){
        String[] strArr = new String[3];
        Integer seperator = formatNum.indexOf(",");
        DecimalFormat df = new DecimalFormat(formatNum.substring(seperator+1, formatNum.length()));
        String key = formatNum.substring(0, seperator);
        if(key.indexOf(".")>0){
            strArr[0] = "";
            strArr[1] = key.substring(0, formatNum.indexOf('.'));
            strArr[2] = key.substring(formatNum.indexOf('.') + 1);
            return strArr;
        }
        Object formatVal = variables.get(formatNum.substring(0, seperator));
        formatVal = (formatVal==null)?"0":formatVal;
        return new String[]{df.format(Decimal.valueOf(formatVal.toString())),"",""};
    }

}

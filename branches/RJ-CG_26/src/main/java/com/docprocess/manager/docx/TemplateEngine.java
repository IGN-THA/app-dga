package com.docprocess.manager.docx;

import com.docprocess.manager.DocumentRenderException;

import java.util.HashMap;
import java.util.Map;

public class TemplateEngine {

    public static Object[] validateCondition(String key, Map<String, ?> mappings) throws DocumentRenderException{
        Object[] element = new Object[2];
        try {

            int startKey = key.indexOf('(');
            if (startKey == -1)
                return element;
            else {
                String beginVal = key.substring(0, startKey);
                int keyEnd = key.indexOf(41, startKey);
                String cond = key.substring(startKey + 1, keyEnd);
                String arrCond[] = cond.split("\\=|>|<|!");
                arrCond[arrCond.length - 1].length();
                String operator = cond.substring(arrCond[0].length(), cond.length() - arrCond[arrCond.length - 1].length());
                Object dynamicVal = mappings.get(arrCond[0].trim());
                dynamicVal = (dynamicVal == null) ? "" : dynamicVal;
                Boolean condMet = validateCriteria(operator, dynamicVal.toString(), arrCond[arrCond.length - 1]);
                element[0] = condMet;
                element[1] = beginVal;
            }
        }catch (Exception e){
            throw new DocumentRenderException("Validation failed due to incorrect syntax "+key);
        }
        return element;
    }

    public static Boolean validateCriteria(String operator, String val, String input) {
        Boolean condMet = false;
        if (operator.equals("=")) {

            condMet = input.equals(val);
        } else if (operator.equals("!=")) {
            condMet = !input.equals(val);
        } else if (operator.equals(">")) {
            condMet = Double.valueOf(input) > Double.valueOf(val);
        } else if (operator.equals("<")) {
            condMet = Double.valueOf(input) < Double.valueOf(val);
        } else if (operator.equals(">=")) {
            condMet = Double.valueOf(input) >= Double.valueOf(val);
        } else if (operator.equals("<=")) {
            condMet = Double.valueOf(input) <= Double.valueOf(val);
        }

        return condMet;
    }

    public static HashMap getListDetailsFromKey(String key, HashMap<String, Object> variables) throws DocumentRenderException{
        HashMap<String, String> listViewMap = new HashMap<String, String>();
        try {
            String tableName = key.substring(key.indexOf("_") + 1, key.indexOf("("));
            String whereClauseOrg = key.substring(key.indexOf("(") + 1, key.indexOf(","));
            String keyName = key.substring(key.indexOf(",") + 1, key.indexOf(")"));

            listViewMap.put("tableName", "fmsapp." + tableName);
            listViewMap.put("keyName", keyName);


            while (whereClauseOrg.indexOf("<!") > 0) {
                String dynaValParam = whereClauseOrg.substring(whereClauseOrg.indexOf("<!") + 2, whereClauseOrg.indexOf(">"));
                String dynaVal = (variables.get(dynaValParam) != null) ? variables.get(dynaValParam).toString() : "";
                dynaVal = "'" + dynaVal + "'";
                whereClauseOrg = whereClauseOrg.replace("<!" + dynaValParam + ">", dynaVal);
            }
            listViewMap.put("whereClause", whereClauseOrg);
        }catch(Exception e){
            throw new DocumentRenderException("List syntax is incorrect for "+key);
        }
        return listViewMap;
    }
//
//    public static final String covertToSQLParam(Object obj){
//
//    }
}

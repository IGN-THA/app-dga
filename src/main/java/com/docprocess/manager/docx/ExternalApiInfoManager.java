package com.docprocess.manager.docx;

import com.docprocess.config.ErrorConfig;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.manager.HttpHandler;
import com.docprocess.model.ExternalApiInfo;
import com.docprocess.repository.ExternalApiInfoRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ExternalApiInfoManager {

    static Logger logger = LogManager.getLogger(ExternalApiInfoManager.class);

    private static final String ODATAACC = "ODATAACC";

    public static JSONObject extData(String[] keys, HashMap<String, Object> variables, HashMap<String, JSONObject> keyMap, ExternalApiInfoRepository repository) {

        if (keys != null) {
            JSONObject jsonBody = new JSONObject();
            HttpHandler handler = new HttpHandler();
            Map<String, String> headers = new HashMap<String, String>();
            JSONObject response = null;
            try {
                Object object = variables.get(keys[1]);
                String keyId = object.toString();
                String mapKey = keys[0] + "_" + keyId;
                if(keyMap.containsKey(mapKey)){
                    response = keyMap.get(mapKey);
                    return response;
                }
                ExternalApiInfo apiInfo = repository.findByApiKey(keys[0]).get();
                if (apiInfo != null) {
                    if(keyId != null){
                        if(apiInfo.getApiKey().equalsIgnoreCase(ODATAACC)){
                            jsonBody.put("accountExtId", keyId);
                        }else {
                            jsonBody.put("assetId", keyId);
                        }

                        headers.put("Content-Type", "application/json; charset=UTF-8");
                        headers.put("x-api-key", apiInfo.getSecurityKey());
                        response = new JSONObject(handler.callRestAPI(jsonBody.toString(), apiInfo.getEndpoint(), headers, null));
                        keyMap.put(mapKey, response);
                        return response;
                    }
                }
            } catch (Exception e) {

                String errorMessage = ErrorConfig.getErrorMessages(ExternalApiInfoManager.class.getSimpleName(), "extData", e);
                logger.error(errorMessage);
            }
        }

        return null;
    }

}

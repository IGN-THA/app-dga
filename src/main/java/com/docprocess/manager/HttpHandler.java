package com.docprocess.manager;

import com.docprocess.config.ConfigConstant;
import com.docprocess.config.ErrorConfig;
import com.docprocess.repository.SystemConfigRepository;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HttpHandler {

    @Autowired
    SystemConfigRepository systemConfigRepository;

    Logger logger = LogManager.getLogger(HttpHandler.class);
    public Boolean notifySalesforce(JSONObject jsonBody,String resourceEndPoint) {

        String responseMsg = "Success";

        //HttpHandler handler = new HttpHandler();
        Map<String, String> headers = new HashMap<String, String>();
        Map<String, String> params = new HashMap<String, String>();
        Boolean apiSuccess = true;
        try {
            JSONObject response = null;
            JSONObject tokenCache = CacheManager.getTokenCache();
            String tokenType;
            String accessToken;
            String instanceURL;
            if (tokenCache == null || ((System.currentTimeMillis() - tokenCache.getLong("expiryTime")) >= 300000)) {

                String userName = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_USERNAME).getConfigValue();
                String password = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_PASSWORD).getConfigValue();
                String grantType = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_GRANT_TYPE).getConfigValue();
                String clientId = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_CLIENT_ID).getConfigValue();
                String clientSecret = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_CLIENT_SECRET).getConfigValue();
                String tokenURL = systemConfigRepository.findByConfigKey(ConfigConstant.SALESFORCE_TOKEN_URL).getConfigValue();
                params.put("username", userName);
                params.put("password", password);
                params.put("grant_type", grantType);
                params.put("client_id", clientId);
                params.put("client_secret", clientSecret);
                headers.put("Content-Type", "application/json; charset=UTF-8");

                response = new JSONObject(callRestAPI(null, tokenURL, headers, params));
                tokenType = response.getString("token_type");
                accessToken = response.getString("access_token");
                instanceURL = response.getString("instance_url");
                CacheManager.updateCache(tokenType, accessToken, instanceURL);
                tokenCache = CacheManager.getTokenCache();
            }

            tokenType = tokenCache.getString("token_type");
            accessToken = tokenCache.getString("access_token");
            instanceURL = tokenCache.getString("instance_url");


            headers = new HashMap<>();
            headers.put("Authorization", tokenType + " " + accessToken);
            headers.put("Content-Type", "application/json; charset=UTF-8");
            response = new JSONObject(callRestAPI(jsonBody.toString(), instanceURL + resourceEndPoint, headers, null));
            apiSuccess = Boolean.valueOf(response.get("success").toString());
        } catch (JSONException e) {
            responseMsg = "Fail: e.getMessage()";
            apiSuccess = false;
            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "notifySalesforce", e);
            logger.error(errorMessage);
        }
        return apiSuccess;
    }

    public String callRestAPI(String requestBody, String callOutURL, Map<String, String> headers, Map<String, String> queryParam) {
        JSONObject json = null;
        String httpResponse="";
        //System.out.println("requestBody "+ requestBody);
        //System.out.println("callOutURL "+callOutURL);
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            URIBuilder builder = new URIBuilder(callOutURL);
            if (queryParam != null && !queryParam.isEmpty()) {
                List<NameValuePair> qparams = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry : queryParam.entrySet()) {
                    qparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                builder.setParameters(qparams);
            }
            HttpPost postRequest = new HttpPost(builder.build());
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    postRequest.addHeader(entry.getKey(), entry.getValue());
                }
            }

            if (requestBody != null && requestBody.length() > 0) {
                postRequest.setEntity(new StringEntity(requestBody, "UTF-8"));
            }

            HttpResponse response = httpClient.execute(postRequest);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                logger.info("Http status error "+statusCode);
                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
            }
            HttpEntity entity = response.getEntity();
            httpResponse = EntityUtils.toString(entity);
            //json = new JSONObject(content);
        } catch (Exception e) {
            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "callRestAPI", e);
            logger.error(errorMessage);
        }

        return httpResponse;
    }
}

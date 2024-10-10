package com.docprocess.manager;

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
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpHandler {

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
                postRequest.setEntity(new StringEntity(requestBody));
            }

            HttpResponse response = httpClient.execute(postRequest);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println("Http status error "+statusCode);
                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
            }
            HttpEntity entity = response.getEntity();
            httpResponse = EntityUtils.toString(entity);
            //json = new JSONObject(content);
        } catch (Exception e) {
            System.out.println("error>> "+e.getMessage());
        }

        return httpResponse;
    }
}

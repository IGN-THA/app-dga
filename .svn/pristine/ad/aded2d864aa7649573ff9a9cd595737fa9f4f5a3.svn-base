package com.docprocess.manager;

import com.docprocess.config.ErrorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public class CacheManager {
    static JSONObject tokenCache = null;
    static JSONObject tokenFmsAppCache = null;

    static Logger logger = LogManager.getLogger(CacheManager.class);
    public static void updateCache(String tokenType, String accessToken, String instanceUrl) {
        tokenCache = new JSONObject();
        try {
            tokenCache.put("token_type", tokenType);
            tokenCache.put("access_token", accessToken);
            tokenCache.put("instance_url", instanceUrl);
            tokenCache.put("expiryTime", System.currentTimeMillis());
            evictAllCacheValues();
        } catch (JSONException e) {
            String errorMessage = ErrorConfig.getErrorMessages(CacheManager.class.getName(), "updateCache", e);
            logger.error(errorMessage);
        }
    }

    public static void updateFmaAppCache(String tokenType, String accessToken) {
        tokenFmsAppCache = new JSONObject();
        try {
            tokenFmsAppCache.put("token_type", tokenType);
            tokenFmsAppCache.put("access_token", accessToken);
            tokenFmsAppCache.put("expiryTime", System.currentTimeMillis());

            evictFmsAppCacheValues();
        } catch (JSONException e) {

            String errorMessage = ErrorConfig.getErrorMessages(CacheManager.class.getName(), "updateFmaAppCache", e);
            logger.error(errorMessage);
        }
    }

    @Cacheable(value = "SalesforceToken")
    public static JSONObject getTokenCache() throws JSONException {
        return tokenCache;
    }

    @CacheEvict(value = "SalesforceToken", allEntries = true)
    public static void evictAllCacheValues() {}

    @Cacheable(value = "FmsAppToken")
    public static JSONObject getFmsAppTokenCache() throws JSONException {
        return tokenFmsAppCache;
    }

    @CacheEvict(value = "FmsAppToken", allEntries = true)
    public static void evictFmsAppCacheValues() {}
}

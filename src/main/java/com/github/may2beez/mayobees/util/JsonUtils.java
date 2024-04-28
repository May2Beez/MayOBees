package com.github.may2beez.mayobees.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JsonUtils {
    private static final Gson gson = new Gson();

    private static final JsonParser parser = new JsonParser();
    public static List<String> getListFromUrl(String url, String name) {
        List<String> ret = new ArrayList<>();
        try {
            JsonObject json = getContent(url);
            if (json != null) {
                json.getAsJsonArray(name).forEach(je -> ret.add(je.getAsString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static JsonObject getContent(String url) {
        try {
            HttpGet request = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000)
                    .setSocketTimeout(5000)
                    .setExpectContinueEnabled(false)
                    .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                    .setProxyPreferredAuthSchemes(Collections.singletonList(AuthSchemes.BASIC))
                    .build();
            request.setConfig(requestConfig);
            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(request);
            InputStream inputStream = response.getEntity().getContent();
            InputStreamReader reader = new InputStreamReader(inputStream, UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
package com.darktornado.library;
  
/*
Simple Requester
© 2020 Dark Tornado, All rights reserved.
*/

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class SimpleRequester {
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    private String url;
    private StringBuilder params;
    private HashMap<String, String> header;
    private String method = METHOD_GET;
    private int timeout = 5000;
    private String encode1 = "UTF-8";
    private String encode2 = "UTF-8";
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36";

    public SimpleRequester() {
        header = new HashMap<>();
    }

    public SimpleRequester(String url) {
        this();
        this.url = url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setEncoding(String encoding) {
        this.encode1 = encoding;
        this.encode2 = encoding;
    }

    public void setEncoding(String request, String response) {
        this.encode1 = request;
        this.encode2 = response;
    }

    public void addHeader(String key, String value) {
        header.put(key, value);
    }

    public void addData(String key, String value) {
        if (params == null) {
            params = new StringBuilder(key + "=" + value);
        } else {
            params.append("&" + key + "=" + value);
        }
    }

    public static SimpleRequester create(String url) {
        return new SimpleRequester(url);
    }

    public SimpleRequester method(String method) {
        this.method = method;
        return this;
    }

    public SimpleRequester header(String key, String value) {
        header.put(key, value);
        return this;
    }

    public SimpleRequester userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public SimpleRequester encoding(String encoding) {
        this.encode1 = encoding;
        this.encode2 = encoding;
        return this;
    }

    public SimpleRequester encoding(String request, String response) {
        this.encode1 = request;
        this.encode2 = response;
        return this;
    }

    public SimpleRequester data(String key, String value) {
        if (params == null) {
            params = new StringBuilder(key + "=" + value);
        } else {
            params.append("&" + key + "=" + value);
        }
        return this;
    }

    public void timeout(int timeout) {
        this.timeout = timeout;
    }

    public Response execute() throws IOException {
        if (this.url == null) {
            throw new NullPointerException("Url is null. Please set url before execute request");
        }
        URL url;
        if (method.equals(METHOD_POST) || params == null) url = new URL(this.url);
        else url = new URL(this.url + "?" + params.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setConnectTimeout(timeout);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("User-Agent", userAgent);
        con.setRequestProperty("Accept-Charset", encode1);
        for (String key : header.keySet()) {
            con.setRequestProperty(key, header.get(key));
        }
        if (method.equals(METHOD_POST)) {
            DataOutputStream dos = new DataOutputStream(con.getOutputStream());
            dos.writeBytes(params.toString());
            dos.flush();
            dos.close();
        }

        DataInputStream dis = new DataInputStream(con.getInputStream());
        InputStreamReader isr = new InputStreamReader(dis, encode2);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder str = new StringBuilder(br.readLine());
        String line;
        while ((line = br.readLine()) != null) {
            str.append("\n").append(line);
        }
        br.close();
        isr.close();
        dis.close();
        return new Response(con.getResponseCode(), str.toString());
    }

    public static class Response {
        public int responseCode;
        public String body;
        public String params;

        public Response(int code, String body) {
            responseCode = code;
            this.body = body;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getResponseBody() {
            return body;
        }

        @Override
        public String toString() {
            return body;
        }
    }

}


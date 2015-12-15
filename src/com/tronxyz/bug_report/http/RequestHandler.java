package com.tronxyz.bug_report.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import android.util.Log;

import com.tronxyz.bug_report.Constants;
import com.tronxyz.bug_report.http.Response.HTTP_STATUS_CODES;



public class RequestHandler {
    static final String tag = "BugReportRequestHandler";
    static final int DEFAULT_CONN_TIMEOUT = 10 * 1000;
    static final int DEFAULT_SO_TIMEOUT = 30 * 1000;
    private DefaultHttpClient client = null;
    //private CookieStore myCookieStore = null;
    public interface Callback {
        boolean handleResponse(Response resp);
    }

    private volatile static RequestHandler handler;

    public static RequestHandler getInstance() {
        if (handler == null)
            handler = new RequestHandler();
        return handler;
    }

    private RequestHandler() {
        HttpParams httpParameters = new BasicHttpParams();
        //httpParameters.setLongParameter(ConnManagerPNames.TIMEOUT, DEFAULT_CONN_TIMEOUT);
        HttpConnectionParams.setConnectionTimeout(httpParameters, DEFAULT_CONN_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParameters, DEFAULT_SO_TIMEOUT);

        SchemeRegistry sr = new SchemeRegistry();
        sr.register(new Scheme("http", new PlainSocketFactory(), 80));
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        sr.register(new Scheme("https", socketFactory, 443));

        ClientConnectionManager connMgr = new ThreadSafeClientConnManager(httpParameters, sr);
        client = new DefaultHttpClient(connMgr, httpParameters);
        //myCookieStore = new BasicCookieStore();
    }

    /**
     * processRequest - starts a web service request, when finished the
     * callback will be called
     *
     * @param req
     *            - the request to start
     * @param callback
     *            - callback which will be called when response is available
     */
    public void processRequest(Request req, Callback callback) {
        new Thread(new Transaction(req, callback)).start();
    }

    /**
     * processRequest - starts a web service synchronous request
     *
     * @param req - the request to start
     */
    public synchronized Response processRequest(Request req){
        return doHttpRequest(req);
    }

    private class Transaction implements Runnable {
        private Request request;
        private Callback callback;

        public Transaction(Request req, Callback callback) {
            this.request = req;
            this.callback = callback;
        }

        public void run() {
            if(request == null)
                return;
            Response resp = null;
            try {
                resp = doHttpRequest(request);
            } finally {
                if (callback != null){
                    if(resp == null){
                        Log.d(tag, String.format("Got null response for request : %d", request.getId()));
                        resp = request.createResponse(HTTP_STATUS_CODES.NONE, (JSONObject)null);
                    }
                    callback.handleResponse(resp);
                }
            }
        }
    }
    private Response doHttpRequest(Request req) {
        HttpResponse response = null;
        HttpRequestBase httpRequest = null;
        HttpEntity entity = null;
        Header errorCode = null;
        Header msg = null;
        JSONObject jsonData = null;
        int responseCode = 0;

        try {
            HttpContext httpContext = new BasicHttpContext();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, Constants.getCookieStore());
            if (req.PostData != null) {
                Log.d(tag, new StringBuilder("----httpPost----").toString());
                httpRequest = new HttpPost(req.getUrl());
                ((HttpPost) httpRequest).setEntity(new StringEntity(req.PostData, "utf-8"));

            }  else {
                Log.d(tag, new StringBuilder("----httpGet----").toString());
                httpRequest = new HttpGet(req.getUrl());
            }

            httpRequest.setHeader("User-Agent", req.getUserAgent());
            httpRequest.setHeader("Accept-Encoding", "gzip");
            httpRequest.setHeader("Accept-Language", Locale.getDefault().getLanguage());


            response = client.execute(httpRequest, httpContext);
            entity = response.getEntity();

            responseCode = response.getStatusLine().getStatusCode();
            Log.d(tag, new StringBuilder("----responseCode----: ").append(responseCode).toString());

            InputStream stream = null;

            if (responseCode == HttpURLConnection.HTTP_OK) {
                errorCode = response.getFirstHeader("RetCode");
                msg = response.getFirstHeader("RetMsg");
/*
                String sign = errorCode.getValue().toLowerCase();

                if (sign.endsWith("e")) {
                    Log.d(tag, new StringBuilder("----cloud error---- ").append(errorCode).append(msg).toString());
                }
*/
                if (entity != null) {
                    stream = entity.getContent();
                    if (stream != null) {
                        Log.d(tag, new StringBuilder("----Response message----: ").append(stream).toString());
                        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

                        try {
                            Header encoding = entity.getContentEncoding();

                            if (encoding != null && "gzip".equalsIgnoreCase(encoding.getValue())) {
                                stream = new GZIPInputStream(stream);
                            }

                            String outputStream = null;
                            byte[] buff = new byte[1024];
                            int count = -1;

                            while ((count = stream.read(buff, 0, 1024)) != -1) {
                                outStream.write(buff, 0, count);
                            }

                            outputStream = new String(outStream.toByteArray(), HTTP.UTF_8);
                            Log.d(tag," RequestHandler.java  doHttpRequest() outputStream="+outputStream);
                            if(outputStream != null && !outputStream.trim().equals(""))
                            jsonData = new JSONObject(outputStream);
                        }finally {
                            stream.close();
                            outStream.close();
                            Log.d(tag, new StringBuilder("stream.close() and outStream.close()").toString());
                        }
                    }
                }
            }
            Log.d(tag, new StringBuilder("Successed to execute http request").toString());
            return req.createResponse(responseCode, jsonData);

        } catch (Exception e) {
            Log.e(tag, "exception when doing http request", e);
            Response resp = req.createResponse(HTTP_STATUS_CODES.NONE, jsonData);
            resp.setException(e);
            return resp;
        } finally {
           if (entity != null) {
                try {
                    entity.consumeContent();
                } catch (IOException e) {
                    Log.e(tag, "IOException error", e);
                }
            }
        }
    }
}

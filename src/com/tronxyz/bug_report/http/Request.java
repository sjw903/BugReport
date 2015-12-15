package com.tronxyz.bug_report.http;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.json.JSONObject;

import android.content.Context;

public class Request {
    //how many times we've retried this request
    protected byte mRetryCount = 0;
    //max number of times we'll retry the request, negative number means infinite
    protected byte mMaxRetries = 0;
    protected byte[] mData = null;
    protected long id; // correlate with the response
    private Context context;
    private String mUrl;
    private Map<String, Object> httpParams;
    public String PostData;

    public Request(Context context, String url, byte[] data) {
        this(System.currentTimeMillis(), context, url, data);
    }

    public Request(long id, Context context, String url, byte[] data) {
        this.id = id;
        this.context = context;
        this.mData = data;
        this.mUrl = url;
        this.httpParams = new HashMap<String, Object>();
    }

    public boolean shouldRetry() {
        return mMaxRetries < 0 || mRetryCount < mMaxRetries;
    }

    public void upRetryCount() {
        mRetryCount++;
    }

    public HttpEntity getRequestEntity() {
        ByteArrayEntity reqEntity = new ByteArrayEntity(mData);
        reqEntity.setContentType("application/octet-stream");
        return reqEntity;
    }

    /**
     * hasData() - used to determine if this request is a GET or a POST.
     *
     * @return true - request will be treated as a POST
     * @return false - request will be treated as a GET
     */
    public boolean hasData() {
        return (mData != null && mData.length > 0);
    }

    /**
     * isSecure() - whether this request needs to be done over SSL
     *
     * @return true - request will use SSL
     * @return false - request will not use SSL
     */
    public boolean isSecure() {
        return false;
    }

    /**
     * getBodySize() - returns the size of the body so we can put that in the
     * query string params
     *
     * @return int - size of body in bytes
     */
    public long getBodySize() {
        return mData != null ? mData.length : 0;
    }

    public Context getContext() {
        return context;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(StringBuffer value) {
        mUrl=value.insert(0, mUrl).toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setHttpParams(Map<String, Object> params) {
        if(params != null && !params.isEmpty())
            httpParams.putAll(params);
    }

    public void setHttpParam(String key, Object value) {
        httpParams.put(key, value);
    }

    public Map<String, Object> getHttpParams() {
        return httpParams;
    }

    public Response createResponse(int statusCode, byte[] data) {
        return new Response(getId(), statusCode, data);
    }

    public Response createResponse(int statusCode, JSONObject data) {
        return new Response(getId(), statusCode, data);
    }

    public String getUserAgent()
    {
        return "Mozilla/5.0 (iPhone; U; CPU iPhone OS 3_0 like Mac OS X; en-us) AppleWebKit/528.18 " +
                  "(KHTML, like Gecko) Version/4.0 Mobile/7A341 Safari/528.16";
    }
}

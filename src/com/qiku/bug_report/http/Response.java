package com.qiku.bug_report.http;

import org.json.JSONObject;

public class Response {
    protected long id;
    protected byte[] mData = null;
    protected JSONObject mJSONData = null;
    protected int mStatusCode;
    protected ErrorCode errorCode = ErrorCode.None;
    protected Throwable error;

    public enum ErrorCode {
        None, BadRequestError, InternalError, UnknownError;
    }

    public static class HTTP_STATUS_CODES {
        public static final int NONE = 0;
        public static final int OK = 200;
        public static final int ACCEPTED = 202;
        public static final int BAD_REQUEST = 400;
        public static final int FORBIDDEN = 403;
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int BAD_GATEWAY = 502;
        public static final int SERVICE_UNAVAILABLE = 503;
    }

    public Response(int statusCode, byte[] data) {
        this(0, statusCode, data);
    }

    public Response(int statusCode, JSONObject data) {
        this(0, statusCode, data);
    }

    public Response(long id, int statusCode, byte[] data) {
        this.id = id;
        mData = data;
        mStatusCode = statusCode;
        switch (mStatusCode) {
        case HTTP_STATUS_CODES.OK:
        case HTTP_STATUS_CODES.ACCEPTED:
            errorCode = ErrorCode.None;
            break;
        case HTTP_STATUS_CODES.BAD_REQUEST:
        case HTTP_STATUS_CODES.FORBIDDEN:
            errorCode = ErrorCode.BadRequestError;
            break;
        case HTTP_STATUS_CODES.INTERNAL_SERVER_ERROR:
        case HTTP_STATUS_CODES.BAD_GATEWAY:
        case HTTP_STATUS_CODES.SERVICE_UNAVAILABLE:
            errorCode = ErrorCode.InternalError;
            break;
        case HTTP_STATUS_CODES.NONE:
        default:
            errorCode = ErrorCode.UnknownError;
        }
    }

    public Response(long id, int statusCode, JSONObject data) {
        this.id = id;
        mJSONData = data;
        mStatusCode = statusCode;
        switch (mStatusCode) {
        case HTTP_STATUS_CODES.OK:
        case HTTP_STATUS_CODES.ACCEPTED:
            errorCode = ErrorCode.None;
            break;
        case HTTP_STATUS_CODES.BAD_REQUEST:
        case HTTP_STATUS_CODES.FORBIDDEN:
            errorCode = ErrorCode.BadRequestError;
            break;
        case HTTP_STATUS_CODES.INTERNAL_SERVER_ERROR:
        case HTTP_STATUS_CODES.BAD_GATEWAY:
        case HTTP_STATUS_CODES.SERVICE_UNAVAILABLE:
            errorCode = ErrorCode.InternalError;
            break;
        case HTTP_STATUS_CODES.NONE:
        default:
            errorCode = ErrorCode.UnknownError;
        }
    }
    public int getStatusCode() {
        return mStatusCode;
    }

    public Throwable getException() {
        return error;
    }

    public void setException(Throwable error) {
        this.error = error;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public byte[] getData() {
        return mData;
    }

    public JSONObject getJSONData() {
        return mJSONData;
    }

    public String getAppError() {
        return null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}

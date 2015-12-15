
package com.tronxyz.bug_report.http.upload;

import android.content.Context;
import android.util.Log;

import com.tronxyz.bug_report.http.RequestHandler;
import com.tronxyz.bug_report.http.Response;
import com.tronxyz.bug_report.http.upload.GusWS.FileListResponse;
import com.tronxyz.bug_report.http.upload.GusWS.GetSarUrlResponse;
import com.tronxyz.bug_report.http.upload.GusWS.GusResponse;
import com.tronxyz.bug_report.http.upload.GusWS.LogonResponse;
import com.tronxyz.bug_report.helper.Util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.PrintWriter;

@SuppressWarnings("unused")
public class GUS implements RequestHandler.Callback {

    static private final String LOG_TAG = "BugReportGUS";

    public static enum ReturnCode {
        SUCCESS, UNAUTHORIZED, BAD_REQUEST, UPLOAD_DELETED, NETWORK_DISCONNECTED, BATTERY_LOW, UPLOAD_PAUSED, SERVER_ERROR;
    }

    private final ConcurrentHashMap<Long, GusJob> mJobs = new ConcurrentHashMap<Long, GusJob>();
    private final Context mParent;
    private String mLogonUrl;
    private String mLogoffUrl;
    private String mFileListUrl;
    private String mFileUploadUrl;
    private String mFolderCreateUrl;
    private String mGetIdUrl;
    private String mUploadUrl;
    private String mResumeUrl;
    private JSONObject reportInfo;
    private boolean mIsAPRUpload;
    private float mUpTime;
    private float mElaspedTime;
    private float mDeltaUpTime;
    private float mDeltaElaspedTime;

    public GUS(Context parent, String getIdUrl, String uploadUrl,
            String resumeUrl, String logonUrl, String logoffUrl,
            String filelistUrl, String fileuploadUrl, String foldercreateUrl) {
        mParent = parent;
        mGetIdUrl = getIdUrl;
        mUploadUrl = uploadUrl;
        mResumeUrl = resumeUrl;
        mLogonUrl = logonUrl;
        mLogoffUrl = logoffUrl;
        mFileListUrl = filelistUrl;
        mFileUploadUrl = fileuploadUrl;
        mFolderCreateUrl = foldercreateUrl;
    }

    public void shutdown() {
        Log.i(LOG_TAG,
                "shutdown(): cancelling jobs I have left: " + mJobs.size());
        for (Map.Entry<Long, GusJob> entry : mJobs.entrySet()) {
            stop(entry.getKey());
        }
    }

    public long start(GusJob job, JSONObject ri) {
        reportInfo = ri;
        if(reportInfo == null){
            mIsAPRUpload = true;
        }else{
            mIsAPRUpload = false;
        }
        if (job.mState != GusJob.State.Idle) {
            Log.e(LOG_TAG, new StringBuilder("start(): job: ").append(job)
                    .append(" isn't in the correct state: ").append(job.mState)
                    .append(" resetting...").toString());
            job.mState = GusJob.State.Idle;
        }
        mJobs.put(job.mGusId, job);
        Log.i(LOG_TAG, "start(): starting job: " + job);
        runStateMachine(job, null);
        return job.mGusId;
    }

    public boolean stop(long id, ReturnCode reason) {
        if (mJobs.containsKey(id)) {
            Log.i(LOG_TAG, "stop(): cancelling job " + id);
            GusJob j = mJobs.get(id);
            if (j.mRequest != null) {
                j.mRequest.cancel();
            } else {
                Log.e(LOG_TAG,
                        new StringBuilder("stop(): can't cancel request ")
                                .append(id)
                                .append(" because the request is null, that's weird...")
                                .toString());
            }
            if (reason == null)
                jobIsDone(j, ReturnCode.UPLOAD_DELETED);
            else
                jobIsDone(j, reason);
            return true;
        } else {
            Log.e(LOG_TAG,
                    new StringBuilder("stop(): can't stop job ").append(id)
                            .append(" because I don't know aboot him...")
                            .toString());
            return false;
        }
    }

    public boolean stop(long id) {
        return stop(id, null);
    }

    public boolean handleResponse(Response resp) {
        Log.d(LOG_TAG," *** GUS.java  handleResponse()" + resp.getStatusCode());
        GusResponse gResp = (GusResponse) resp;
        GusJob j = gResp.getJob();
        try {
            // First check to see if this job's been cancelled. If so then drop
            // him to the floor
            if (!mJobs.containsKey(j.mGusId)) {
                Log.i(LOG_TAG,
                        new StringBuilder("handleResponse(): job: ")
                                .append(j.mGusId)
                                .append(" seems to have been cancelled - dropping this response to the floor")
                                .toString());
                return false;
            }

            ReturnCode code = gResp.getReturnCode();
            Log.d(LOG_TAG,"&&&&&  GUS.java  handleResponse()   gResp.getReturnCode()="+gResp.getReturnCode());
            if (code != ReturnCode.SUCCESS) {
                String msg = new StringBuilder("handleResponse(): job: ")
                        .append(j.mGusId).append(" state: ").append(j.mState)
                        .append(" got an error: ").append(code).toString();
                Log.e(LOG_TAG, msg, resp.getException());
                if (j.shouldRetry()) {
                    Log.i(LOG_TAG, new StringBuilder(
                            "handleResponse(): Retrying job: ")
                            .append(j.mGusId).toString());
                    j.upRetryCount();
                    j.mState = GusJob.State.Idle;
                    runStateMachine(j, null);
                } else
                    jobIsDone(j, code);
            } else {
                runStateMachine(j, resp);
            }
        } catch (Error e) {
            Log.e(LOG_TAG, "Unexcepted error occured while handling response.",
                    e);
            jobIsDone(j, ReturnCode.SERVER_ERROR);
        }
        return true;
    }

    private void runStateMachine(GusJob j, Response r) {
        switch (j.mState) {
            case Idle:
                Log.d(LOG_TAG,"GUS.java  runStateMachine()  Idle ...");
                // Before we do anything we need to login the server.
                startLogon(j);
                break;
            case Logon:
                Log.d(LOG_TAG,"GUS.java  runStateMachine()   Logon ...");
                handleLogonResp(j, (LogonResponse) r);
                break;
            case Logoff:
                Log.d(LOG_TAG,"GUS.java  runStateMachine()   LogOff ...");
                handleLogoffResp();
                break;
            case FileList:
                Log.d(LOG_TAG,"GUS.java  runStateMachine()  FileList ...");
                handleFileListResponse(j, (FileListResponse) r);
                break;
            case GetSarUrl:
                Log.d(LOG_TAG,"GUS.java  runStateMachine()   GetSarUrl ...");
                handleGetSarUrlResponse(j, (GetSarUrlResponse) r);
                break;
            case FileUpload:
                Log.d(LOG_TAG,"GUS.java  runStateMachine()   FileUpload ...");
                // handleFileUploadResponse(j, (FileUploadResponse) r);
                break;
            case FolderCreate:
                Log.d(LOG_TAG," FolderCreate ...");
                // handleFolderCreateResponse(j, (FolderCreateResponse) r);
                break;
            default:
                break;
        }
    }

    private void handleLogonResp(GusJob j, LogonResponse resp) {

        resp.getJSONData();
        if(!mIsAPRUpload){
            Log.d(LOG_TAG,
                new StringBuilder("handleLogonResp(): ").append(
                        "sucess and trying to startFileList()").toString());
            startFileList(j);
        }else{
            Log.d(LOG_TAG, " This is a APR upload..., will logoff");
            resetFile(j.mUploadData.mFilePath);
            startLogoff(j);
        }
    }

    private void handleLogoffResp() {
        Log.d(LOG_TAG,
                new StringBuilder("handleLogoffResp(): ").append("sucess.")
                        .toString());
    }

    private void handleFileListResponse(GusJob j, FileListResponse resp) {
        resp.getJSONData();
        Log.d(LOG_TAG,
                new StringBuilder("handleFileListResponse(): ").append(
                        "sucess get file id and try to get sar url.")
                        .toString());
        startGetSarUrl(j);
    }

    private void handleGetSarUrlResponse(GusJob j, GetSarUrlResponse resp) {
        resp.getJSONData();
        Log.d(LOG_TAG,
                new StringBuilder("handleGetSarUrlResponse(): ").append(
                        "sucess get sar url and uploading file...").toString());

        if (uploadFile(j)) {
            Log.d(LOG_TAG, new StringBuilder("handleGetSarUrlResponse(): ")
                    .append("sucess uploaded bug file!").toString());
            jobIsDone(j, ReturnCode.SUCCESS);
        } else {
            Log.d(LOG_TAG, new StringBuilder("handleGetSarUrlResponse(): ")
                    .append("failed uploaded bug file! process as ReturnCode.SERVER_ERROR").toString());
            jobIsDone(j, ReturnCode.SERVER_ERROR);
        }
        startLogoff(j);
    }

    private boolean uploadFile(GusJob j) {
        HttpURLConnection conn = null;
        File uploadFile = new File(j.mUploadData.mFilePath);
/*
        if (j.mUploadIp == null || j.mUploadIp.isEmpty()) // The File is already
                                                          // uploaded
            return true;
        if (j.mNeedUpload == false) {
            Log.i(LOG_TAG, "no need to upload this file");
            return true;
        }
*/
        String uploadurl = null;
        String req_params = null;

        //uploadurl = "http://" + j.mUploadIp + "/bpupload/" + j.mBucketName
        //        + "/" + uploadFile.getName();

        uploadurl = "http://" + j.mUploadIp;

        Log.d(LOG_TAG," *** uploadurl = " + uploadurl);

        if(reportInfo != null)
            req_params = reportInfo.toString();
/*
        req_params = "userId*" + j.mUserId + ",objectSize*"
                + uploadFile.length() + ",objectSha1*" + j.mSha1
                + ",requestId*" + j.mRequestId + ",salAccessKeyId*"
                + j.mSalAccessKeyId + ",policy*private" + ",callbackUri*"
                + j.mCallbackUri + ",range*" + j.mRange + "-";
*/
        Log.d(LOG_TAG, new StringBuilder("uploadFile(): ").append("uploadurl")
                .append(uploadurl).append("/").append(req_params).toString());

        conn = null;

        DataOutputStream dos = null;
        // DataInputStream inStream = null;

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "----------------sdjfdhfjdsggdsdhsshgffhjdfdfdfjdgdwqwg";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 64 * 1024; // old value 1024*1024

        boolean isSuccess = true;
        try {

            // ------------------ CLIENT REQUEST
            FileInputStream fileInputStream = null;
            // Log.i("FNF","UploadService Runnable: 1");
            try {
                fileInputStream = new FileInputStream(uploadFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                // Log.e(LogUtil.TAG, "file not found");
            }
            // open a URL connection to the Servlet
            URL url = new URL(uploadurl);
            // Open a HTTP connection to the URL
            conn = (HttpURLConnection) url.openConnection();
            // Allow Inputs
            conn.setDoInput(true);
            // Allow Outputs
            conn.setDoOutput(true);
            // Don't use a cached copy.
            conn.setUseCaches(false);
            // set timeout
            conn.setConnectTimeout(60000);
            conn.setReadTimeout(60000);
            // Use a post method.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);

            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(lineEnd + twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"req_params\""
                    + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(req_params + lineEnd);

            dos.writeBytes(lineEnd + twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\""
                    + uploadFile.getName() + "\"; filename=\""
                    + uploadFile.getName() + "\"" + lineEnd);
            dos.writeBytes("Content-Type:" + "application/octet-stream"
                    + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            fileInputStream.skip(j.mRange);
            Log.i(LOG_TAG, "ready to dos write, range is " + j.mRange);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "UploadService:Client Request error", e);
            isSuccess = false;
        } catch (IOException e) {
            Log.e(LOG_TAG, "UploadService Runnable:Client Request error", e);
            isSuccess = false;
        }
        // ------------------ read the SERVER RESPONSE
        try {
            Log.d(LOG_TAG, new StringBuilder("uploadFile(), response code =")
                    .append(conn.getResponseCode()).toString());
            if (conn.getResponseCode() != 200) {
                isSuccess = false;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "FNF: Connection error", e);
            isSuccess = false;
        }
        Log.d(LOG_TAG, " upload file isSuccess = " + isSuccess);
        return isSuccess;
    }

    // private void handleFileUploadResponse(GusJob j, FileUploadResponse resp)
    // {
    //
    // }
    //
    // private void handleFolderCreateResponse(GusJob j, FolderCreateResponse
    // resp) {
    //
    // }

    private void startLogon(GusJob j) {
        Log.d(LOG_TAG,
                new StringBuilder("startLogon(): ").append(
                        " need to login to server, trying to logon").toString());
        j.mState = GusJob.State.Logon;
        if(mIsAPRUpload ){
            getTime(j.mUploadData.mFilePath);
        }
        j.mRequest = new GusWS.LogonRequest(mParent, j, mLogonUrl, mDeltaElaspedTime,mDeltaUpTime);
        RequestHandler.getInstance().processRequest(j.mRequest, this);
    }

    private void startLogoff(GusJob j) {
        Log.d(LOG_TAG,
                new StringBuilder("startLogoff(): ").append("trying to logoff")
                        .toString());
        j.mState = GusJob.State.Logoff;
        j.mRequest = new GusWS.LogoffRequest(mParent, j, mLogoffUrl);
        RequestHandler.getInstance().processRequest(j.mRequest, this);
    }

    private void startFileList(GusJob j) {
        Log.d(LOG_TAG,
                new StringBuilder("startFileList(): ").append(
                        "try to get the bugReport file id").toString());
        j.mState = GusJob.State.FileList;
        j.mRequest = new GusWS.FileListRequest(mParent, j, mFileListUrl);
        RequestHandler.getInstance().processRequest(j.mRequest, this);
    }

    private void startGetSarUrl(GusJob j) {
        Log.d(LOG_TAG,
                new StringBuilder("startGetSarUrl: ").append(
                        " needs a Sar upload url, trying to get one")
                        .toString());
        j.mState = GusJob.State.GetSarUrl;
        j.mRequest = new GusWS.GetSarUrlRequest(mParent, j, mFileUploadUrl);
        RequestHandler.getInstance().processRequest(j.mRequest, this);
    }

    private void jobIsDone(GusJob j, ReturnCode code) {
        j.mState = GusJob.State.Idle;
        j.mRequest = null;
        mJobs.remove(j.mGusId);
        j.mCallback.done(j, code);
    }

    private void getTime(String fileName){
        Log.d(LOG_TAG,"      getTime ......");
        File root = new File(mParent.getFilesDir().toString());
        File[] files = root.listFiles();
            if(fileName != null && fileName.contains(Util.getVersion())){
                try
                {
                    BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
                    String str;
                    while ((str = in.readLine()) != null)
                    {
                        try{
                           mElaspedTime = Float.parseFloat( str.substring(0, str.indexOf(':')) );
                           mUpTime = Float.parseFloat( str.substring(str.indexOf('*') + 1, str.lastIndexOf(':')) );
                           mDeltaElaspedTime = Float.parseFloat( str.substring(str.indexOf(':') + 1,str.indexOf('*')) );
                           mDeltaUpTime = Float.parseFloat( str.substring(str.lastIndexOf(':') + 1) );
                        }catch (NumberFormatException e) {
                            Log.d(LOG_TAG, "error parsing sync error: " + str);
                        }
                    }
                    in.close();
                } catch (IOException e){
                    e.getStackTrace();
                }
            }
        Log.d(LOG_TAG,"\n\n\n  ------------------------ totalElaspedTime= "+mDeltaElaspedTime+",totalUptime="+mDeltaUpTime);
    }

    private void resetFile(String fileName){
        if(fileName != null){
             try
            {
                PrintWriter out = new PrintWriter(new FileOutputStream(new File(fileName), false));
                out.write(mElaspedTime + ":0.0" +  "*"+ Util.getUpTime() + ":0.0");
                out.close();
                Log.d(LOG_TAG,"resetFile:" + fileName);
            } catch (IOException e){
                e.getStackTrace();
            }
        }
    }
}

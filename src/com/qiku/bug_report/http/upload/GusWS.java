package com.qiku.bug_report.http.upload;

import java.io.File;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.qiku.bug_report.Constants;
import com.qiku.bug_report.helper.DeviceID;
import com.qiku.bug_report.helper.PBECoderUtil;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.http.Request;
import com.qiku.bug_report.http.Response;
import com.qiku.bug_report.http.upload.GUS.ReturnCode;

import android.os.Build;

public class GusWS {

	private enum Type {
		getuploadid, resume, upload, logon, logoff, filelist, getsarurl, fileupload, foldercreate
	}

	public static abstract class GusRequest extends Request {

		protected static final String ID_BASE = "fileupload/1/";
		protected static final String ROOT_URL_BASE = "/ws/" + ID_BASE;
		protected static final int BUF_SIZE = 8192;

		protected final GusJob mGusJob;
		protected final Type mType;
		protected final AtomicBoolean mCancelled;

		public GusRequest(Context parent, GusJob j, Type t, String url) {
			super(parent, url, null);
			mGusJob = j;
			mType = t;
			mCancelled = new AtomicBoolean();
		}

		public void cancel() {
			mCancelled.set(true);
		}

		public String getUrl() {
			if (mCancelled.get()) {
				throw new CancellationException(
						"Requestion has been cancelled: " + mGusJob.mGusId);
			}
			return super.getUrl();
		}

	}

	public static class LogonRequest extends GusRequest {

		private static final String LOG_TAG = "GusWS.LogonReq";

		public LogonRequest(Context parent, GusJob j, String url, float elapsedtime, float uptime) {
			super(parent, j, Type.logon, url);

			StringBuffer addtionalUrl = new StringBuffer();

			addtionalUrl.append("uid=").append("bugreport@qiku.com").append("&");
			StringBuffer temp = new StringBuffer();
			temp.append("logonPassword=").append("123456789")
					.append("&client=android&version=2.0.0")
					.append("&appType=pan").append("&channel=qiku");
			String data = temp.toString();
			try {
				data = PBECoderUtil.encrypty("SNMTLogon", data);
			} catch (Exception e) {
				Log.e(LOG_TAG, "passwd encryty error!", e);
			}
			addtionalUrl.append("data=").append(data).append("&")
					.append("logonType=").append("2").append("&")
					.append("mobilePattern=")
					.append(android.os.Build.MODEL.replaceAll(" ", ""))
					.append("&").append("mobileClientVersion=").append("2.0.0")
					.append("&").append("mobileSystemVersion=")
					.append(android.os.Build.VERSION.RELEASE)
					.append("&").append("BuildID=")
					//.append(android.os.Build.ID)
					.append(android.os.Build.VERSION.INCREMENTAL)
					.append("&").append("PRODUCT=")
					.append(android.os.Build.PRODUCT)
					.append("&").append("DEVICE=")
					//.append(android.os.Build.DEVICE)
					.append(DeviceID.getInstance().getId(parent))
					.append("&").append("BOARD=")
					.append(android.os.Build.BOARD)
					.append("&").append("SERIAL_NO=")
					.append(android.os.Build.SERIAL)
					.append("&").append("ELAPSED_TIME=")
					.append(String.valueOf(elapsedtime))
					.append("&").append("UP_TIME=")
					.append(String.valueOf(uptime));


			setUrl(addtionalUrl);

			Log.i(LOG_TAG, String.format("getUrl(): %s", getUrl()));

		}

		@Override
		public Response createResponse(int statusCode, JSONObject data) {
			return new LogonResponse(statusCode, data, mGusJob);
		}

	}

	public static class LogoffRequest extends GusRequest {

		private static final String LOG_TAG = "GusWS.LogoffReq";

		public LogoffRequest(Context parent, GusJob j, String url) {
			super(parent, j, Type.logoff, url);
			StringBuffer addtionalUrl = new StringBuffer();
			addtionalUrl.append("sessionId=").append(j.mSessionId);

			setUrl(addtionalUrl);
			Log.i(LOG_TAG, String.format("getUrl(): %s", getUrl()));

		}

		@Override
		public Response createResponse(int statusCode, JSONObject data) {
			return new LogoffResponse(statusCode, data, mGusJob);
		}

	}

	public static class FileListRequest extends GusRequest {

		private static final String LOG_TAG = "GusWS.FileListReq";

		public FileListRequest(Context parent, GusJob j, String url) {
			super(parent, j, Type.filelist, url);

			StringBuffer addtionalUrl = new StringBuffer();
			addtionalUrl.append("pageSize=").append("65535").append("&")
					.append("column=").append("TIME").append("&")
					.append("point=").append("DESC").append("&")
					.append("pageIndex=").append("1").append("&")
					.append("folderId=").append(j.mRootId).append("&")
					.append("userId=").append(j.mUserId);

			setUrl(addtionalUrl);
			Log.i(LOG_TAG, String.format("getUrl(): %s", getUrl()));

		}

		@Override
		public Response createResponse(int statusCode, JSONObject data) {
			return new FileListResponse(statusCode, data, mGusJob);
		}

	}

	public static class GetSarUrlRequest extends GusRequest {

		private static final String LOG_TAG = "GusWS.GetSarUrlRequest";

		public GetSarUrlRequest(Context parent, GusJob j, String url) {
			super(parent, j, Type.getsarurl, url);

			File uploadFile = new File(mGusJob.mUploadData.mFilePath);
			mGusJob.mUploadSize = uploadFile.length();

			try {
				StringBuffer addtionalUrl = new StringBuffer();
				String Sha1 = Util.getFileSha1(uploadFile);
				addtionalUrl.append("userId=").append(j.mUserId).append("&")
						.append("folderId=").append(j.mBugReportFolderId)
						.append("&").append("fileName=")
						.append(uploadFile.getName()).append("&")
						.append("fileSize=").append(j.mUploadSize).append("&")
						.append("sha1=").append(Sha1);
				j.mSha1 = Sha1;
				setUrl(addtionalUrl);
				Log.i(LOG_TAG, String.format("getUrl(): %s", getUrl()));
			} catch (Exception e) {
				Log.e(LOG_TAG, "GetSarUrlRequest error!", e);
			}
		}

		@Override
		public Response createResponse(int statusCode, JSONObject data) {
			return new GetSarUrlResponse(statusCode, data, mGusJob);
		}

	}

	public static class FileUploadRequest extends GusRequest {

		public FileUploadRequest(Context parent, GusJob j, String url) {
			super(parent, j, Type.fileupload, url);
		}

		@Override
		public Response createResponse(int statusCode, JSONObject data) {
			return new FileUploadResponse(statusCode, data, mGusJob);
		}

	}

	public static class FolderCreateRequest extends GusRequest {

		public FolderCreateRequest(Context parent, GusJob j, String url) {
			super(parent, j, Type.foldercreate, url);
		}

		@Override
		public Response createResponse(int statusCode, JSONObject data) {
			return new FolderCreateResponse(statusCode, data, mGusJob);
		}

	}

	public static abstract class GusResponse extends Response {

		private final GusJob mGusJob;

		public GusResponse(int statusCode, byte[] data, GusJob j) {
			super(statusCode, data);
			mGusJob = j;
		}

		public GusResponse(int statusCode, JSONObject data, GusJob j) {
			super(statusCode, data);
			mGusJob = j;
		}

		public GusJob getJob() {
			return mGusJob;
		}

		public ReturnCode getReturnCode() {
			switch (errorCode) {
			case None:
				return ReturnCode.SUCCESS;
			case BadRequestError:
				return ReturnCode.BAD_REQUEST;
			case InternalError:
			case UnknownError:
			default:
				return ReturnCode.SERVER_ERROR;
			}
		}
	}

	public static class LogonResponse extends GusResponse {

		private static final String LOG_TAG = "GusWS.LogonResponse";

		LogonResponse(int statusCode, JSONObject data, GusJob j) {
			super(statusCode, data, j);

			if (getErrorCode() == ErrorCode.None) {
				if (data != null) {
					try {
						JSONObject userInfo = data.getJSONObject("data");
						if (userInfo.has("sessionId"))
							j.mSessionId = userInfo.getString("sessionId");
						if (userInfo.has("userId"))
							j.mUserId = userInfo.getInt("userId");
						if (userInfo.has("rootId"))
							j.mRootId = userInfo.getInt("rootId");
						Log.i(LOG_TAG,
								new StringBuilder("sessionId: ")
										.append(j.mSessionId)
										.append(" userId: ").append(j.mUserId)
										.append(" rootId: ").append(j.mRootId)
										.toString());
					} catch (JSONException e) {
						Log.e(LOG_TAG,
								"LogonResponse(): got exception when parsing JOSNObject: ",
								e);
					}
				}
			}
		}

		public String getAppError() {
			return null;
		}
	}

	public static class LogoffResponse extends GusResponse {

		private static final String LOG_TAG = "GusWS.LogoffResponse";

		LogoffResponse(int statusCode, JSONObject data, GusJob j) {
			super(statusCode, data, j);
			j.mSessionId = null;
			j.mUserId = 0;
			j.mRootId = 0;
			j.mUploadIp = null;
			j.mCallbackUri = null;
			j.mSalAccessKeyId = null;
			j.mRequestId = null;
			j.mBucketName = null;
			j.mRange = 0;
			j.mNeedUpload = true;

			Log.i(LOG_TAG, new StringBuilder("sessionId: ")
					.append(j.mSessionId).append(" userId: ").append(j.mUserId)
					.append(" rootId: ").append(j.mRootId).toString());
			Constants.getCookieStore().clear();
			List<Cookie> cookies = Constants.getCookieStore().getCookies();
			for (int i = 0; i < cookies.size(); i++) {
				Log.d(LOG_TAG,
						new StringBuilder("Local cookie:").append(
								cookies.get(i)).toString());
			}
		}

		public String getAppError() {
			return null;
		}
	}

	public static class FileListResponse extends GusResponse {

		private static final String LOG_TAG = "GusWS.FileListResponse";

		FileListResponse(int statusCode, JSONObject data, GusJob j) {
			super(statusCode, data, j);

			if (getErrorCode() == ErrorCode.None) {
				if (data != null) {
					try {
						JSONArray jsonArray = data.getJSONObject("data")
								.getJSONArray("files");
						// 记录开机时长上传到新文件夹
						String folder = null;
						if (j.isZip == true) {
							folder = "bugreport";
						} else {
							folder = "APR";
						}
						Log.i("获取上传地址", folder);

						for (int i = 0; i < jsonArray.length(); i++) {
							Log.i(LOG_TAG, jsonArray.getJSONObject(i)
									.getString("name"));
							if (jsonArray.getJSONObject(i).getString("name")
									.equals(folder)) {
								j.mBugReportFolderId = jsonArray.getJSONObject(
										i).getInt("id");
								break;
							}
						}

						Log.i(LOG_TAG, new StringBuilder("BugReportFolderId: ")
								.append(j.mBugReportFolderId).toString());

					} catch (JSONException e) {
						Log.e(LOG_TAG,
								"LogonResponse(): got exception when parsing JOSNObject: ",
								e);
					}
				}
			}

		}

		public String getAppError() {
			return null;
		}
	}

	public static class GetSarUrlResponse extends GusResponse {
		private static final String LOG_TAG = "GusWS.GetSarUrlResponse";

		GetSarUrlResponse(int statusCode, JSONObject data, GusJob j) {
			super(statusCode, data, j);
			if (getErrorCode() == ErrorCode.None) {
				if (data != null) {
					try {
						JSONObject dataObject = data.getJSONObject("data");
						if (dataObject.has("uploadIp"))
							j.mUploadIp = dataObject.getString("uploadIp");
						if (dataObject.has("callbackUri"))
							j.mCallbackUri = dataObject
									.getString("callbackUri");
						if (dataObject.has("salAccessKeyId"))
							j.mSalAccessKeyId = dataObject
									.getString("salAccessKeyId");
						if (dataObject.has("requestId"))
							j.mRequestId = dataObject.getString("requestId");
						if (dataObject.has("bucketName"))
							j.mBucketName = dataObject.getString("bucketName");
						if (dataObject.has("range"))
							j.mRange = dataObject.getInt("range");
						if (dataObject.has("needUpload"))
							j.mNeedUpload = dataObject.getBoolean("needUpload");

						Log.i(LOG_TAG,
								new StringBuilder("GetSarUrlResponse:")
										.append(" upoloadIp: ")
										.append(j.mUploadIp)
										.append(" calbackUri: ")
										.append(j.mCallbackUri)
										.append(" salAccessKeyId: ")
										.append(j.mSalAccessKeyId)
										.append(" requestId: ")
										.append(j.mRequestId)
										.append(" bucketName: ")
										.append(j.mBucketName).toString());
					} catch (JSONException e) {
						Log.e(LOG_TAG,
								"GetSarUrlResponse(): got exception when parsing JOSNObject: ",
								e);
					}
				}
			}
		}

		public String getAppError() {
			return null;
		}
	}

	public static class FileUploadResponse extends GusResponse {

		FileUploadResponse(int statusCode, JSONObject data, GusJob j) {
			super(statusCode, data, j);
		}

		public String getAppError() {
			return null;
		}
	}

	public static class FolderCreateResponse extends GusResponse {

		FolderCreateResponse(int statusCode, JSONObject data, GusJob j) {
			super(statusCode, data, j);
		}

		public String getAppError() {
			return null;
		}
	}

}

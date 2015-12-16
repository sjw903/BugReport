
package com.qiku.bug_report.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;













//import com.google.android.mms.ContentType;
import com.qiku.bug_report.R;
import com.qiku.bug_report.helper.DialogHelper;
import com.qiku.bug_report.helper.Util;
import com.qiku.bug_report.model.UserSettings;

public class ReportProblemDescriptionFragment extends Fragment implements
        android.view.View.OnClickListener {

    private static final int FILE_SELECT_CODE = 1000;
    private static final String CONTENTTYPE = "image/*";
    public static final String USER_PHONE_NUMBER = "user_phone";
    public static final String USER_EMAIL_ADRESS = "user_email";

    private Context mContext;
    private String mEditString;
    private EditText mEditText;
    private LinearLayout ll_add_more;
    private ImageView iv_add_more_arrow;
    private TextView tv_add_more_text;
    private LinearLayout ll_add_image;
    private LinearLayout ll_add_phone_email;
    private boolean mShowAddMore = true;
    private EditText mEditTextPhoneNumber;
    private String mPhoneString;
    private EditText mEditTextEmailAdress;
    private String mAdressString;
    private Button mBtnCommit;
    private ScreenShotsAdapter mScreenShotsAdapter;
    private OnItemClickListener mItemClickListener;
    private int mSelectIndex = 0;
    public boolean canClick = true;

    public EditText getmEditText() {
        return mEditText;
    }

    public ScreenShotsAdapter getmScreenShotsAdapter() {
        return mScreenShotsAdapter;
    }

    public ReportProblemDescriptionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        if (mContext instanceof Launcher) {
            mEditString = ((Launcher)mContext).mDescriptionText;
            mScreenShotsAdapter = ((Launcher)mContext).mScreenShotsAdapter;
            mPhoneString = ((Launcher) mContext).mPhoneText;
            mAdressString = ((Launcher) mContext).mEmailText;
        }
        if (null == mScreenShotsAdapter) {
            mScreenShotsAdapter = new ScreenShotsAdapter(mContext);
        }
        mItemClickListener = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                mSelectIndex = position;
                // Intent intent = new Intent(Intent.ACTION_PICK,
                // android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // startActivityForResult(intent, FILE_SELECT_CODE);
                selectPicture(mContext, FILE_SELECT_CODE, CONTENTTYPE, false);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        View view = inflater.inflate(R.layout.fragment_problem_description, container, false);
        mEditText = (EditText) view.findViewById(R.id.et_problem_description);
        if (mEditString != null && !mEditString.trim().equals("")) {
            mEditText.setText(mEditString);
        }
        setEditTextMaxCharacters(mEditText, mContext, 2000, R.string.problem_description_max_length_toast);
        ll_add_more = (LinearLayout)view.findViewById(R.id.ll_add_more);
        ll_add_more.setOnClickListener(this);
        iv_add_more_arrow = (ImageView)view.findViewById(R.id.iv_add_more);
        tv_add_more_text = (TextView)view.findViewById(R.id.tv_add_more);
        ll_add_image = (LinearLayout)view.findViewById(R.id.ll_add_image);
        ll_add_phone_email = (LinearLayout)view.findViewById(R.id.ll_add_phone_email);
        mEditTextPhoneNumber = (EditText) view.findViewById(R.id.et_problem_user_info_phone);
        mEditTextEmailAdress = (EditText) view.findViewById(R.id.et_problem_user_info_email);
        inittialEditText();
        mBtnCommit = (Button) view.findViewById(R.id.bt_problem_user_info_commit);
        mBtnCommit.setOnClickListener(this);
        GridView grid = (GridView) view.findViewById(R.id.gd_problem_screen_shots);
        grid.setAdapter(mScreenShotsAdapter);
        grid.setOnItemClickListener(mItemClickListener);
        return view;
    }

    private void selectPicture(Context context, int requestCode, String contentType,
            boolean localFilesOnly) {
        if (context instanceof Activity) {

            Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            innerIntent.setType(contentType);
            if (localFilesOnly) {
                innerIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            }
            Intent wrapperIntent = Intent.createChooser(innerIntent, null);
            startActivityForResult(wrapperIntent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (resultCode == Activity.RESULT_OK && requestCode == FILE_SELECT_CODE) {
            if (resultCode == Activity.RESULT_OK
                    && requestCode == FILE_SELECT_CODE) {
                Uri uri = data.getData();
                Log.d("URI", uri.getPath());
                String path = getPath(mContext, uri);
                Log.d("Path", path);
                if (path != null && path.length() > 0) {
                    if (!mScreenShotsAdapter.contains(path)) {
                        if (null != mScreenShotsAdapter
                                .getShotsFile(mSelectIndex)) {
                            mScreenShotsAdapter.updateShotsFile(mSelectIndex,
                                    path);
                        } else {
                            mScreenShotsAdapter.addShotsFile(path);
                        }
                        mScreenShotsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(mContext,
                                getString(R.string.problem_description_select_same_file),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri,
            String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub

        switch (view.getId()) {

            case R.id.ll_add_more:
                updateImageAndPhonePanel(mShowAddMore);
                break;

            case R.id.bt_problem_user_info_commit:
                if (!checkDescription()) {
                    Util.showToast(mContext, R.string.problem_description_toast_info);
                    return;
                }
                saveWhenCommit();
                if (mContext instanceof Launcher) {
                    ((Launcher) mContext).sendReport();
                }
                break;

            default:
                break;
        }
    }

    private void updateImageAndPhonePanel(boolean showAddMore) {
        if (showAddMore) {
            iv_add_more_arrow.setBackgroundResource(R.drawable.bugreport_ic_blue_close);
            tv_add_more_text.setText(R.string.problem_description_no_add_more);
            mShowAddMore = false;
        } else {
            iv_add_more_arrow.setBackgroundResource(R.drawable.bugreport_ic_blue_open);
            tv_add_more_text.setText(R.string.problem_description_add_more);
            mShowAddMore = true;
        }
        int visible = showAddMore == true ? View.VISIBLE :View.INVISIBLE;
        ll_add_image.setVisibility(visible);
        ll_add_phone_email.setVisibility(visible);
    }

    private boolean checkDescription() {
        String description = mEditText.getText().toString();
        boolean isOK = false;
        if ((description.length() == 0 || description.trim().equals(""))
                && mScreenShotsAdapter.isShotsEmpty()) {
            isOK = false;
        } else {
            isOK = true;
        }
        return isOK;
    }

    private void showDialog() {
        DialogHelper.createDialog(mContext, R.string.problem_description_dialog_info, 0, 0,
                R.string.problem_description_dialog_cancel, null,
                R.string.problem_description_dialog_ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        destroy();
                    }
                }).show();
    }

    private void destroy() {
        if (mContext instanceof Launcher) {
            ((Launcher) mContext).onExit();
        }
    }

    public void initData(String editText, ScreenShotsAdapter adapter) {
        mEditString = editText;
        mScreenShotsAdapter = adapter;
        if (mEditText != null && mEditString != null && !mEditString.trim().equals("")) {
            mEditText.setText(mEditString);
        }
        if (mScreenShotsAdapter != null) {
            mScreenShotsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    private void setEditTextMaxCharacters(final EditText editText, final Context context, final int max, final int stringId) {
        editText.addTextChangedListener(new TextWatcher() {
            private CharSequence temp;
            private int editStart ;
            private int editEnd ;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
                temp = s;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                editStart = editText.getSelectionStart();
                editEnd = editText.getSelectionEnd();
                if (temp.length() > max) {
                    String text = context.getResources().getString(stringId);
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                    s.delete(editStart-1, editEnd);
                    int tempSelection = editStart;
                    editText.setText(s);
                    editText.setSelection(tempSelection);
                }
            }
        });
    }

    private void saveWhenCommit() {
        String phone = mEditTextPhoneNumber.getText().toString();
        String adress = mEditTextEmailAdress.getText().toString();
        UserSettings settings = ((Launcher) mContext).mTaskMaster
                .getConfigurationManager().getUserSettings();
        settings.setPhone(phone);
        settings.setEmail(adress);
        ((Launcher) mContext).mTaskMaster.getConfigurationManager()
                .saveUserSettings(settings);
        if (phone != null) {
            System.putString(mContext.getContentResolver(), USER_PHONE_NUMBER,
                    phone);
        }
        if (adress != null) {
            System.putString(mContext.getContentResolver(), USER_EMAIL_ADRESS,
                    adress);
        }
    }

    private void inittialEditText() {
        if (mPhoneString != null && !mPhoneString.trim().equals("")) {
            mEditTextPhoneNumber.setText(mPhoneString);
        } else {
            mEditTextPhoneNumber.setText(System.getString(mContext.getContentResolver(),
                    USER_PHONE_NUMBER));
        }
        if (mAdressString != null && !mAdressString.trim().equals("")) {
            mEditTextEmailAdress.setText(mAdressString);
        } else {
            mEditTextEmailAdress.setText(System.getString(mContext.getContentResolver(),
                    USER_EMAIL_ADRESS));
        }
    }
}

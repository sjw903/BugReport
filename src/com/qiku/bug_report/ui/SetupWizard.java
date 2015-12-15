package com.qiku.bug_report.ui;

import android.app.Activity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.qiku.bug_report.BugReportApplication;
import com.qiku.bug_report.Constants;
import com.qiku.bug_report.R;
import com.qiku.bug_report.TaskMaster;
import com.qiku.bug_report.helper.Notifications;
import com.qiku.bug_report.model.UserSettings;

public class SetupWizard extends Activity {

	CheckBox mTronxyz_employee;
	EditText mCoreId;
	EditText mFirstName;
	EditText mPhoneNumber;
	Button mBtnReset;
	Button mBtnDone;
	Button mBtnSkip;
	TaskMaster mTaskMaster;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup_user_info);
		mTaskMaster = ((BugReportApplication) getApplicationContext())
				.getTaskMaster();

		mTronxyz_employee = (CheckBox) findViewById(R.id.setup_tronxyz_employee);
		mCoreId = (EditText) findViewById(R.id.setup_coreId);

		mFirstName = (EditText) findViewById(R.id.setup_firstName);
		mPhoneNumber = (EditText) findViewById(R.id.setup_phone);

		mBtnReset = (Button) findViewById(R.id.setup_reset);
		mBtnDone = (Button) findViewById(R.id.setup_done);
		// chenf
		mBtnSkip = (Button) findViewById(R.id.setup_skip);
		// Remove this notification from the bar
		Notifications
				.cancel(this, R.string.notification_title_incomplete_setup);

		init();
		addListener();
	}

	private void init() {
		UserSettings settings = mTaskMaster.getConfigurationManager()
				.getUserSettings();
		String coreid = settings.getCoreID();
		String email = settings.getEmail();

		if (!TextUtils.isEmpty(email) && TextUtils.isEmpty(coreid)) {
			mTronxyz_employee.setChecked(false);
		}

		if (mTronxyz_employee.isChecked()) {
			if (!TextUtils.isEmpty(coreid))
				mCoreId.setText(coreid);
		} else {
			mCoreId.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			if (!TextUtils.isEmpty(email)) {
				mCoreId.setText(email);
			} else {
				mCoreId.setHint(R.string.settings_email);
			}
		}

		if (!TextUtils.isEmpty(settings.getFirstName())) {
			mFirstName.setText(settings.getFirstName());
		}

		if (!TextUtils.isEmpty(settings.getPhone())) {
			mPhoneNumber.setText(settings.getPhone());
		}

		if (mCoreId.getText().toString().equals("unknown")) {
			mCoreId.setText("");
			mFirstName.setText("");
			mPhoneNumber.setText("");
		}
		if (!mTaskMaster.getConfigurationManager().getUserSettings()
				.isContactInfoComplete()) {
			settings.setCoreID("unknown");
			settings.setEmail("unknown");
			settings.setFirstName("unknown");
			TelephonyManager mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			String num = mTm.getLine1Number();
			try {
				if (num.isEmpty()) {
					settings.setPhone("unknown");
				} else {
					settings.setPhone(num);
				}
			} catch (NullPointerException e) {
				settings.setPhone("unknown");
			}
			mTaskMaster.getConfigurationManager().saveUserSettings(settings);
		}
	}

	private void addListener() {
		mTronxyz_employee
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						mCoreId.setText(null);
						mCoreId.setHint(isChecked ? R.string.settings_coreid
								: R.string.settings_email);
						if (isChecked) {
							mCoreId.setInputType(InputType.TYPE_CLASS_TEXT);
						} else {
							mCoreId.setInputType(InputType.TYPE_CLASS_TEXT
									| InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
						}
					}
				});

		mBtnReset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// chenf
				mTronxyz_employee.setChecked(true);
				mCoreId.setText(null);
				mFirstName.setText(null);
				mPhoneNumber.setText(null);
			}
		});

		mBtnDone.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (TextUtils.isEmpty(mCoreId.getText().toString())) {
					mCoreId.requestFocus();
					return;
				}
				if (TextUtils.isEmpty(mFirstName.getText().toString())) {
					mFirstName.requestFocus();
					return;
				}
				if (TextUtils.isEmpty(mPhoneNumber.getText().toString())) {
					mPhoneNumber.requestFocus();
					return;
				}
				// Save user settings to file
				UserSettings settings = mTaskMaster.getConfigurationManager()
						.getUserSettings();
				if (mTronxyz_employee.isChecked()) {
					settings.setCoreID(mCoreId.getText().toString());
					settings.setEmail(settings.getCoreID()
							+ Constants.TRONXYZ_EMAIL_DOMAIN);
				} else {
					settings.setCoreID(null);
					settings.setEmail(mCoreId.getText().toString());
				}
				settings.setFirstName(mFirstName.getText().toString());
				settings.setPhone(mPhoneNumber.getText().toString());
				mTaskMaster.getConfigurationManager()
						.saveUserSettings(settings);
				finish();
			}
		});

		// chenf
		mBtnSkip.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				finish();
			}
		});
	}

	public void onDestroy() {
		super.onDestroy();
	}

}

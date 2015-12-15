package com.tronxyz.bug_report.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.tronxyz.bug_report.R;
import com.tronxyz.bug_report.helper.Util;

public class Help extends Activity{

    TextView mContentView;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        setTitle(R.string.help_title);

        String mkp = Util.getMagicKeyNames(this);
        if(mkp == null)
            mkp = getString(R.string.help_answer_magic_keys);
        String content = getString(R.string.help_content);
        content = String.format(content, mkp);

        mContentView = (TextView)findViewById(R.id.help_contentview);
        mContentView.setText(content);

    }
}

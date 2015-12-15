
package com.qiku.bug_report.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BottomBar extends LinearLayout {

    private boolean mHasInit;
    private boolean mHasKeybord;
    private int mHeight;

    public BottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        super.onLayout(changed, l, t, r, b);
        if (!mHasInit) {
            mHasInit = true;
            mHeight = b;
        } else {
            mHeight = mHeight < b ? b : mHeight;
        }
        if (mHasInit && mHeight > b) {
            mHasKeybord = true;
        } 
        if (mHasInit && mHasKeybord && mHeight == b) {
            mHasKeybord = false;
        }
    }
}

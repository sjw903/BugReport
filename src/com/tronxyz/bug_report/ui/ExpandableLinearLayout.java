package com.tronxyz.bug_report.ui;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExpandableLinearLayout extends LinearLayout {
    private TextView mTitle;
    private LinearLayout mContent;
    private boolean mExpanded = true;
    private LayoutInflater mInflater;
    private List<? extends Object> mData;

    public ExpandableLinearLayout(Context context, AttributeSet attrs){
        this(context, attrs,  null, null);
    }

    public ExpandableLinearLayout(Context context, AttributeSet attrs, String groupTitle, List<? extends Object> data) {
        super(context, attrs);
        this.setOrientation(VERTICAL);
        mData = data;
        mInflater = LayoutInflater.from(context);

        mTitle = new TextView(context, null, android.R.attr.textAppearanceLarge);
        mTitle.setText(groupTitle);
        mTitle.setPadding(0, 5, 0, 5);
        addView(mTitle, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mContent = new LinearLayout(context);
        mContent.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mContent.setOrientation(VERTICAL);
        addView(mContent, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        addListener();
        fillData();
        toogle();
    }

    private void addListener(){
        mTitle.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                toogle();
            }
        });
    }

    private void fillData(){
        mContent.removeAllViews();
        for(int i=0; mData != null && i<mData.size(); i++){
            TextView itemView = (TextView)mInflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
            itemView.setText(mData.get(i).toString());
            mContent.addView(itemView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
    }

    private void toogle(){
        mExpanded = !mExpanded;
        mContent.setVisibility(mExpanded ? VISIBLE : GONE);
    }

    public void setData(List<? extends Object> data){
        mData = data;
        fillData();
    }

    public void setTitle(String title){
        mTitle.setText(title);
    }
}

package com.qiku.bug_report.conf;

import android.content.Context;

import com.qiku.bug_report.conf.bean.ConfigEntry;

public abstract class Configurable<T extends ConfigEntry> {
    private Context mContext;
    private XMLParserBase<T> mParser;
    protected T mConf;
    public Configurable(Context context, XMLParserBase<T> parser){
        this.mContext = context;
        this.mParser = parser;
    }

    /**
     * called while the Configurable starts
     * To overrides this method to initialize the mConf when the Configurable starts
     */
    public abstract void start();


    public Context getContext(){
        return mContext;
    }
    protected XMLParserBase<T> getParser(){
        return mParser;
    }

    public T get(){
        return mConf;
    }
}

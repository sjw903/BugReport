package com.qiku.bug_report.conf.bean;

public abstract class ParentAware<T> {
    protected T mParent;

    public T getParent(){
        return mParent;
    }

    public void setParent(T t){
        mParent = t;
    }
}

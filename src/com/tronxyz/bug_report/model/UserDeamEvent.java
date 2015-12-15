package com.tronxyz.bug_report.model;

import java.io.InputStream;
import java.io.IOException;

import com.tronxyz.bug_report.conf.bean.Deam.Tag.Type;

public class UserDeamEvent extends DeamEvent {
    private String tag;
    private long time;

    public UserDeamEvent(String tag, long time) {
        this.tag = tag;
    }
    @Override
    public Type getType() {
        return Type.USER;
    }
    @Override
    public String getTag() {
        return this.tag;
    }
    @Override
    public long getTimeMillis() {
        return this.time;
    }
    @Override
    public boolean isEntryText() {
        return false;
    }
    @Override
    public InputStream createEntryInputStream() throws IOException {
        throw new IOException("User type doesn't have entry data");
    }
    @Override
    public void cleanup() {
        // Nothing to do.
    }
}

package com.tronxyz.bug_report.model;

import java.io.InputStream;
import java.io.IOException;

import com.tronxyz.bug_report.conf.bean.Deam.Tag.Type;

public abstract class DeamEvent {
    public abstract Type getType();
    public abstract String getTag();
    public abstract long getTimeMillis();
    public abstract boolean isEntryText();
    public abstract InputStream createEntryInputStream() throws IOException;
    // This must be called to ensure all system resources are cleaned up.
    public abstract void cleanup();
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        result.append(this.getClass().getName() + " Object {" + NEW_LINE);
        result.append(" Type: " + getType().toString() + NEW_LINE);
        result.append(" Tag: " + getTag() + NEW_LINE);
        result.append(" Text?: " + Boolean.valueOf(isEntryText()).toString() + NEW_LINE);
        result.append("}");
        return result.toString();
    }
}

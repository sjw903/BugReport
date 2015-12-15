package com.qiku.bug_report.model;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import android.os.DropBoxManager;

import com.qiku.bug_report.conf.bean.Deam.Tag.Type;

public class DropBoxDeamEvent extends DeamEvent {
    private DropBoxManager.Entry mEntry;
    private DropBoxManager mDropBox;
    private boolean mIsEntryInputStreamUsed = false;

    public DropBoxDeamEvent(DropBoxManager dropbox, String tag, long timeMillis)
            throws IOException {
        mDropBox = dropbox;
        mEntry = createEntry(tag, timeMillis);
    }
    @Override
    public Type getType() {
        return Type.DROPBOX;
    }
    @Override
    public long getTimeMillis() {
        return mEntry.getTimeMillis();
    }
    @Override
    public String getTag() {
        return mEntry.getTag();
    }
    @Override
    public boolean isEntryText() {
        return (mEntry.getFlags() & DropBoxManager.IS_TEXT) == DropBoxManager.IS_TEXT;
    }
    @Override
    public InputStream createEntryInputStream() throws IOException {
        // There is no way to reset the InputStream returned for a DropBox entry, so that it
        // can be reused from the beginning, so we must re-fetch the entry in order to give a
        // new InputStream for the caller to use.  The caller is responsible for closing the
        // stream, but this method is responsible for closing the prior entry.
        if (false == mIsEntryInputStreamUsed) {
            // This runs on the 1st call to this method.
            mIsEntryInputStreamUsed = true;
            return mEntry.getInputStream();
        } else {
            // This runs on the 2nd-Nth calls to this method.
            String tag = mEntry.getTag();
            long timeMillis = mEntry.getTimeMillis();
            DropBoxManager.Entry oldEntry = mEntry;
            mEntry = createEntry(tag, timeMillis);
            oldEntry.close();
            return mEntry.getInputStream();
        }
    }
    @Override
    public void cleanup() {
        mEntry.close();
    }
    private DropBoxManager.Entry createEntry(String tag, long timeMillis)
            throws FileNotFoundException {
        DropBoxManager.Entry entry = mDropBox.getNextEntry(tag, timeMillis - 1);
        if (null == entry || timeMillis != entry.getTimeMillis())
            throw new FileNotFoundException("DropBox entry disappeared: " +
                    tag + "@" + timeMillis);
        return entry;
    }
}

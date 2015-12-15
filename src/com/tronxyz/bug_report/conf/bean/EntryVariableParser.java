package com.tronxyz.bug_report.conf.bean;

import java.io.InputStream;
import java.io.IOException;

import com.tronxyz.bug_report.conf.bean.Scenario.Parser.VariableParser;
import com.tronxyz.bug_report.model.DeamEvent;

public class EntryVariableParser extends VariableParser{
    protected InputStream createInputStream(DeamEvent event) throws IOException {
        if (!event.isEntryText())
            throw new IOException("Cannot parse non-text entry");
        return event.createEntryInputStream();
    }
}

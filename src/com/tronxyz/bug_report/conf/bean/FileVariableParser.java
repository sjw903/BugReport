package com.tronxyz.bug_report.conf.bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.tronxyz.bug_report.conf.bean.Scenario.Parser.VariableParser;
import com.tronxyz.bug_report.model.DeamEvent;

public class FileVariableParser extends VariableParser{
    public String mFilePath;

    protected InputStream createInputStream(DeamEvent event) throws IOException {
        return new FileInputStream(new File(mFilePath));
    }
}

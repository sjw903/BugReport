package com.tronxyz.bug_report;

public class BugReportException extends Exception {

    private static final long serialVersionUID = 1L;

    public BugReportException() {
        // TODO Auto-generated constructor stub
    }

    public BugReportException(String detailMessage) {
        super(detailMessage);
        // TODO Auto-generated constructor stub
    }

    public BugReportException(Throwable throwable) {
        super(throwable);
        // TODO Auto-generated constructor stub
    }

    public BugReportException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        // TODO Auto-generated constructor stub
    }

}

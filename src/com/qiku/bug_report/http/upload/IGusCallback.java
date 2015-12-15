package com.qiku.bug_report.http.upload;

public interface IGusCallback {

    /**
     * done() - called by GUS when the current job is done.
     *
     * @param job
     *            - the job that just finished
     * @param code
     *            - the error code
     */
    public void done(GusJob job, GUS.ReturnCode code);
}

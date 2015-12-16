//--------------------------------------------------------------------------------------------------
// Description:
//  A utility to collect a user bugreport for the BugReport app.  It's intended to run as a one-shot
//  init service, with escalated permissions.
//--------------------------------------------------------------------------------------------------

// TODO Consider allocating strings on the heap, with only as much length as needed, instead of
// using static allocations of PATH_MAX chars.

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <time.h>
#include <fcntl.h>
#include <sys/prctl.h>
#include <sys/vfs.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>

#define LOG_TAG "qk-bugreport"
#include <cutils/log.h>
#include <cutils/properties.h>
#include <private/android_filesystem_config.h>

#include "Babysitter.h"
#include "capability.h"

using namespace NsBabysitter;

#define VERSION "2"

#define REPORT_ID_MAX 64

#if (PLATFORM_SDK_VERSION >= 17)
// As of Android 4.2, Google does not want log files in unsecure external storage.
#define OUTPUT_DEV_PATH         "/data"
#else
#define OUTPUT_DEV_PATH         "/sdcard"
#endif
#define ROOT_REPORT_DIR_PATH    OUTPUT_DEV_PATH"/bugreport"
#define SCREENSHOT_DIR_PATH     ROOT_REPORT_DIR_PATH"/screenshots"
#define DUMPSTATE       "dumpstate"
#define CPUINFO         "cat  /proc/cpuinfo"
#define DEBUG_VERSION   "userdebug";
#define USER_VERSION    "user"


static int
safeChmod(const char* path, mode_t mode)
{
    int fd = open(path, O_RDONLY|O_NOFOLLOW);
    if (-1 == fd) {
        return -1;
    }
    int result = fchmod(fd, mode);
    if (-1 == result) {
        int errnoCopy = errno;
        close(fd);
        errno = errnoCopy;
        return -1;
    }
    close(fd);
    return 0;
}

static void
vibrate(const char* durationSecs)
{
    FILE* vibratorFile = fopen("/sys/class/timed_output/vibrator/enable", "w");
    if (vibratorFile) {
        fputs(durationSecs, vibratorFile);
        fclose(vibratorFile);
    }
}

static size_t
getAvailableSpaceMb(const char* path)
{
    size_t availableSpaceMb = 0;
    struct statfs pathStats;
    if (-1 == statfs(path, &pathStats)) {
        ALOGE("statfs(%s) failed; errno=%s", path, strerror(errno));
    } else {
        availableSpaceMb = (pathStats.f_bavail * pathStats.f_bsize) / 1048576;
        ALOGI("Available space in %s is %zu MB", path, availableSpaceMb);
    }
    return availableSpaceMb;
}

static size_t
getRequiredSpace()
{
    char requiredSpaceProp[PROPERTY_VALUE_MAX];
    property_get("ro.bugreport.bugreport.size.max", requiredSpaceProp, "50");
    size_t requiredSpace = atoi(requiredSpaceProp);
    ALOGI("Required space is %zu", requiredSpace);
    return requiredSpace;
}

static bool
shouldRun()
{
    /*// Run if this is a debuggable build.
    char debuggableProp[PROPERTY_VALUE_MAX];
    property_get("ro.debuggable", debuggableProp, "0");
    if ('1' == debuggableProp[0])
        return true;
    // Run if ADB (USB Debugging) is enabled.
    char adbSvcStatusProp[PROPERTY_VALUE_MAX];
    property_get("init.svc.adbd", adbSvcStatusProp, "");
    if (0 == strcmp(adbSvcStatusProp, "running"))
        return true;
    // All checks failed.
    return false;*/
	//M: by chengwenbin,this will be running always
	return true;
}

static void
createReportId(char* reportId)
{
    time_t localTime;
    struct tm localTm;
    time(&localTime);
    localtime_r(&localTime, &localTm);
    strftime(reportId, REPORT_ID_MAX, "%Y-%m-%d_%H-%M-%S.000%z", &localTm);
    ALOGI("reportId=%s", reportId);
}

static bool
runActivityMgr(char* const argv[])
{
    Babysitter sitter(LOG_TAG, LOG_PRI_WARN);
    sitter.setOrphanKillSignal(SIGKILL);
    int dupFrom[] = {0, 1, 2, -1};
    int dupTo[] =   {0, 1, 2, -1};
    if (-1 == sitter.createChild("am", argv, dupFrom, dupTo)) {
        ALOGE("Could not create child");
        return false;
    }
    const struct timeval timeout = {10, 0};
    if (!sitter.watchChild(timeout)) {
        sitter.killChild();
    }
    return 0 == sitter.waitChildExitStatus() ? true : false;
}

static bool
sendStartIntent(const char* reportId, const char* reportDirPath)
{
// TODO Make this a broadcast intent.
    char* const argv[] = {
            (char*)"am",
            (char*)"startservice",
            (char*)"-a",
            (char*)"qiku.intent.action.BUGREPORT.START",
            (char*)"-t",
            (char*)"text/plain",
            (char*)"--ei",
            (char*)"version",
            (char*)VERSION,
            (char*)"-e",
            (char*)"id",
            (char*)reportId,
            (char*)"-e",
            (char*)"log_path",
            (char*)reportDirPath,
            (char*)NULL};
    return runActivityMgr(argv);
}

static bool
sendEndIntent(const char* reportId, const char* reportDirPath, const char* reportInfoFilePath)
{
// TODO Make this a broadcast intent.
    char* const argv[] = {
            (char*)"am",
            (char*)"startservice",
            (char*)"-a",
            (char*)"qiku.intent.action.BUGREPORT.END",
            (char*)"-t",
            (char*)"text/plain",
            (char*)"--ei",
            (char*)"version",
            (char*)VERSION,
            (char*)"-e",
            (char*)"id",
            (char*)reportId,
            (char*)"-e",
            (char*)"report_info",
            (char*)reportInfoFilePath,
            (char*)NULL};
    return runActivityMgr(argv);
}

static bool
sendErrorIntent(const char* reportId, const char* error)
{
// TODO Make this a broadcast intent.
    char* const argv[] = {
            (char*)"am",
            (char*)"startservice",
            (char*)"-a",
            (char*)"qiku.intent.action.BUGREPORT.ERR",
            (char*)"-t",
            (char*)"text/plain",
            (char*)"--ei",
            (char*)"version",
            (char*)VERSION,
            (char*)"-e",
            (char*)"errortype",
            (char*)error,
            (char*)"-e",
            (char*)"id",
            (char*)reportId,
            (char*)NULL};
    return runActivityMgr(argv);
}

static bool
captureScreenshot(const char* screenshotFilePath)
{
    Babysitter sitter(LOG_TAG, LOG_PRI_WARN);
    sitter.setOrphanKillSignal(SIGKILL);
    char* const argv[] = {
            (char*)"screencap",
            (char*)"-p",
            (char*)screenshotFilePath,
            (char*)NULL};
    int dupFrom[] = {0, 1, 2, -1};
    int dupTo[] =   {0, 1, 2, -1};
    if (-1 == sitter.createChild("screencap", argv, dupFrom, dupTo)) {
        ALOGE("Could not create child");
        return false;
    }
    const struct timeval timeout = {10, 0};
    if (!sitter.watchChild(timeout)) {
        sitter.killChild();
    }
    if (0 == sitter.waitChildExitStatus()) {
        safeChmod(screenshotFilePath, S_IRUSR|S_IWUSR|S_IRGRP /* 0640 */);
        lchown(screenshotFilePath, AID_LOG, AID_LOG);
        return true;
    } else {
        return false;
    }
}

static bool
runDumpstate(const char* dumpstateFilePath)
{
    // Send output directly to a file that we've created, so that we can
    // control the permissions.
    int outFd = open(dumpstateFilePath, O_CREAT|O_WRONLY|O_TRUNC|O_NOFOLLOW,
            S_IRUSR|S_IWUSR|S_IRGRP /* 0640 */);
    if (-1 == outFd) {
        ALOGE("open(%s) failed; errno=%s", dumpstateFilePath, strerror(errno));
        return false;
    }
    if (-1 == fchown(outFd, AID_LOG, AID_LOG)) {
        ALOGE("fchown(%s) failed; errno=%s", dumpstateFilePath, strerror(errno));
        close(outFd);
        return false;
    }
    char buildTypeProp[PROPERTY_VALUE_MAX];
    property_get("ro.build.type", buildTypeProp, USER_VERSION);
    if (strcmp(USER_VERSION, buildTypeProp) == 0) {
        close(outFd);
        return true;
    }
    Babysitter sitter(LOG_TAG, LOG_PRI_WARN);
    sitter.setOrphanKillSignal(SIGKILL);
    char* const argv[] = {
            (char*)DUMPSTATE,
            // Disable vibrations; we do those ourselves.
            (char*)"-q",
            (char*)NULL};
    int dupFrom[] = {0, outFd, outFd, -1};
    int dupTo[] =   {0, 1,     2,     -1};
    if (-1 == sitter.createChild(DUMPSTATE, argv, dupFrom, dupTo)) {
        ALOGE("Could not create child");
        close(outFd);
        return false;
    }
    const struct timeval timeout = {300, 0};
    if (!sitter.watchChild(timeout)) {
        sitter.killChild();
    }
    int result = sitter.waitChildExitStatus();
    close(outFd);
    return 0 == result ? true : false;
}

static void
dropRoot()
{
    if (0 != getuid())
        return;
    ALOGI("Dropping from root user to log user\n");
    prctl(PR_SET_KEEPCAPS, 1, 0, 0, 0);
    gid_t groups[] = {
        // AID_DIAG, to have read access to system logs hidden from the adb shell user.
        AID_DIAG,
        // AID_SDCARD_R, to have read access to logs in /storage/sdcard0.
        AID_SDCARD_R,
        // AID_SDCARD_RW, to have write access to logs in /storage/sdcard0.
        AID_SDCARD_RW,
        // AID_MEDIA_RW, to have read access to logs in /storage/sdcard{1-N}.
        AID_MEDIA_RW,
        // AID_INET, to allow network diagnostics
        AID_INET
    };
    if (-1 == setgroups(sizeof(groups)/sizeof(groups[0]), groups)) {
        ALOGE("setgroups() failed; errno=%d", errno);
        exit(1);
    }
    if (-1 == setgid(AID_LOG)) {
        ALOGE("setgid() failed; errno=%d", errno);
        exit(1);
    }
    if (-1 == setuid(AID_LOG)) {
        ALOGE("setuid() failed; errno=%d", errno);
        exit(1);
    }
    struct __user_cap_header_struct capHeader;
    struct __user_cap_data_struct capData[2];
    memset(&capHeader, 0, sizeof(capHeader));
    memset(&capData, 0, sizeof(capData));
    capHeader.version = _LINUX_CAPABILITY_VERSION_3;
    capHeader.pid = 0;
    capData[CAP_TO_INDEX(CAP_SYSLOG)].permitted = CAP_TO_MASK(CAP_SYSLOG);
    capData[CAP_TO_INDEX(CAP_SYSLOG)].effective = CAP_TO_MASK(CAP_SYSLOG);
    capData[0].inheritable = 0;
    capData[1].inheritable = 0;
    if (-1 == capset(&capHeader, &capData[0])) {
        ALOGE("capset() failed; errno=%d", errno);
        exit(1);
    }
}


// Save the report info into a special file, so that the app can discover
// reports on disk even when it failed to receive/process the intent for the
// same (possibly due to a system lockup).  Once processed, the app should
// remove this file.
static void
saveReportInfo(const char* reportId, const char* reportDirPath, const char* screenshotFilePath,
        char* reportInfoFilePath)
{
    // The name of the file should be the same as the folder + the .info
    // extension.  We can avoid building a new string and reuse reportDirPath
    // as long as it doesn't end with a '/'.
    snprintf(reportInfoFilePath, PATH_MAX, "%s.info", reportDirPath);
    int infoFileFd = -1;
    FILE* infoFile = NULL;
    infoFileFd = open(reportInfoFilePath, O_CREAT|O_WRONLY|O_TRUNC|O_NOFOLLOW,
            S_IRUSR|S_IWUSR|S_IRGRP /* 0640 */);
    if (-1 == infoFileFd) {
        ALOGE("open(%s) failed; errno=%s", reportInfoFilePath, strerror(errno));
        goto cleanup;
    }
    fchown(infoFileFd, AID_LOG, AID_LOG);
    // TODO I have no idea why, but the use of fdopen() here results in fputs()
    // TODO failing silently below.
    infoFile = fopen(reportInfoFilePath, "w");
    if (!infoFile) {
        ALOGE("fopen(%s) failed; errno=%s", reportInfoFilePath, strerror(errno));
        goto cleanup;
    }
    // Allocate enough space for our key plus a full path.
    char lineBuf[256 + PATH_MAX];
    // Store the version of this executable.
    snprintf(lineBuf, sizeof(lineBuf), "version=%s\n", VERSION);
    if (EOF == fputs(lineBuf, infoFile)) {
        ALOGE("fputs() failed");
        goto cleanup;
    }
    // Store the report ID.
    snprintf(lineBuf, sizeof(lineBuf), "timestamp=%s\n", reportId);
    if (EOF == fputs(lineBuf, infoFile)) {
        ALOGE("fputs() failed");
        goto cleanup;
    }
    // Store the path to the report.
    snprintf(lineBuf, sizeof(lineBuf), "files=%s\n", reportDirPath);
    if (EOF == fputs(lineBuf, infoFile)) {
        ALOGE("fputs() failed");
        goto cleanup;
    }
    // Store the path to the screenshot.
    // TODO Once this is stored under reportDirPath, make this a relative path.
    snprintf(lineBuf, sizeof(lineBuf), "screenshot=%s\n", screenshotFilePath);
    if (EOF == fputs(lineBuf, infoFile)) {
        ALOGE("fputs() failed");
        goto cleanup;
    }
    // Store the path to this info file, so that it may be deleted.
    // TODO Quit writing this to the info file, and instead update the app to
    // always delete this file.
    snprintf(lineBuf, sizeof(lineBuf), "files_to_remove=%s\n", reportInfoFilePath);
    if (EOF == fputs(lineBuf, infoFile)) {
        ALOGE("fputs() failed");
        goto cleanup;
    }
    ALOGI("Saved report info to %s", reportInfoFilePath);
cleanup:
    if (-1 != infoFileFd) {
        close(infoFileFd);
    }
    if (infoFile) {
        fclose(infoFile);
    }
}

static bool
isAppInstalled()
{
    struct stat statBuf;
    return (0 == stat("/data/data/com.qiku.bug_report", &statBuf) && S_ISDIR(statBuf.st_mode));
}

static int
runBugMailer()
{
    ALOGI("bugmailer.sh started");
    Babysitter sitter(LOG_TAG, LOG_PRI_WARN);
    sitter.setOrphanKillSignal(SIGKILL);
    char* const argv[] = {
            (char*)"bugmailer.sh",
            (char*)NULL};
    int dupFrom[] = {0, 1, 2, -1};
    int dupTo[] =   {0, 1, 2, -1};
    if (-1 == sitter.createChild("bugmailer.sh", argv, dupFrom, dupTo)) {
        ALOGE("Could not create child");
        return false;
    }
    const struct timeval timeout = {300, 0};
    if (!sitter.watchChild(timeout)) {
        sitter.killChild();
    }
    int result = sitter.waitChildExitStatus();
    ALOGI("bugmailer.sh finished");
    return result;
}

int
main(int argc, char* argv[])
{
    // Determine whether our app is installed.  If not, we'll run Android's
    // basic SendBug instead.
    if (!isAppInstalled()) {
        exit(runBugMailer());
    }
    // Determine whether we should run at all.
    if (!shouldRun()) {
        ALOGW("Ignoring bugreport keypress");
        exit(0);
    }
    ALOGI("bugreport started");
    // Warn the caller if not root.
    if (0 != getuid()) {
        ALOGW("Running as non-root user; this may not work properly");
    }
    // Loosen up the umask to allow group read and execute access by default.
    // We always want the AID_LOG group to be able to read our output.
    umask(S_IWGRP|S_IRWXO /* 0027 */);
    // Allocate a unique ID for this report.
    char reportId[REPORT_ID_MAX];
    createReportId(reportId);
    // Ensure the root report folder exists.
    if (-1 == mkdir(ROOT_REPORT_DIR_PATH, S_IRWXU|S_IRGRP|S_IXGRP /* 0750 */) && EEXIST != errno) {
        ALOGE("Couldn't create "ROOT_REPORT_DIR_PATH"; errno=%s; aborting", strerror(errno));
        sendErrorIntent(reportId, "nostorage");
        exit(1);
    }
    lchown(ROOT_REPORT_DIR_PATH, AID_LOG, AID_LOG);
    // Ensure there's enough room to store the report.
    if (getAvailableSpaceMb(ROOT_REPORT_DIR_PATH) < getRequiredSpace()) {
        ALOGE("Not enough space in "ROOT_REPORT_DIR_PATH" for report; aborting");
        sendErrorIntent(reportId, "nostorage");
        exit(1);
    }
    // Create the report folder.
    char reportDirPath[PATH_MAX];
    snprintf(reportDirPath, PATH_MAX, "%s/user@%s", ROOT_REPORT_DIR_PATH, reportId);
    if (-1 == mkdir(reportDirPath, S_IRWXU|S_IRGRP|S_IXGRP /* 0750 */) && EEXIST != errno) {
        ALOGE("Couldn't create %s; errno=%s; aborting", reportDirPath, strerror(errno));
        sendErrorIntent(reportId, "nostorage");
        exit(1);
    }
    lchown(reportDirPath, AID_LOG, AID_LOG);
    // Create the screenshot folder.
    // We must store the screenshot outside the reportDirPath, because the app
    // copies it into the reportDirPath if the user chooses to attach it, and will
    // remove it from reportDirPath if the user changes his/her mind.
    // TODO Move the screenshot into the reportDirPath, and update the app to
    // defer the removal of the file until the user presses Send.
    if (-1 == mkdir(SCREENSHOT_DIR_PATH, S_IRWXU|S_IRGRP|S_IXGRP /* 0750 */) && EEXIST != errno) {
        ALOGE("Couldn't create %s; errno=%s; aborting", SCREENSHOT_DIR_PATH, strerror(errno));
        sendErrorIntent(reportId, "nostorage");
        exit(1);
    }
    lchown(SCREENSHOT_DIR_PATH, AID_LOG, AID_LOG);
    ALOGI("Storing report to %s", reportDirPath);
    // Give the user vibrational feedback that the report is being created.
    //vibrate("150");
    // Capture the screenshot before we do anything that affects the GUI, and
    // before we drop root, because it won't work without root.
    char screenshotFilePath[PATH_MAX];
    snprintf(screenshotFilePath, PATH_MAX, "%s/screenshot-%s.png", SCREENSHOT_DIR_PATH,
            reportId);
    captureScreenshot(screenshotFilePath);
    // Notify the user via the GUI that the report is being created.
    sendStartIntent(reportId, reportDirPath);
    // Run dumpstate prior to dropping root, because it won't work fully
    // without root.  After dropping root, we'd still be able to run dumpstate,
    // but only by calling bugreport to connect to the singleton dumpstate init
    // service.  It's better to call it directly, because we can run in
    // parallel with a DEAM thread that calls bugreport without conflict.
    char dumpstateFilePath[PATH_MAX];
    snprintf(dumpstateFilePath, PATH_MAX, "%s/bugreport-%s.txt", reportDirPath, reportId);
    runDumpstate(dumpstateFilePath);
    // Save report info to a file for the app to retrieve.
    char reportInfoFilePath[PATH_MAX];
    saveReportInfo(reportId, reportDirPath, screenshotFilePath, reportInfoFilePath);
    // Notify the user that the report is now complete.
    for (int i = 0; i < 3; i++) {
        //vibrate("75");
        usleep((75 + 50) * 1000);
    }
    sendEndIntent(reportId, reportDirPath, reportInfoFilePath);
    ALOGI("bugreport finished");
    return 0;
}

#include <sys/wait.h>
#include <sys/stat.h>
#include <signal.h>
#include <fcntl.h>
#include <dirent.h>
#include <sys/prctl.h>
#include <string.h>
#include <errno.h>

#include "Utils.h"
#include "Reaper.h"
#include "Killer.h"
#include "Babysitter.h"
using namespace NsBabysitter;

Babysitter::Babysitter()
{
    MYLOGENTER();
    init();
    MYLOGEXIT();
}

Babysitter::Babysitter(const char* logTag)
{
    Utils::mLogTag = logTag;
    MYLOGENTER();
    init();
    MYLOGEXIT();
}

Babysitter::Babysitter(const char* logTag, LOG_PRI_T minLogPri)
{
    Utils::mLogTag = logTag;
    Utils::mMinLogPri = minLogPri;
    MYLOGENTER();
    init();
    MYLOGEXIT();
}

void
Babysitter::init()
{
    MYLOGENTER();
    mChildPid = -1;
    mKillerFromReaperFd = -1;
    mUserFromReaperFd = -1;
    mKillerToUserFd = -1;
    mIsChildUmaskSet = false;
    mChildUmask = 0;
    mOrphanKillSignal = SIGKILL;
    mShouldKeepAndroidPropFd = true;

    // We must ignore SIGPIPE, otherwise we risk killing the user process when
    // writing to broken pipes.  There are legitimate cases where we do write
    // to broken pipes, so we must do this.  The downside here is that we are
    // now setting the signal disposition for the entire user process, which
    // will interfere with users who are actively handling this signal.  Due to
    // this, Babysitter SHOULD NOT BE USED by any processes that want to do
    // anything other than ignore SIGPIPE.
    struct sigaction sa;
    sa.sa_handler = SIG_IGN;
    if (-1 == sigaction(SIGPIPE, &sa, NULL))
        PANIC("sigaction() failed; errno=%d", errno);
    MYLOGEXIT();
}

Babysitter::~Babysitter()
{
    MYLOGENTER();
    if (-1 != mKillerFromReaperFd) {
        MYLOGV("close(mKillerFromReaperFd=%d)", mKillerFromReaperFd);
        close(mKillerFromReaperFd);
        mKillerFromReaperFd = -1;
    }
    if (-1 != mUserFromReaperFd) {
        MYLOGV("close(mUserFromReaperFd=%d)", mUserFromReaperFd);
        close(mUserFromReaperFd);
        mUserFromReaperFd = -1;
    }
    if (-1 != mKillerToUserFd) {
        MYLOGV("close(mKillerToUserFd=%d)", mKillerToUserFd);
        close(mKillerToUserFd);
        mKillerToUserFd = -1;
    }
    MYLOGEXIT();
}

// This method will fork-exec the specified child with the specified args, and
// will close all inherited file descriptors from the parent except those
// explicitly preserved in dupFrom/dupTo.  It will return a file descriptor
// (userFromReaperFd) which the user process may optionally use to monitor the status
// of the child.  To monitor the child status, the caller should select() on
// userFromReaperFd for when data becomes available, but the user should not actually
// read from it.  Alternatively, the user can simply call watchChild() and the
// Babysitter will watch the child status.  The only reason the user should
// monitor the child directly by selecting on userFromReaperFd is to avoid blocking
// the user thread while waiting for the child, which is what watchChild() will
// do.  The user should NOT close userFromReaperFd; it's only borrowed from the
// Babysitter.
//
// The caller must be smart when deciding the order of the dupFrom and dupTo
// arrays.  dup2() will close the target FD if it's already open, which can
// cause problems if you pick the wrong ordering.
// Example - The caller has the read end of a socket which needs to be
//      redirected to stdout and stderr of the child.  Also, the caller wants to
//      redirect stdout to the stdin of the child.  With the ordering below,
//      the caller's stdin will be closed on the first call to dup2().  On the
//      third call to dup2(), there will be no more stdout to duplicate onto the
//      child's stdin.
//      Problematic Ordering:
//          4 --> 1
//          4 --> 2
//          1 --> 0
//      Corrected Ordering:
//          1 --> 0
//          4 --> 1
//          4 --> 2
int
Babysitter::createChild(const char* path, char* const argv[],
        const int dupFrom[], const int dupTo[])
{
    MYLOGENTER();
    if (-1 != getChildPid()) {
        MYLOGE("Invalid attempt to reuse Babysitter on another child");
        MYLOGEXIT();
        return (-1);
    }
    MYLOGI("Running %s", path);
    int j = 0;
    while (argv[j]) {
        MYLOGI("argv[%d]=%s", j, argv[j]);
        j++;
    }
    pid_t pid = fork();
    if(pid < 0)
    {
        MYLOGE("fork() failed; errno=%d", errno);
        MYLOGEXIT();
        return (-1);
    }
    else if(pid == 0) // Child
    {
        if (-1 != getOrphanKillSignal()) {
            // This Linux-only command will cause the specified signal to
            // go to the child if the parent dies.  This ensures that the
            // child will be cleaned up if the parent crashes.  Use
            // SIGKILL since the parent won't be around to make sure the
            // child actually dies from a softer signal.
            prctl(PR_SET_PDEATHSIG, getOrphanKillSignal(), 0, 0, 0);
        }
        if (isChildUmaskSet()) {
            mode_t oldUmask = umask(getChildUmask());
            MYLOGD("oldUmask=%d", oldUmask);
        }
        if (dupFrom && dupTo) {
            for (int i = 0; -1 != dupFrom[i]; i++) {
                // The lists must be equal length, with -1 terminating them.
                if (-1 == dupTo[i])
                    PANIC("dupFrom[%d] does not match dupTo[%d]", i, i);
                MYLOGV("Duplicating fd=%d to fd=%d", dupFrom[i], dupTo[i]);
                while (-1 == dup2(dupFrom[i], dupTo[i])) {
                    if (EINTR == errno)
                        MYLOGW("Got EINTR; retrying dup2()");
                    else
                        PANIC("dup2() failed; errno=%d", errno);
                }
            }
        }
        closeAllFds(dupTo);
        // Static analysis tools like Klocwork will complain about the usage
        // of execvp() instead of execv(), because execvp() will duplicate the
        // shell's behavior for doing path lookups.  Just ignore them.  It's up
        // to the user of Babysitter to specify a full path when appropriate to
        // make the code secure.  See "man 3 exec" for more details.
        execvp(path, argv);
        PANIC("execvp() failed; errno=%d", errno);
        // No need to free argv.  It gets cleaned up when execvp() is called.
    }
    MYLOGI("childPid=%d created", pid);
    setChildPid(pid);
    if (false == setupChildWatchers(pid)) {
        // We cannot proceed normally without our watchers, so bail out early.
        // We have to create these after the child is forked, because the
        // Reaper requires the child's pid to start.
        // Make an effort to kill the child, but we cannot monitor that it
        // actually dies without our watchers.
        kill(pid, SIGKILL);
        MYLOGEXIT();
        return (-1);
    }
    MYLOGEXIT();
    return (getUserFromReaperFd());
}

// This method calls the base createChild() method above, but will parse a
// single string of args from the caller and build argv from it.
int
Babysitter::createChild(const char* path, const char* name, const char* args,
        const int dupFrom[], const int dupTo[])
{
    MYLOGENTER();
    char** argv = buildArgv(name, args);
    if (!argv) {
        MYLOGE("buildArgv() failed");
        return (-1);
    }
    int userFromReaperFd = createChild(path, argv, dupFrom, dupTo);
    freeArgv(argv);
    MYLOGEXIT();
    return (userFromReaperFd);
}

// This watchChild() should be called by users that want to run the child
// with no execTimeout.  For full details on behavior, see the comments on the
// called method below.
bool
Babysitter::watchChild()
{
    MYLOGENTER();
    return (watchChild(NULL));
    MYLOGEXIT();
}

// This watchChild() should be called by users that want to run the child
// with an execTimeout.  For full details on behavior, see the comments on the
// called method below.
bool
Babysitter::watchChild(timeval execTimeout)
{
    MYLOGENTER();
    MYLOGI("Waiting %ld.%06ld secs for childPid=%d",
            execTimeout.tv_sec, execTimeout.tv_usec, getChildPid());
    return (watchChild(&execTimeout));
    MYLOGEXIT();
}

// This will block the user's thread until the child dies on its own, select()
// times out (only possible if user passes non-NULL execTimeout), or we
// encounter an error monitoring the child.
//
// If this method returns true, then the child is dead, and the user can
// proceed to call waitChildExitStatus() or destruct the Babysitter.
//
// If this method returns false, then the child is probably not dead, and the
// user should call killChild() if the user wants to ensure the child dies.
//
// If the user doesn't care whether the child dies or not, and doesn't want to
// call waitChildExitStatus(), then the user can proceed to destruct the
// Babysitter without calling this method at all.
bool
Babysitter::watchChild(timeval* execTimeout)
{
    MYLOGENTER();
    if (-1 == getUserFromReaperFd()) {
        MYLOGE("Invalid attempt to watch an unwatchable child");
        return (false);
    }
    fd_set masterFds;
    FD_ZERO(&masterFds);
    fd_set readFds;
    FD_ZERO(&readFds);
    FD_SET(getUserFromReaperFd(), &masterFds);
    bool isChildDead = false;
    while (1) {
        errno = 0;
        readFds = masterFds;
        int numFdsSet = select(getUserFromReaperFd() + 1, &readFds, NULL, NULL, execTimeout);
        if ((-1 == numFdsSet) && (EINTR == errno)) {
            if (EINTR == errno) {
                MYLOGW("Got EINTR");
                continue;
            } else {
                // Treat any errors while reading to mean the child should be killed.
                break;
            }
        } else if (0 == numFdsSet) {
            MYLOGE("Timed out waiting for childPid=%d to exit on its own", getChildPid());
            break;
        } else {
            if (FD_ISSET(getUserFromReaperFd(), &readFds)) {
                MYLOGD("User got notif from Reaper or Killer; childPid=%d", getChildPid());
                isChildDead = true;
                break;
            }
        }
    }
    MYLOGEXIT();
    return (isChildDead);
}

// This is the simple version of killChild().  It uses a general-purpose
// killSequence for users that do not need exact control over the signals
// sent to the child.  If you are trying to kill a child that ignores any
// of the signals in the default below, then you should specify your own
// killSequence via the overloaded killChild() method further below.
void
Babysitter::killChild()
{
    MYLOGENTER();
    const KILL_PROFILE_T killProfiles[] = {
        {"SOFT",    SIGTERM,    {2, 0}}, // Ask nicely
        {"HARD",    SIGQUIT,    {2, 0}}, // More forceful
        {"FINAL",   SIGKILL,    {2, 0}}, // No more Mr. Nice Guy
        {NULL,      0,          {0, 0}}
    };
    killChild(killProfiles);
    MYLOGEXIT();
}

// The caller must specify a KILL_PROFILE_T array which tell this method the
// sequence of kill signals and timeouts for each.  Typically, the final
// signal should be SIGKILL, to ensure the child dies.  SIGKILL should not
// usually be the first signal in the sequence, so that the child has a chance
// to catch a softer signal before being forced to exit with no cleanup.  The
// list must be terminated with a NULL profile, so that we know when we've
// reach the end of the list.
//
// Example:
// const KILL_PROFILE_T killProfiles[] = {
//     {"SOFT",    SIGHUP,     {1, 0}},
//     {"FINAL",   SIGKILL,    {5, 0}},
//     {NULL,      0,          {0, 0}}
// };
void
Babysitter::killChild(const KILL_PROFILE_T* killProfiles)
{
    MYLOGENTER();
    if (!killProfiles)
        PANIC("killProfiles is NULL");
    if (!killProfiles->name)
        PANIC("killProfiles is empty");

    Killer::kill(getChildPid(), killProfiles, getKillerFromReaperFd(), getKillerToUserFd());
    // The FDs belong to the Killer now.  We "forget" them so we
    // won't try to close them later.  Even if the thread startup fails, the above
    // call will make sure to close them.  This code deliberately does not worry
    // about the above call failing.
    setKillerToUserFd(-1);
    setKillerFromReaperFd(-1);
    // After we return, the caller can choose to destroy the Babysitter object
    // and forget about the child, or continue to listen on the userFromReaperFd to
    // know when the child dies.
    MYLOGEXIT();
}

// We pass the childPid directly into this method even though we could get it
// from a call to getChildPid(), because we want to emphasize that it must
// be available at the time this method is called.  This means we cannot call
// this method prior to forking the child.
bool
Babysitter::setupChildWatchers(int childPid)
{
    MYLOGENTER();
    int reaperToUserPipe[2] = {-1, -1};
    int killerToUserFd = -1;
    int reaperToKillerPipe[2] = {-1, -1};
    int pipeFlags;
    // Create a pipe so the Reaper can signal the User when the
    // child dies.
    if (-1 == pipe(reaperToUserPipe)) {
        MYLOGE("pipe() failed; errno=%d", errno);
        goto reaperToUserPipeErr;
    }
    MYLOGV("userFromReaperFd=%d", reaperToUserPipe[0]);
    MYLOGV("reaperToUserFd=%d", reaperToUserPipe[1]);
    // Get an extra handle to the write end of the reaperToUserPipe, so that the Killer
    // can signal the User when it encounters an unkillable child.  This is
    // needed to keep the User from blocking forever waiting for the exitStatus
    // from the Reaper.
    //
    // Since writes to a pipe of size less than PIPE_BUF must be atomic
    // (see "man 7 pipe), and we are writing way less than PIPE_BUF on any
    // sane system, we don't need to worry about a collision where the Killer
    // and Reaper both write to the pipe at the same time.
    if (-1 == (killerToUserFd = dup(reaperToUserPipe[1]))) {
        MYLOGE("dup() failed; errno=%d", errno);
        goto killerToUserDupErr;
    }
    MYLOGV("killerToUserFd=%d", killerToUserFd);
    // Create a pipe so the Reaper can signal the Killer when the child dies.
    // We must create the reaperToKillerPipe now, even though the Killer
    // is not running yet, because the Reaper needs to know the write FD
    // when it starts.  We cannot safely pass data to Reaper after
    // creation.
    if (-1 == pipe(reaperToKillerPipe)) {
        MYLOGE("pipe() failed; errno=%d", errno);
        goto reaperToKillerPipeErr;
    }
    MYLOGV("killerFromReaperFd=%d", reaperToKillerPipe[0]);
    MYLOGV("reaperToKillerFd=%d", reaperToKillerPipe[1]);
    // The Killer will only read from Reaper in non-blocking mode
    if (0 > (pipeFlags = fcntl(reaperToKillerPipe[0], F_GETFL, 0)))
        goto setNonblockErr;
    if (0 > fcntl(reaperToKillerPipe[0], F_SETFL, pipeFlags | O_NONBLOCK))
        goto setNonblockErr;
    if (!Reaper::start(childPid, reaperToUserPipe[1], reaperToKillerPipe[1])) {
        goto threadErr;
    }
    setKillerFromReaperFd(reaperToKillerPipe[0]);
    setUserFromReaperFd(reaperToUserPipe[0]);
    setKillerToUserFd(killerToUserFd);
    MYLOGEXIT();
    return (true);
threadErr:
setNonblockErr:
    close(reaperToKillerPipe[0]);
    close(reaperToKillerPipe[1]);
reaperToKillerPipeErr:
    close(killerToUserFd);
killerToUserDupErr:
    close(reaperToUserPipe[0]);
    close(reaperToUserPipe[1]);
reaperToUserPipeErr:
    MYLOGEXIT();
    return (false);
}

// The user MUST NOT call this method if the user has already read from the
// userFromReaperFd returned by createChild().  The user is intended to only select()
// on the userFromReaperFd so that it knows when it should call this method.
int
Babysitter::waitChildExitStatus()
{
    MYLOGENTER();
    if (-1 == getUserFromReaperFd()) {
        MYLOGE("Invalid attempt to wait with non-existent child watcher");
        MYLOGEXIT();
        return (-1);
    }
    MYLOGD("User waiting for Reaper; childPid=%d", getChildPid());
    int numRead;
    char readBuf[4] = {0}; // 0-255 + '\0'
    // TODO: partial read handling
    // TODO: select here too to avoid blocking forever?
    do {
        numRead = read(getUserFromReaperFd(), readBuf, sizeof(readBuf) - 1);
        if (-1 == numRead)
            MYLOGE("read() failed; errno=%d", errno);
    } while ((-1 == numRead) && (EINTR == errno));

    int exitStatus = -1;
    if (0 == strlen(readBuf)) {
        // This would happen if the Reaper or Killer close the pipe without
        // writing to it.
        MYLOGW("User got empty exitStatus from Reaper or Killer; childPid=%d", getChildPid());
    } else {
        // 0 is a legitimate return value for strtol, so we must clear errno before
        // calling strtol() in order to detect errors.  We still might not catch
        // the case where readBuf contains no digits, because it's implementation-
        // dependent whether strtol() will set errno to EINVAL in that case.
        errno = 0;
        int tmpExitStatus = strtol(readBuf, (char**)NULL, 10);
        if ((0 == errno) && (-1 <= tmpExitStatus) && (255 >= tmpExitStatus)) {
            exitStatus = tmpExitStatus;
            MYLOGI("User got exitStatus=%d from Reaper; childPid=%d", exitStatus, getChildPid());
        } else {
            MYLOGE("User got invalid exitStatus=\"%s\" from Reaper or Killer; childPid=%d",
                    readBuf, getChildPid());
        }
    }
    return (exitStatus);
    MYLOGEXIT();
}

char**
Babysitter::buildArgv(const char* name, const char* args)const
{
    MYLOGENTER();
    char* myArgs = NULL;
    int numTokens = 0;
    char** argv;

    if (args) {
        // We must copy the args, because strtok_r will modify the string.
        myArgs = strdup(args);
        if (!myArgs) {
            MYLOGE("strdup() failed; errno=%d", errno);
            goto myArgsStrdupErr;
        }
    }

    if (myArgs) {
        // strtok_r will go through the args and put a '\0' in place of every
        // space.  Doing this step allows us to count how much memory we need
        // to allocate for argv, and also helps with the final copying of the
        // tokens into argv.
        const char delims[] = " ";
        char* nextToken;
        char* token = strtok_r(myArgs, delims, &nextToken);
        for (; token; numTokens++) {
            MYLOGV("Parsed %s from params", token);
            token = strtok_r(NULL, delims, &nextToken);
        }
    }
    MYLOGV("numTokens=%d", numTokens);

    // Make extra room for arg0 and NULL
    argv = (char**)malloc((numTokens + 2) * sizeof(char*));
    if (!argv) {
        MYLOGE("malloc() failed");
        goto argvMallocErr;
    }
    // NULL-terminate the list
    argv[numTokens + 1] = (char*)NULL;

    // Populate argv
    argv[0] = strdup(name);
    if (!argv[0]) {
        MYLOGE("strdup() failed");
        goto argvStrdupErr;
    }
    if (myArgs) {
        char* token = myArgs;
        int i;
        for (i = 1; i <= numTokens; i++) {
            MYLOGV("token %d: \"%s\"", i, token);
            argv[i] = strdup(token);
            if (NULL == argv[i]) {
                MYLOGE("strdup() failed; errno=%d", errno);
                break;
            }
            token += strlen(token) + 1;  // get the next token by skipping past the '\0'
            token += strspn(token, ","); // then skipping any starting delimiters
        }
        // If we couldn't allocate all the tokens, free the ones we did already.
        if (i <= numTokens) {
            MYLOGE("strdup() failed");
            goto argvStrdupErr;
        }
        free(myArgs);
        myArgs = NULL;
    }

    MYLOGEXIT();
    return argv;
argvStrdupErr:
    freeArgv(argv);
    argv = NULL;
argvMallocErr:
    free(myArgs);
    myArgs = NULL;
myArgsStrdupErr:
    MYLOGEXIT();
    return NULL;
}

void
Babysitter::freeArgv(char** argv)const
{
    MYLOGENTER();
    int i = 0;
    while (argv[i]) {
        MYLOGV("Freeing argv[%d]", i);
        free(argv[i]);
        i++;
    }
    MYLOGV("Freeing argv");
    free(argv);
    MYLOGEXIT();
}

void
Babysitter::closeAllFds(const int exceptFds[])const
{
    MYLOGENTER();
    DIR *dir;
    struct dirent *dirent;
    if (NULL == (dir = opendir("/proc/self/fd"))) {
        PANIC("open(/proc/self/fd) failed; errno=%d", errno);
    }
    while (NULL != (dirent = readdir(dir))) {
        if (('0' <= dirent->d_name[0]) || ('9' >= dirent->d_name[0])) {
            int fd = atoi(dirent->d_name);
#ifdef ANDROID
            // If the user wants the child to be able to access the Android
            // Property system, then we must preserve the FD for it.  This
            // should probably be done explicitly by the user via dupFrom/dupTo,
            // but it's a very odd quirk of Android that the property library
            // doesn't handle this automatically for child processes, and
            // instead puts the burden on the caller of fork-exec to know to
            // preserve it.  I don't think most users of Babysitter will have
            // a clue why children that rely on the property library aren't
            // working properly, so they won't know to explicitly preserve the
            // FD.  It's going to be much simpler to just handle it here.
            if (shouldKeepAndroidPropFd() && (getAndroidPropFd() == fd))
                continue;
#endif // ANDROID
            if (fd != dirfd(dir)) {
                bool isMatch = false;
                if (exceptFds) {
                    for (int i = 0; -1 != exceptFds[i]; i++) {
                        if (exceptFds[i] == fd) {
                            MYLOGV("Preserving fd=%d", fd);
                            isMatch = true;
                            break;
                        }
                    }
                }
                if (!isMatch) {
                    MYLOGV("Closing fd=%d", fd);
                    close(fd);
                }
            }
        }
    }
    closedir(dir);
    MYLOGEXIT();
}

int
Babysitter::getAndroidPropFd()const
{
    char* propWorkspace = getenv("ANDROID_PROPERTY_WORKSPACE");
    if (NULL == propWorkspace) {
        MYLOGE("Could not find ANDROID_PROPERTY_WORKSPACE");
        return -1;
    }
    errno = 0;
    int propFd = strtol(propWorkspace, (char**)NULL, 10);
    if ((0 != errno) || (0 >= propFd)) {
        return -1;
    }
    return propFd;
}

bool
Babysitter::nSleep(struct timespec time)
{
    struct timespec remainder;
    while (-1 == nanosleep(&time, &remainder)) {
        if (EINTR == errno) {
            MYLOGD("nanosleep() got EINTR");
            time = remainder;
        } else {
            MYLOGE("nanosleep() failed; errno=%d", errno);
            return false;
        }
    }
    return true;
}

bool
Babysitter::mSleep(unsigned long msec)
{
    struct timespec requested;
    time_t sec = (long)(msec/1000);
    msec = msec - (sec*1000);
    requested.tv_sec = sec;
    requested.tv_nsec = (msec*1000000L);
    return nSleep(requested);
}

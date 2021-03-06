#include <sys/wait.h>
#include <signal.h>
#include <errno.h>
#include "Utils.h"
#include "Reaper.h"
using namespace NsBabysitter;

namespace NsBabysitter {
void* reaperThread(void* params);
} // namespace

Reaper::Reaper(int childPid, int reaperToUserFd, int reaperToKillerFd)
  : mChildPid(childPid), mReaperToUserFd(reaperToUserFd), mReaperToKillerFd(reaperToKillerFd)
{
    MYLOGENTER();
    MYLOGEXIT();
}

Reaper::~Reaper()
{
    MYLOGENTER();
    // The User may be waiting for EOF, so generate it by closing the FD.  The
    // Killer may also be looking for the same thing to know when to quit trying
    // to kill the child.
    MYLOGV("close(reaperToUserFd=%d)", getReaperToUserFd());
    close(getReaperToUserFd());
    MYLOGV("close(reaperToKillerFd=%d)", getReaperToKillerFd());
    close(getReaperToKillerFd());
    MYLOGEXIT();
}

bool
Reaper::start(int childPid, int reaperToUserFd, int reaperToKillerFd)
{
    MYLOGENTER();
    Reaper* thread = new Reaper(childPid, reaperToUserFd, reaperToKillerFd);
    pthread_t pthread;
    pthread_attr_t pthreadAttr;
    if (!thread) {
        MYLOGE("new Reaper failed");
        goto allocErr;
    }
    if (0 != pthread_attr_init(&pthreadAttr)) {
        MYLOGE("pthread_attr_init() failed; errno=%d", errno);
        goto attrInitErr;
    }
    pthread_attr_setdetachstate(&pthreadAttr, PTHREAD_CREATE_DETACHED);
    if (0 != pthread_create(&pthread, &pthreadAttr, reaperThread, (void*)thread)) {
        MYLOGE("pthread_create() failed; errno=%d", errno);
        goto startErr;
    }
    pthread_attr_destroy(&pthreadAttr);
    MYLOGEXIT();
    return (true);
startErr:
    pthread_attr_destroy(&pthreadAttr);
attrInitErr:
    delete thread;
    thread = NULL;
allocErr:
    MYLOGEXIT();
    return (false);
}

namespace NsBabysitter {
void*
reaperThread(void* params)
{
    MYLOGENTER();
    MYLOGV("reaperThread is starting");
    if (!params)
        PANIC("reaperThread() params is NULL");
    Reaper* thread = (Reaper*)params;
    thread->run();
    delete thread;
    thread = NULL;
    MYLOGV("reaperThread is dying");
    MYLOGEXIT();
    pthread_exit(NULL);
    return (NULL);
}
} // namespace

void
Reaper::run()
{
    MYLOGENTER();
    int waitpidStatus;
    int reapedChildPid;
    int exitStatus = -1;
    // Make sure we retry if we get EINTR, which can happen due to unblocked signals.
    do {
        MYLOGD("Waiting to reap childPid=%d", getChildPid());
        reapedChildPid = waitpid(getChildPid(), &waitpidStatus, 0);
    } while ((-1 == reapedChildPid) && (EINTR == errno));

    if (-1 == reapedChildPid) {
        MYLOGE("waitpid(childPid=%d) failed; errno=%d", getChildPid(), errno);
    }
    else if (reapedChildPid != getChildPid()) {
        // This shouldn't be possible.
        MYLOGE("Reaped wrong child; reapedChildPid=%d, childPid=%d", reapedChildPid, getChildPid());
    } else {
        if (WIFEXITED(waitpidStatus)) {
            exitStatus = WEXITSTATUS(waitpidStatus);
            MYLOGI("childPid=%d is reaped; exitStatus=%d", reapedChildPid, exitStatus);
        } else if (WIFSIGNALED(waitpidStatus)) {
            MYLOGI("childPid=%d is reaped; killed by signal=%d",
                reapedChildPid, WTERMSIG(waitpidStatus));
                // When killed by a signal, the exit status would typically be
                // the signal number + 128.
                // http://www.slac.stanford.edu/BFROOT/www/Computing/Environment/Tools/Batch/exitcode.html
                exitStatus = WTERMSIG(waitpidStatus) + 128;
        } else {
            MYLOGE("Impossible reap; reapedChildPid=%d; waitpidStatus=%d",
                reapedChildPid, waitpidStatus);
        }
    }
    MYLOGD("Reaper writing exitStatus=%d to User", exitStatus);
    Utils::writeExitStatus(exitStatus, getReaperToUserFd());
    MYLOGEXIT();
}

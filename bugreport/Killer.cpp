#include <string.h>
#include <errno.h>
#include "Utils.h"
#include "Killer.h"
using namespace NsBabysitter;

namespace NsBabysitter {
void killerThread(sigval_t sigval);
} // namespace

void
Killer::kill(pid_t childPid, const KILL_PROFILE_T* killProfiles, int killerFromReaperFd,
        int killerToUserFd)
{
    MYLOGENTER();
    Killer* killer = new Killer(childPid, killerFromReaperFd, killerToUserFd);
    if (!killer) {
        MYLOGE("new Killer failed");
        // The killer cannot clean these up if we cannot allocate it.
        MYLOGV("close(killerFromReaperFd=%d)", killerFromReaperFd);
        close(killerFromReaperFd);
        killerFromReaperFd = -1;
        MYLOGV("close(killerToUserFd=%d)", killerToUserFd);
        close(killerToUserFd);
        killerToUserFd = -1;
        // We couldn't create the objects required to do this the nice way, so
        // do it the mean way.
        MYLOGE("kill(childPid=%d, signal=%d)", childPid, SIGKILL);
        ::kill(childPid, SIGKILL);
    } else if (killer->isChildDead()) {
        // The killer is already finished killing the child, so we don't need
        // it anymore.
        delete killer;
        killer = NULL;
    } else if (killer->start(killProfiles)) {
        delete killer;
        killer = NULL;
    } else {
        // We started the first profile successfully.
    }
    MYLOGEXIT();
}

Killer::Killer(pid_t childPid, int killerFromReaperFd, int killerToUserFd)
  : mChildPid(childPid), mProfileList(NULL), mKillerFromReaperFd(killerFromReaperFd),
        mKillerToUserFd(killerToUserFd), mDeathTimer(killerThread, (void*)this, "Death"),
        mIsChildDead(false)
{
    MYLOGENTER();
    MYLOGEXIT();
}

Killer::~Killer()
{
    MYLOGENTER();
    MYLOGV("close(mKillerFromReaperFd=%d)", mKillerFromReaperFd);
    close(mKillerFromReaperFd);
    mKillerFromReaperFd = -1;
    MYLOGV("close(mKillerToUserFd=%d)", mKillerToUserFd);
    close(mKillerToUserFd);
    mKillerToUserFd = -1;
    delete mProfileList;
    mProfileList = NULL;
    if (!mIsChildDead) {
        // If we're quitting before the child is actually dead, make one last
        // attempt to clean up before abandoning the child.
        MYLOGE("kill(childPid=%d, signal=%d)", getChildPid(), SIGKILL);
        ::kill(getChildPid(), SIGKILL);
    }
    MYLOGEXIT();
}

// Returns true if the Killer is done killing the child, and the Killer object
// should be deleted; false if we started a timer to keep trying.
bool
Killer::start(const KILL_PROFILE_T* killProfiles)
{
    MYLOGENTER();
    // Do all memory allocation in here, so that we can then proceed later
    // without worrying about allocation failures.
    if (!getDeathTimer().init()) {
        return true;
    }
    // We have to build our profileList from the array passed to us.  The linked
    // list is easier to deal with internally, while the array is easier for
    // user's to interface with, so we convert.
    mProfileList = new List();
    const KILL_PROFILE_T* kp = killProfiles;
    if (!mProfileList) {
        MYLOGE("new List failed");
        return true;
    }
    while (mProfileList && kp->name) {
        if (!mProfileList->insertLast(new KillProfile(kp->name, kp->signal, kp->timeout))) {
            MYLOGE("new KillProfile failed");
            return true;
        }
        ++kp;
    }
    if (mProfileList->isEmpty()) {
        MYLOGE("mProfileList is empty");
        return true;
    }

    // We're all initialized now, so start the timer and send the first kill
    // signal.
    KillProfile* profile = (KillProfile*)getProfileList()->getFirst();
    if (getDeathTimer().start(profile->getTimeout())) {
        // Child isn't dead, and we have a new timer running.
        MYLOGI("kill(childPid=%d, signal=%d); profile=%s",
                getChildPid(), profile->getSignal(), profile->getName());
        ::kill(getChildPid(), profile->getSignal());
    } else {
        // We couldn't start the timer.
        return true;
    }
    MYLOGEXIT();
    // We need this object to keep going, so return false.
    return false;
}

namespace NsBabysitter {
void
killerThread(sigval_t sigval)
{
    MYLOGENTER();
    MYLOGV("killerThread is starting");
    if (!sigval.sival_ptr)
        PANIC("killerThread() sigval.sival_ptr is NULL");
    Killer* killer = (Killer*)sigval.sival_ptr;
    // Killer::resume() returns true if it is done and should be deleted.
    if (killer->resume()) {
        delete killer;
        killer = NULL;
    }
    MYLOGV("killerThread is dying");
    MYLOGEXIT();
}
} // namespace

// Returns true if the Killer is done killing the child, and the Killer object
// should be deleted; false if we started a timer to keep trying.
bool
Killer::resume()
{
    MYLOGENTER();
    // Remove the profile we just ran from the list.
    getProfileList()->removeAndDelete(getProfileList()->getFirst());
    bool shouldDeleteMe = false;
    if (isChildDead()) {
        shouldDeleteMe = true;
    } else {
        if (getProfileList()->isEmpty()) {
            MYLOGW("No profiles left for killing; giving up");
            MYLOGD("Killer writing exitStatus=-1 to User");
            // We must signal the User here, because we cannot rely on
            // the Reaper to do so after we give up.  We're abandoning
            // the unkillable child.
            Utils::writeExitStatus(-1, getKillerToUserFd());
            shouldDeleteMe = true;
        } else {
            KillProfile* profile = (KillProfile*)getProfileList()->getFirst();
            if (getDeathTimer().start(profile->getTimeout())) {
                // Child isn't dead, and we have a new timer running.
                MYLOGI("kill(childPid=%d, signal=%d); profile=%s",
                        getChildPid(), profile->getSignal(), profile->getName());
                ::kill(getChildPid(), profile->getSignal());
            } else {
                shouldDeleteMe = true;
            }
        }
    }
    MYLOGEXIT();
    return (shouldDeleteMe);
}

bool
Killer::isChildDead()
{
    MYLOGENTER();
    char buffer[1]; // EOF is all that is ever received here
    int numRead;
    do {
        // This pipe is set to O_NONBLOCK, so this will always return right away.
        // Don't worry about partial reads, because we just want the EOF anyway.
        // We just want to quickly check whether the Reaper has signaled
        // us.  If it hasn't, we proceed to run the next profile.
        numRead = read(getKillerFromReaperFd(), buffer, sizeof(buffer));
        if ((-1 == numRead) && (EAGAIN != errno))
            MYLOGE("read() failed; errno=%d", errno);
    } while ((-1 == numRead) && (EINTR == errno));

    if ((-1 == numRead) && (EAGAIN == errno)) {
        // The Reaper has not signaled us yet
        MYLOGD("childPid=%d not dead yet", getChildPid());
    } else {
        // We interpret all other scenarios as death.  Just exit without
        // starting a new timer.
        MYLOGD("Killer got notif from Reaper; childPid=%d", getChildPid());
        mIsChildDead = true;
    }
    MYLOGEXIT();
    return (mIsChildDead);
}

Killer::KillProfile::KillProfile(const char* name, int signal, timespec timeout)
  : mName(strdup(name)), mSignal(signal), mTimeout(timeout)
{
    MYLOGENTER();
    if (!mName) {
        MYLOGE("strdup(%s) failed", name);
    }
    MYLOGEXIT();
}

Killer::KillProfile::~KillProfile()
{
    MYLOGENTER();
    if (mName) {
        free(mName);
    }
    MYLOGEXIT();
}

const char*
Killer::KillProfile::getName()const
{
    if (mName)
        return mName;
    else
        return "<unnamed>";
}

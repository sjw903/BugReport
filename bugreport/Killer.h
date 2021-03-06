#ifndef _NsBabysitter_Killer_h_
#define _NsBabysitter_Killer_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif
//--------------------------------------------------------------------------------------------------
// Description:
//  Implements a class which will guarantee that everything possible is done to kill a given child
//  process, so that the caller does not need to implement any retry logic of its own.  The caller
//  does not even need to bother waiting around for the child to die.  The only entry point to the
//  class from the caller is the kill() method, which simply starts the process, and does not
//  block.  The caller never interacts with an actual instance of the object.
//
//  First, it checks whether the given child is already dead.  If not, it sends a kill signal and
//  arms a POSIX timer.  When that timer expires, a SIGEV_THREAD thread will run.  The Killer
//  object itself is the only argument that's passed to the thread.  Each time the thread runs, it
//  will check whether the child is dead.  If it's not, it will again send a kill signal and arm
//  the timer.  It continues to do this until the "kill profile" passed by the caller is exhausted,
//  at which point the Killer gives up and abandons the child.
//
//  Just like ReaperThread, this object must never touch the Babysitter object.  Doing so would
//  create major synchronization problems, which I believe are not solvable.  The key to ensuring
//  everything gets cleaned up as it should, is to NOT cancel or delete the timer from any thread
//  other than the SIGEV_THREAD itself.  If we allowed the Babysitter main thread to cancel the
//  timer directly, we would have no deterministic way to ensure this object will not be accessed
//  after it's deleted; because even if the main thread canceled the timer, the underlying OS
//  signal which generates this callback thread may have already fired, and be ready to run.  So,
//  it's perfectly normal for this object to outlive the Babysitter object.  Once the sequence of
//  timers begin, the logic in here will ensure the object gets cleaned up independent from the
//  Babysitter.

#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include "Timer.h"
#include "Babysitter.h"

namespace NsBabysitter {
class Killer {
    friend void killerThread(sigval_t);

public:
    static void kill(pid_t childPid, const KILL_PROFILE_T* profiles, int killerFromReaperFd,
            int killerToUserFd);
    Killer(pid_t childPid, int killerFromReaperFd, int killerToUserFd);
    ~Killer();

private:
    class KillProfile : public ListNode {
    public:
        KillProfile(const char* name, int signal, timespec timeout);
        ~KillProfile();
        const char* getName()const;
        int getSignal()const {return mSignal;}
        timespec getTimeout()const {return mTimeout;}
    private:
        KillProfile(); // Hide default constructor
        char*           mName;
        int             mSignal;
        timespec        mTimeout;
    };

    Killer(); // Hide default constructor
    Killer(const Killer&); // Hide copy constructor
    Killer& operator=(const Killer&); // Hide assignment operator
    bool start(const KILL_PROFILE_T* profiles);
    bool resume();
    int getChildPid()const {return mChildPid;}
    List* getProfileList()const {return mProfileList;}
    int getKillerFromReaperFd()const {return mKillerFromReaperFd;}
    int getKillerToUserFd()const {return mKillerToUserFd;}
    Timer& getDeathTimer() {return mDeathTimer;}
    bool isChildDead();

    pid_t                   mChildPid;
    List*                   mProfileList;
    int                     mKillerFromReaperFd;
    int                     mKillerToUserFd;
    Timer                   mDeathTimer;
    bool                    mIsChildDead;
};
} // namespace
#endif // _NsBabysitter_Killer_h_

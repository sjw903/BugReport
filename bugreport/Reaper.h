#ifndef _NsBabysitter_Reaper_h_
#define _NsBabysitter_Reaper_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif
//--------------------------------------------------------------------------------------------------
// Description:
//  Implements a thread which reaps the child for the Babysitter, and notifies the User and
//  Killer once the child dies.
//
//  It's vital that reaperThread() does not touch the Babysitter object.  If it did, then we would
//  need to change the semantics of the Babysitter interface to require a "cleanup" phase, where
//  the user of the object must wait for reaperThread() to join with the main thread before it is
//  ok to destroy the Babysitter object.  Otherwise the memory for the object could be accessed
//  after it was already destructed.  So, it's perfectly ok for this object to outlive the
//  Babysitter object.  It will outlive the Babysitter if the Killer abandons an unkillable
//  child.
// TODO: Add a pthread_kill() from the Killer to the Reaper when abandoning an unkillable child.

#include <stdlib.h>
#include <pthread.h>

namespace NsBabysitter {
class Reaper {
    friend void* reaperThread(void*);

public:
    static bool start(int childPid, int reaperToUserFd, int reaperToKillerFd);
    Reaper(int childPid, int reaperToUserFd, int reaperToKillerFd);
    ~Reaper();
private:
    Reaper(); // Hide default constructor
    Reaper(const Reaper&); // Hide copy constructor
    Reaper& operator=(const Reaper&); // Hide assignment operator
    void run();
    int getChildPid() {return mChildPid;}
    int getReaperToUserFd() {return mReaperToUserFd;}
    int getReaperToKillerFd() {return mReaperToKillerFd;}

    int mChildPid;
    int mReaperToUserFd;
    int mReaperToKillerFd;
};
} // namespace
#endif // _NsBabysitter_Reaper_h_

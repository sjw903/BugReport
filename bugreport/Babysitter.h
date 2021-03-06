#ifndef _NsBabysitter_Babysitter_h_
#define _NsBabysitter_Babysitter_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif
//--------------------------------------------------------------------------------------------------
// Description:
//  Implements an easy-to-use object-oriented library for managing a child process from birth
//  to death.  A user of this class may have 1-N instances of this object, for managing 1-N
//  different children.
// Principles:
//  - Crash the process deliberately for any obvious interface misuse by the user
//  - Return error codes to user for system failures/outages
//  - Guarantee that the child will die when killed
//  - The user must use a wake lock if doesn't want this code to sleep
//  - Don't block the user's thread unless asked explicitly to do so
//  - Only one babysitter per child; no reusing the same object for multiple
// TODO: Create special log macros to print childPid all the time

#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include "List.h"
#include "CommonDefs.h"

namespace NsBabysitter {
typedef struct {
    const char*     name;
    int             signal;
    timespec        timeout;
} KILL_PROFILE_T;

class Babysitter {
public:
    Babysitter();
    Babysitter(const char* logTag);
    Babysitter(const char* logTag, LOG_PRI_T minLogPri);
    ~Babysitter();
    int createChild(const char* path, const char* name, const char* args,
            const int dupFrom[] = NULL, const int dupTo[] = NULL);
    int createChild(const char* path, char* const argv[],
            const int dupFrom[] = NULL, const int dupTo[] = NULL);
    bool watchChild();
    bool watchChild(timeval execTimeout);
    void killChild();
    void killChild(const KILL_PROFILE_T* killProfiles);
    int waitChildExitStatus();
    void setChildUmask(mode_t mask){mChildUmask = mask; mIsChildUmaskSet = true;}
    int getOrphanKillSignal(){return mOrphanKillSignal;}
    void setOrphanKillSignal(int signal){mOrphanKillSignal = signal;}
    void setShouldKeepAndroidPropFd(bool newVal) {mShouldKeepAndroidPropFd = newVal;}
    static bool nSleep(const struct timespec time);
    static bool mSleep(unsigned long msec);

private:
    // TODO: These should be disallowed, because they don't make sense for this
    // TODO: class, but modifying the class definition risks altering the binary
    // TODO: interface of the library.  Postpone until we can be sure it won't
    // TODO: cause issues.
    // Babysitter(const Babysitter&); // Hide copy constructor
    // Babysitter& operator=(const Babysitter&); // Hide assignment operator
    void init();
    pid_t getChildPid()const {return mChildPid;}
    void setChildPid(pid_t childPid) {mChildPid = childPid;}
    int getKillerFromReaperFd()const {return mKillerFromReaperFd;}
    void setKillerFromReaperFd(int fd) {mKillerFromReaperFd = fd;}
    int getUserFromReaperFd() {return mUserFromReaperFd;}
    void setUserFromReaperFd(int fd) {mUserFromReaperFd = fd;}
    int getKillerToUserFd() {return mKillerToUserFd;}
    void setKillerToUserFd(int fd) {mKillerToUserFd = fd;}
    bool setupChildWatchers(int childPid);
    bool watchChild(timeval* execTimeout);
    char** buildArgv(const char* name, const char* args)const;
    void freeArgv(char** argv)const;
    void closeAllFds(const int exceptFds[])const;
    bool isChildUmaskSet() {return mIsChildUmaskSet;}
    mode_t getChildUmask() {return mChildUmask;}
    bool shouldKeepAndroidPropFd()const {return mShouldKeepAndroidPropFd;}
    int getAndroidPropFd()const;

    pid_t   mChildPid;
    int     mKillerFromReaperFd;
    int     mUserFromReaperFd;
    int     mKillerToUserFd;
    bool    mIsChildUmaskSet;
    mode_t  mChildUmask;
    int     mOrphanKillSignal;
    bool    mShouldKeepAndroidPropFd;
};
} // namespace
#endif // _NsBabysitter_Babysitter_h_

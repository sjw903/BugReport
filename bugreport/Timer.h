#ifndef _NsBabysitter_Timer_h_
#define _NsBabysitter_Timer_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif
//--------------------------------------------------------------------------------------------------
// Description:
//  Implements a POSIX timer, which upon expiration will spawn a SIGEV_THREAD thread and pass
//  opaque data to the thread entry function.
//--------------------------------------------------------------------------------------------------

#include <stdlib.h>
#include <time.h>
#include <signal.h>

namespace NsBabysitter {
class Timer {
public:
    Timer(void (*handler)(sigval_t), void* handlerParams, const char* name);
    ~Timer();
    bool init();
    bool start(timespec timeout);
    void stop();
    const char* getName()const;
private:
    Timer(); // Hide default constructor
    Timer(const Timer&); // Hide copy constructor
    Timer& operator=(const Timer&); // Hide assignment operator
    void        (*mHandler)(sigval_t);
    void*       mHandlerParams;
    timer_t*    mTimerId;
    char*       mName;
};
} // namespace
#endif // _NsBabysitter_Timer_h_

#include <string.h>
#include <errno.h>
#include "Utils.h"
#include "Timer.h"
using namespace NsBabysitter;

#define NSECS_PER_MSEC 1000000

Timer::Timer(void (*handler)(sigval_t), void* handlerParams, const char* name)
  : mHandler(handler), mHandlerParams(handlerParams), mTimerId(NULL), mName(strdup(name))
{
    MYLOGENTER();
    if (!mName) {
        MYLOGE("strdup(%s) failed", name);
    }
    MYLOGEXIT();
}

Timer::~Timer()
{
    MYLOGENTER();
    if (mTimerId && (-1 == timer_delete(*mTimerId)))
        MYLOGE("timer_delete() failed; errno=%d", errno);
    if (mName)
        free(mName);
    MYLOGEXIT();
}

bool
Timer::init()
{
    MYLOGENTER();
    if (mTimerId)
        PANIC("Duplicate call to Timer::init()");
    mTimerId = new timer_t;
    if (!mTimerId) {
        MYLOGE("new timer_t failed");
        MYLOGEXIT();
        return false;
    }
    struct sigevent se;
    memset(&se, 0, sizeof(se));
    se.sigev_notify = SIGEV_THREAD;
    se.sigev_notify_function = mHandler;
    se.sigev_value.sival_ptr = mHandlerParams;
    // We must use CLOCK_MONOTONIC, because it is not affected by changes in the system time.
    // However, CLOCK_MONOTONIC does not run when the system is in standby.  This means that
    // in order for timers to work reliably, they must be encapsulated by a wake lock.  The
    // user must use a wakelock if one is desired.
    if (-1 == timer_create(CLOCK_MONOTONIC, &se, mTimerId))
    {
        MYLOGE("timer_create() failed; errno=%d", errno);
        MYLOGEXIT();
        return false;
    }
    MYLOGEXIT();
    return true;
}

bool
Timer::start(timespec timeout)
{
    MYLOGENTER();
    if (!mTimerId) {
        MYLOGE("Invalid attempt to start uninitialized timer");
        MYLOGEXIT();
        return false;
    }
    struct itimerspec newTimer = {{0, 0}, timeout};
    if (-1 == timer_settime(*mTimerId, 0, &newTimer, NULL)) {
      MYLOGE("timer_settime failed; errno=%d", errno);
      MYLOGEXIT();
      return false;
    }
    MYLOGD("Started %s Timer (%u.%u secs)",
            getName(),
            (unsigned int)newTimer.it_value.tv_sec,
            (unsigned int)newTimer.it_value.tv_nsec / NSECS_PER_MSEC);
    MYLOGEXIT();
    return true;
}

void
Timer::stop()
{
    MYLOGENTER();
    if (!mTimerId) {
        MYLOGE("Invalid attempt to stop uninitialized timer");
        MYLOGEXIT();
        return;
    }
    struct itimerspec newTimer = {{0, 0}, {0, 0}};
    struct itimerspec oldTimer;
    timer_settime(*mTimerId, 0, &newTimer, &oldTimer);
    MYLOGD("Stopped %s Timer (%u.%u secs remaining)",
            getName(),
            (unsigned int)oldTimer.it_value.tv_sec,
            (unsigned int)oldTimer.it_value.tv_nsec / NSECS_PER_MSEC);
    MYLOGEXIT();
}

const char*
Timer::getName()const
{
    if (mName)
        return mName;
    else
        return "<unnamed>";
}

#include <signal.h>
#include <errno.h>
#include "Utils.h"
using namespace NsBabysitter;

const char* Utils::mLogTag = "Babysitter";
LOG_PRI_T Utils::mMinLogPri = LOG_PRI_INFO;

void
Utils::logPrintf(const char* category, const char* fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    time_t currTime;
    time(&currTime);
    struct tm *localTime = localtime(&currTime);
    struct timeval timeVal;
    gettimeofday(&timeVal, NULL);
    if (NULL != localTime)
    {
        fprintf(stderr, "%-15.15s | %02u:%02u:%02u:%03u | %8d | %10p | ",
            category,
            localTime->tm_hour,
            localTime->tm_min,
            localTime->tm_sec,
            (unsigned int)(timeVal.tv_usec/1000),
            getpid(),
            (void*)pthread_self());
        vfprintf(stderr, fmt, args);
        fprintf(stderr, "\n");
        fflush(stderr);
    }
    va_end(args);
}

bool
Utils::writeExitStatus(int exitStatus, int writeFd)
{
    MYLOGENTER();
    char writeBuf[4] = {0}; // (-1 through 255) + '\0'
    if (-1 == snprintf(writeBuf, sizeof(writeBuf), "%d", exitStatus)) {
        MYLOGE("snprintf() failed; errno=%d", errno);
        MYLOGEXIT();
        return false;
    }
    // TODO: partial write handling
    int numWritten;
    do {
        numWritten = write(writeFd, writeBuf, sizeof(writeBuf));
        if (-1 == numWritten)
            MYLOGE("write() failed; errno=%d", errno);
    } while ((-1 == numWritten) && (EINTR == errno));
    if (-1 == numWritten) {
        MYLOGEXIT();
        return false;
    }
    MYLOGEXIT();
    return true;
}

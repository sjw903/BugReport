#ifndef _NsBabysitter_Utils_h_
#define _NsBabysitter_Utils_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif
//--------------------------------------------------------------------------------------------------
// Utilies for libbabysit
//--------------------------------------------------------------------------------------------------

#include <cutils/log.h>
#include "CommonDefs.h"

namespace NsBabysitter {
#ifdef LOG_STDERR
#define PANIC(...) do {if (LOG_PRI_FATAL >= Utils::mMinLogPri) {Utils::logPrintf("FATAL", __VA_ARGS__);} exit(1);} while (0)
#define MYLOGE(...) do {if (LOG_PRI_ERROR >= Utils::mMinLogPri) {Utils::logPrintf("ERROR", __VA_ARGS__);}} while (0)
#define MYLOGW(...) do {if (LOG_PRI_WARN >= Utils::mMinLogPri) {Utils::logPrintf("WARN", __VA_ARGS__);}} while (0)
#define MYLOGI(...) do {if (LOG_PRI_INFO >= Utils::mMinLogPri) {Utils::logPrintf("INFO", __VA_ARGS__);}} while (0)
#define MYLOGD(...) do {if (LOG_PRI_DEBUG >= Utils::mMinLogPri) {Utils::logPrintf("DEBUG", __VA_ARGS__);}} while (0)
#define MYLOGV(...) do {if (LOG_PRI_VERBOSE >= Utils::mMinLogPri) {Utils::logPrintf("VERBOSE", __VA_ARGS__);}} while (0)
#ifdef UNIT_TEST
#define MYLOGENTER() Utils::logPrintf("ENTER", "+%s", __func__)
#define MYLOGEXIT() Utils::logPrintf("EXIT", "-%s", __func__)
#else // UNIT_TEST
#define MYLOGENTER()
#define MYLOGEXIT()
#endif // UNIT_TEST
#else // LOG_STDERR
#define PANIC(fmt, ...) do {if (LOG_PRI_FATAL >= Utils::mMinLogPri) {ALOG(LOG_ERROR, Utils::mLogTag, fmt, ##__VA_ARGS__);} exit(1);} while (0)
#define MYLOGE(fmt, ...) do {if (LOG_PRI_ERROR >= Utils::mMinLogPri) {ALOG(LOG_ERROR, Utils::mLogTag, fmt, ##__VA_ARGS__);}} while (0)
#define MYLOGW(fmt, ...) do {if (LOG_PRI_WARN >= Utils::mMinLogPri) {ALOG(LOG_WARN, Utils::mLogTag, fmt, ##__VA_ARGS__);}} while (0)
#define MYLOGI(fmt, ...) do {if (LOG_PRI_INFO >= Utils::mMinLogPri) {ALOG(LOG_INFO, Utils::mLogTag, fmt, ##__VA_ARGS__);}} while (0)
#define MYLOGD(fmt, ...) do {if (LOG_PRI_DEBUG >= Utils::mMinLogPri) {ALOG(LOG_DEBUG, Utils::mLogTag, fmt, ##__VA_ARGS__);}} while (0)
#define MYLOGV(fmt, ...) do {if (LOG_PRI_VERBOSE >= Utils::mMinLogPri) {ALOG(LOG_VERBOSE, Utils::mLogTag, fmt, ##__VA_ARGS__);}} while (0)
#ifdef UNIT_TEST
#define MYLOGENTER() ALOG(LOG_VERBOSE, Utils::mLogTag, "+%s", __func__)
#define MYLOGEXIT() ALOG(LOG_VERBOSE, Utils::mLogTag, "-%s", __func__)
#else // UNIT_TEST
#define MYLOGENTER()
#define MYLOGEXIT()
#endif // UNIT_TEST
#endif // LOG_STDERR

#define MAX(a, b) (((a) > (b)) ? (a) : (b))

class Utils {
public:
    static const char* mLogTag;
    static LOG_PRI_T mMinLogPri;

    static void logPrintf(const char* category, const char* fmt, ...);
    static bool writeExitStatus(int exitStatus, int writeFd);
};
} // namespace
#endif // _NsBabysitter_Utils_h_

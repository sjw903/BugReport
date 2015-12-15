#ifndef _NsBabysitter_CommonDefs_h_
#define _NsBabysitter_CommonDefs_h_
#ifndef __cplusplus
#error "This is a C++ header file; it requires C++ to compile."
#endif

namespace NsBabysitter {
    typedef enum {
        LOG_PRI_VERBOSE,
        LOG_PRI_DEBUG,
        LOG_PRI_INFO,
        LOG_PRI_WARN,
        LOG_PRI_ERROR,
        LOG_PRI_FATAL,
        LOG_PRI_SILENT
    } LOG_PRI_T;
} // namespace
#endif // _NsBabysitter_CommonDefs_h_

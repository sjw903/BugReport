LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
    Utils.cpp \
    Babysitter.cpp \
    Reaper.cpp \
    Killer.cpp \
    Timer.cpp \
    List.cpp \
    ListNode.cpp
LOCAL_SHARED_LIBRARIES := libc libcutils liblog
#LOCAL_CFLAGS += -DLOG_STDERR
#LOCAL_CFLAGS += -DLOG_NDEBUG=0
#LOCAL_CFLAGS += -DUNIT_TEST
LOCAL_CFLAGS += -DANDROID
LOCAL_MODULE_OWNER := qiku
LOCAL_MODULE := libbabysit
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := qk-bugreport
LOCAL_MODULE_OWNER := qiku
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := qk-bugreport.cpp
LOCAL_SHARED_LIBRARIES := liblog libcutils
LOCAL_STATIC_LIBRARIES := libbabysit
LOCAL_CFLAGS := -DPLATFORM_SDK_VERSION=$(PLATFORM_SDK_VERSION)
LOCAL_REQUIRED_MODULES := libbabysit am dumpstate screencap
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
    timedexec.cpp
LOCAL_SHARED_LIBRARIES := libc libcutils liblog
LOCAL_STATIC_LIBRARIES := libbabysit
LOCAL_MODULE_OWNER := qiku
#LOCAL_CFLAGS += -DLOG_STDERR
#LOCAL_CFLAGS += -DLOG_NDEBUG=0
#LOCAL_CFLAGS += -DUNIT_TEST
LOCAL_MODULE := timedexec
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := libbabysit
include $(BUILD_EXECUTABLE)


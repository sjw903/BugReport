LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libcommonexe:lib/commons-exec-1.1.jar
LOCAL_MODULE_TAGS := optional
include $(BUILD_MULTI_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_OWNER := qiku
LOCAL_PACKAGE_NAME := PanicReport
LOCAL_SRC_FILES := $(call all-java-files-under,src)
LOCAL_CERTIFICATE := platform
LOCAL_STATIC_JAVA_LIBRARIES := libcommonexe
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
include $(BUILD_PACKAGE)


# Use the following include to make qk-bugreport
include $(call all-makefiles-under,$(LOCAL_PATH))


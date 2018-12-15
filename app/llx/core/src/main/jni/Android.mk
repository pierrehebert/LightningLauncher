LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(call all-subdir-makefiles)

LOCAL_CFLAGS += -std=c99 -O2 -W -Wall -Wno-unused-parameter
LOCAL_MODULE    := ll
LOCAL_SRC_FILES := ll.c

LOCAL_LDLIBS    := -lm -llog -ljnigraphics -landroid

include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(call my-dir)
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true

export MAINDIR:= $(LOCAL_PATH)

#LAPACK, BLAS, F2C compilation
#include $(CLEAR_VARS)
#include $(MAINDIR)/clapack/Android.mk
#LOCAL_PATH := $(MAINDIR)
#include $(CLEAR_VARS)
#LOCAL_MODULE:= lapack
#LOCAL_STATIC_LIBRARIES := tmglib clapack1 clapack2 clapack3 clapack4 clapack5 blas f2c
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)
#LOCAL_EXPORT_LDLIBS := $(LOCAL_LDLIBS)
#include $(BUILD_STATIC_LIBRARY)

#cvsba compilation
#include $(CLEAR_VARS)
#CVSBAOBJ = cvsba/cvsba.cpp
#cvsba/sba_chkjac.c cvsba/sba_crsm.c cvsba/sba_lapack.c cvsba/sba_levmar_wrap.c cvsba/sba_levmar.c

#LOCAL_MODULE:= cvsba
#LOCAL_SRC_FILES := $(CVSBAOBJ)
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)
#LOCAL_EXPORT_LDLIBS := $(LOCAL_LDLIBS)
#include $(BUILD_STATIC_LIBRARY)

#opencv
#OPENCVROOT:= /home/robotbase/Android/OpenCV-2.4.10-android-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include D:/Eclipse/4.4.x/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := ATAMNative.cpp
LOCAL_SRC_FILES += DirectionEstimator.cpp
LOCAL_SRC_FILES += PointDetector.cpp
LOCAL_SRC_FILES += KeyFrame.cpp
LOCAL_SRC_FILES += MTQueue.cpp
LOCAL_SRC_FILES += MovingAverageFilter.cpp
#LOCAL_SRC_FILES += ATAMData.cpp
#LOCAL_SRC_FILES += sba_levmar_wrap.c
#LOCAL_SRC_FILES += sba_chkjac.c
#LOCAL_SRC_FILES += sba_crsm.c
#LOCAL_SRC_FILES += sba_lapack.c
#LOCAL_SRC_FILES += sba_levmar.c
#LOCAL_SRC_FILES += cvsba.cpp
#LOCAL_SRC_FILES += ATAM.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
#LOCAL_STATIC_LIBRARIES  =  cvsba lapack
LOCAL_LDLIBS := -llog -ldl

LOCAL_MODULE := ATAMNative

include $(BUILD_SHARED_LIBRARY)

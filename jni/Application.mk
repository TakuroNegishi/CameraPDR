NDK_TOOLCHAIN_VERSION := 4.8
APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -fexceptions
APP_CPPFLAGS += -std=c++11
#APP_ABI := armeabi-v7a
APP_ABI := arm64-v8a
APP_PLATFORM := android-8
LOCAL_C_INCLUDES += ${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/4.8/include

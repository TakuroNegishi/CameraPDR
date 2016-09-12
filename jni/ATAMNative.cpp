#include <jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>
#include "DirectionEstimator.h"

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

using namespace std;
using namespace cv;

extern "C" {
DirectionEstimator dirEstimator; // デフォルトコンストラクタ

JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_initNative(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_mainProcNative(JNIEnv *env, jobject thiz, jlong addrRgba, jlong milliTime);
JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_changeStateNative(JNIEnv *env, jobject thiz, jboolean isSaveFrameImg);
JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_setStopNative(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_setResetNative(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_getPointAryNative(JNIEnv *env, jobject thiz, jint num, jfloatArray retObj);
JNIEXPORT jint JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_getPointLengthNative(JNIEnv *env, jobject thiz);



JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_initNative(JNIEnv *env, jobject thiz) {
//	dirEstimator.init();
}

JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_mainProcNative (JNIEnv *env, jobject thiz, jlong addrRgba, jlong milliTime) {
	Mat& rgba = *(Mat*)addrRgba;
	dirEstimator.estimate(rgba, milliTime);

	// TODO ここでATAM点群 >> GLRenderに移す

}

JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_changeStateNative(JNIEnv *env, jobject thiz, jboolean isSaveFrameImg) {
	dirEstimator.changeState(isSaveFrameImg);
}

JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_setStopNative(JNIEnv *env, jobject thiz) {
//	atam.setStop();
}

JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_setResetNative(JNIEnv *env, jobject thiz) {
//	atam.setReset();
}

JNIEXPORT void JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_getPointAryNative(JNIEnv *env, jobject thiz, jint num, jfloatArray retObj) {
	/*
	int count = num;
	jfloat *ret = env->GetFloatArrayElements(retObj, NULL);

//	int size = atam.getPoint3Length();
	float pointAry[count*3];
	atam.getPoint3Float(pointAry);
	for (int i = 0; i < count*3; i++) {
		ret[i] = pointAry[i];
	}
//	atam.setReset();

	env->ReleaseFloatArrayElements(retObj, ret, 0);
	*/
}

JNIEXPORT jint JNICALL Java_hosei_negishi_pdrtam_app_NativeAccesser_getPointLengthNative(JNIEnv *env, jobject thiz) {
	return 0;
//	return atam.getPoint3Length();
}

}

/*!
@file		PointDetector.cpp
@brief		functions in CPointDetector
*/

#include "PointDetector.h"
#include <jni.h>
#include <android/log.h>
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

PointDetector::PointDetector()
{
//	mFASTDetector = FastFeatureDetector::create();
}

PointDetector::~PointDetector()
{
}

void PointDetector::init()
{
	// threshold=0.001(default)
	mAKAZEDetector = AKAZE::create(5, 0, 3, 0.001, 4, 4, 1);;
//	mAKAZEDetector = AKAZE::create(5, 0, 3, 0.0005, 4, 4, 1);;

	//	int threshold = 10,	bool nonmaxSuppression = true, int 	type = FastFeatureDetector::TYPE_9_16)
	/* non-maxsuppression = 「最大でない値は(すべて)値を抑える(=値をゼロにする)」 */
	mFASTDetector = FastFeatureDetector::create(40, true, FastFeatureDetector::TYPE_9_16);
	mMatcher = DescriptorMatcher::create("BruteForce-Hamming");
}

void PointDetector::match(const Mat &query, const Mat &train, vector<DMatch> &vmatch) const
{
	mMatcher->match(query, train, vmatch);
}

void PointDetector::describe(const Mat &img, vector<KeyPoint> &vkpt, Mat &vdesc) const
{
	mAKAZEDetector->compute(img, vkpt, vdesc);
}

void PointDetector::detectAKAZE(const Mat &img, vector<KeyPoint> &vkpt) const
{
	mAKAZEDetector->detect(img, vkpt);
}

void PointDetector::detectFAST(const Mat &img, vector<KeyPoint> &vkpt) const
{
	mFASTDetector->detect(img, vkpt);
}

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
	// initialize detector and matcher
	mFASTDetector = cv::FastFeatureDetector::create();
	mORBDetector = cv::ORB::create();
	mMatcher = cv::DescriptorMatcher::create("BruteForce-Hamming(2)");
}

PointDetector::~PointDetector()
{
}

void PointDetector::init()
{
	//	int threshold = 10,	bool nonmaxSuppression = true, int 	type = FastFeatureDetector::TYPE_9_16)
//	mFASTDetector = cv::FastFeatureDetector::create(60, false, cv::FastFeatureDetector::TYPE_9_16);
	mFASTDetector = cv::FastFeatureDetector::create(10, true, cv::FastFeatureDetector::TYPE_9_16);
	mORBDetector = cv::ORB::create(300, 1.2f, 8);
}

void PointDetector::match(const cv::Mat &query, const cv::Mat &train, std::vector<cv::DMatch> &vmatch) const
{
	mMatcher->match(query, train, vmatch);
}

void PointDetector::describe(const cv::Mat &img, std::vector<cv::KeyPoint> &vkpt, cv::Mat &vdesc) const
{
	mORBDetector->compute(img, vkpt, vdesc);
}

void PointDetector::detect(const cv::Mat &img, std::vector<cv::KeyPoint> &vkpt) const
{
//	mFASTDetector->detect(img, vkpt);
	mORBDetector->detect(img, vkpt);
}

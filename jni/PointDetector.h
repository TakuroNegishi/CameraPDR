/*!
@file		PointDetector.h
@brief		header of CPointDetector
*/
#pragma once

#include <opencv2/core.hpp>
#include "opencv2/features2d.hpp"

using namespace cv;
using namespace std;

class PointDetector
{
public:
	PointDetector();
	~PointDetector();

	void init();
	void match(const Mat &query, const Mat &train, vector<DMatch> &vmatch) const;
	void describe(const Mat &img, vector<KeyPoint> &vkpt, Mat &vdesc) const;
	void detect(const Mat &img, vector<KeyPoint> &vkpt) const;

private:
	int type; // 0:ORB 1:FAST
	Ptr<ORB> mORBDetector;
	Ptr<FastFeatureDetector> mFASTDetector;
	Ptr<DescriptorMatcher> mMatcher;
};


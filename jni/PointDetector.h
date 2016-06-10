/*!
@file		PointDetector.h
@brief		header of CPointDetector
*/
#pragma once

#include <opencv2/core.hpp>
#include "opencv2/features2d.hpp"

class PointDetector
{
public:
	PointDetector();
	~PointDetector();

	void init();
	void match(const cv::Mat &query, const cv::Mat &train, std::vector<cv::DMatch> &vmatch) const;
	void describe(const cv::Mat &img, std::vector<cv::KeyPoint> &vkpt, cv::Mat &vdesc) const;
	void detect(const cv::Mat &img, std::vector<cv::KeyPoint> &vkpt) const;

private:
	cv::Ptr<cv::ORB> mORBDetector;
	cv::Ptr<cv::FastFeatureDetector> mFASTDetector;
	cv::Ptr<cv::DescriptorMatcher> mMatcher;
};


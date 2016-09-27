#pragma once

#include <opencv2/core.hpp>
#include "opencv2/features2d.hpp"
#include "KeyFrame.h"

class Tracker
{
private:
	cv::Ptr<cv::FastFeatureDetector> fastDetector;
	cv::Mat grayImg;		// 現在フレームのグレー画像
	std::vector<cv::KeyPoint> currentKpts;
	bool isFindKeyFrame;
	bool isFirstFrame;
	long long prevKFTime;
	KeyFrame* lastKF;

public:
	Tracker();
	~Tracker();
	void clear();
	bool tracking(const cv::Mat &cameraImg, const long long milliTime);
	KeyFrame getLastKF();
	void detect(const cv::Mat &img, std::vector<cv::KeyPoint> &keyPoints) const;
};


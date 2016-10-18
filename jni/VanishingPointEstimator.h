#pragma once

#include <fstream>
#include <mutex>
#include <opencv2/core.hpp>
#include "opencv2/features2d.hpp"
#include "KeyFrame.h"
#include "MovingAverageFilter.h"

class VanishingPointEstimator
{
public:
	VanishingPointEstimator();
	~VanishingPointEstimator();

	void clear();
	void calcVP(KeyFrame &currentKF);
	std::vector<cv::DMatch> calcMatchingFlow(const std::vector<cv::KeyPoint> &prevKpts, const cv::Mat &prevDesc, const std::vector<cv::KeyPoint> &currentKpts, const cv::Mat &currentDesc);
	cv::Point2f getCrossPoint(const std::vector<cv::DMatch>& matchVector,
		const std::vector<cv::KeyPoint>& currentKpts, const std::vector<cv::KeyPoint>& prevKpts);
	float getDistance(const cv::Point2f &pt1, const cv::Point2f &pt2);
	void getStartEndTime(long long startEndTime[]);

	void saveDebug(cv::Mat &img, const std::vector<cv::DMatch> &matches, const std::vector<cv::KeyPoint> &prev, const std::vector<cv::KeyPoint> &cur, long long milliTime, cv::Point2f vp);
	void saveImg(const cv::Mat &img, long long milliTime);

	void match(const cv::Mat &query, const cv::Mat &train, std::vector<cv::DMatch> &vmatch) const;
	void describe(const cv::Mat &img, std::vector<cv::KeyPoint> &vkpt, cv::Mat &vdesc) const;
	void detect(const cv::Mat &img, std::vector<cv::KeyPoint> &vkpt) const;

private:
	static const int POINT_SIZE;
	static const cv::Scalar SCALAR_RED;
	static const cv::Scalar SCALAR_GREEN;
	static const cv::Scalar SCALAR_BLUE;
	static const cv::Scalar SCALAR_YELLOW;
	static const float ERROR_VP;
	static const int LEFT_VP;
	static const int NORMAL_VP;
	static const int RIGHT_VP;

	cv::Ptr<cv::AKAZE> akazeDetector;
	cv::Ptr<cv::DescriptorMatcher> matcher;
	std::vector<cv::KeyPoint> prevKpts;
	cv::Mat prevDesc;
	bool isFirstProc;
	std::vector<cv::Point2f> pointHistory;
	std::vector<cv::Point2f> pointHistoryMA;
	MovingAverageFilter* maFilterX;
	MovingAverageFilter* maFilterY;
	std::mutex sidewayMutex;
	int sidewayStatus;
	long long startTime;
	int startVPStatus;
	long long endTime;
	std::ofstream ofs;
	int procCount;
	long long sideStartTime;
	long long sideEndTime;
};


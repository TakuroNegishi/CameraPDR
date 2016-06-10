#pragma once

#include <mutex>
#include <opencv2/core.hpp>
#include "PointDetector.h"

class DirectionEstimator
{
public:
	DirectionEstimator();
	void init();
	void clear();
	void changeState(bool isSaveFrameImg);
	void estimate(cv::Mat &rgbaImg);
	float getDistance(const cv::Point2f &pt1, const cv::Point2f &pt2);
	void draw(cv::Mat &rgbaImg);

private:
	static const int POINT_SIZE;			// 特徴点の描画半径
	static const cv::Scalar SCALAR_RED;
	static const cv::Scalar SCALAR_GREEN;
	static const cv::Scalar SCALAR_BLUE;
	static const cv::Scalar SCALAR_YELLOW;
	static const cv::Scalar SCALAR_PURPLE;

	int frameCount;
	std::mutex loopMutex;	// ループ処理制御用Mutex
	bool isLoop;
	bool isSaveFrameImg;
	cv::Mat grayImg;		// 現在フレームのグレー画像
	cv::Mat prevGrayImg;	// 1フレーム前のグレー画像
	PointDetector pointDetector;
	std::vector<cv::KeyPoint> currentKpts;
	std::vector<cv::Point2f> currentPoints;
	std::vector<cv::Point2f> prevPoints;
	std::vector<unsigned char> status;
	std::vector<cv::Point2f> vtracked;

};

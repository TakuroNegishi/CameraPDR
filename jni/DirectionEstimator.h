#pragma once

#include <mutex>
#include <opencv2/core.hpp>
#include <opencv2/superres/optical_flow.hpp>
#include "PointDetector.h"

class DirectionEstimator
{
public:
	DirectionEstimator();
	~DirectionEstimator();
	void init();
	void clear();
	void changeState(bool isSaveFrameImg);
	void estimate(cv::Mat &rgbaImg, long nanoTime);
//	void matchImg();
	void calcOpticalFlow();
	cv::Point2f getCrossPoint(cv::Point2f &firstLinePoint1, cv::Point2f &firstLinePoint2,
			cv::Point2f &secondLinePoint1, cv::Point2f &secondLinePoint2);
	cv::Point2f getCrossPoint2();
	float getDistance(const cv::Point2f &pt1, const cv::Point2f &pt2);
	void draw(cv::Mat &rgbaImg);
	void saveImg(cv::Mat &rgbaImg, long nanoTime);

private:
	static const int POINT_SIZE;			// 特徴点の描画半径
	static const cv::Scalar SCALAR_RED;
	static const cv::Scalar SCALAR_GREEN;
	static const cv::Scalar SCALAR_BLUE;
	static const cv::Scalar SCALAR_YELLOW;
	static const cv::Scalar SCALAR_PURPLE;
	static const cv::Scalar SCALAR_CYAN;
	static const cv::Scalar SCALAR_BLACK;
	static const cv::Scalar SCALAR_WHITE;
	static const int FLOW_LINE_MIN_LIMIT;	// 許容する特徴点の最小距離距離
	static const int FLOW_LINE_MAX_LIMIT;	// 許容する特徴点の最大距離距離
	static const int FRAME_SPAN;

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
//	cv::Mat currentDescriptor;	// 現在画像の特徴量
//	cv::Mat prevDescriptor;		// 1フレーム前のの特徴量
//	int matchFrameCount;	// 特徴点マッチングを行うフレーム周期
//	std::vector<cv::DMatch> matchVector; // 現在画像と1フレーム前の特徴点のマッチング結果

	std::vector<unsigned char> status;	// オプティカルフロー追跡結果(1:成功, 0:失敗)
	std::vector<cv::Point2f> vtracked;
//	std::vector<cv::Point2f> vanishingPoints; // 現在フレームの消失点
	cv::Point2f vanishingPointMean; // 現在フレームの消失点の平均点
};

#pragma once

#include <mutex>
#include <thread>
#include <opencv2/core.hpp>
#include "PointDetector.h"
#include "MTQueue.h"
#include "MovingAverageFilter.h"

using namespace std;
using namespace cv;

class DirectionEstimator
{
public:
	DirectionEstimator();
	~DirectionEstimator();
	void init();
	void clear();
	void changeState(bool isSaveFrameImg);
	void estimate(Mat &rgbaImg, long milliTime);

	void procCalcVP();
	vector<DMatch> calcMatchingFlow(const vector<KeyPoint> &prevKpts, const Mat &prevDesc,
			const vector<KeyPoint> &currentKpts, const Mat &currentDesc);

	Point2f getCrossPoint(const vector<DMatch>& matchVector,
		const vector<KeyPoint>& currentKpts, const vector<KeyPoint>& prevKpts);
	float getDistance(const Point2f &pt1, const Point2f &pt2);
	void saveDebug(Mat &img, const vector<KeyPoint> &prev, const vector<KeyPoint> &cur, long milliTime);
	void saveDebug(Mat &img, const vector<DMatch> &matches, const vector<KeyPoint> &prev,
			const vector<KeyPoint> &cur, long milliTime);
	void drawPoints(Mat &rgbaImg, const vector<Point2f> &points, const Scalar color);
	void saveImg(Mat &rgbaImg, long milliTime);
	void saveImg(Mat &rgbaImg, Mat &grayImg, long milliTime);

private:
	static const int POINT_SIZE;			// 特徴点の描画半径
	static const Scalar SCALAR_RED;
	static const Scalar SCALAR_GREEN;
	static const Scalar SCALAR_BLUE;
	static const Scalar SCALAR_YELLOW;
	static const Scalar SCALAR_PURPLE;
	static const Scalar SCALAR_CYAN;
	static const Scalar SCALAR_BLACK;
	static const Scalar SCALAR_WHITE;
	static const int FLOW_LINE_MIN_LIMIT;	// 許容する特徴点の最小距離距離
	static const int FLOW_LINE_MAX_LIMIT;	// 許容する特徴点の最大距離距離
	static const int IMG_WIDTH;
	static const int IMG_HEIGHT;
	static const int ERROR_VP;
	static const int LEFT_VP;
	static const int NORMAL_VP;
	static const int RIGHT_VP;

	int frameCount;
	long sumElapsedTime;
	mutex loopMutex;	// ループ処理制御用Mutex
	MTQueue keyFrameQueue; // 画像処理用キュー
	bool isFirstFrame;
	bool isFindKeyFrame;
	bool isLoop;
	bool isSaveFrameImg;
	long prevKFTime;		// 前回のキーフレームの時間
	KeyFrame lastKF;	// 最新キーフレーム候補
	Mat lastImg;		// キーフレーム候補のカラー画像
	Mat lastGrayImg;		// キーフレーム候補のカラー画像
	Mat grayImg;		// 現在フレームのグレー画像
	PointDetector pointDetector;
	vector<KeyPoint> currentKpts;

	thread vpCalcThread;
	mutex vpThreadMutex;
	bool isVPThreadLoop;

	PointDetector vpPointDetector;
	vector<KeyPoint> prevKpts;
	Mat prevDesc;
	std::vector<cv::Point2f> pointHistory;
	std::vector<cv::Point2f> pointHistoryMA;
	MovingAverageFilter maFilterX;
	MovingAverageFilter maFilterY;
	int vpStatus;
};

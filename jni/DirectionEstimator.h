#pragma once

#include <fstream>
#include <mutex>
#include <thread>
#include <queue>
#include <opencv2/core.hpp>
#include "MovingAverageFilter.h"
#include "Tracker.h"
#include "VanishingPointEstimator.h"

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
	void estimate(Mat &rgbaImg, long long milliTime);
	void procCalcVP();
	void getStartEndTime(long long startEndTime[]);

	void saveDebug(Mat &img, const vector<KeyPoint> &prev, const vector<KeyPoint> &cur, long long milliTime);
	void saveDebug(Mat &img, const vector<DMatch> &matches, const vector<KeyPoint> &prev,
			const vector<KeyPoint> &cur, long long milliTime);
	void drawPoints(Mat &rgbaImg, const vector<Point2f> &points, const Scalar color);
	void saveImg(const Mat &rgbaImg, long long milliTime);
	void saveImg(Mat &rgbaImg, Mat &grayImg, long long milliTime);

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

	static const int IMG_WIDTH;
	static const int IMG_HEIGHT;

	Tracker* tracker;
	VanishingPointEstimator* vpEstimator;

	int frameCount;
	mutex queueMutex;
	queue<KeyFrame> keyFrameQueue; // 画像処理用キュー

	mutex cameraLoopMutex;	// ループ処理制御用Mutex
	bool isCameraLoop;
	bool isReset;

	thread vpThread;
	mutex vpLoopMutex;
	mutex vpCalcMutex;
	bool isVPLoop;

	//std::vector<cv::Point2f> pointHistory;
	//std::vector<cv::Point2f> pointHistoryMA;
	//MovingAverageFilter maFilterX;
	//MovingAverageFilter maFilterY;
	//int vpStatus;
	//std::ofstream ofs;
	//bool isFirstProc;
};

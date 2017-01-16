#include "DirectionEstimator.h"
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/highgui/highgui.hpp>

#include <jni.h>
#include <android/log.h>
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

#include <iostream>
#include <time.h>
//#include <sys/time.h>
//#include <math.h>

using namespace std;

const int DirectionEstimator::POINT_SIZE = 3;
const Scalar DirectionEstimator::SCALAR_RED(255, 0, 0);
const Scalar DirectionEstimator::SCALAR_GREEN(0, 255, 0);
const Scalar DirectionEstimator::SCALAR_BLUE(0, 0, 255);
const Scalar DirectionEstimator::SCALAR_YELLOW(255, 255, 0);
const Scalar DirectionEstimator::SCALAR_PURPLE(255, 0, 255);
const Scalar DirectionEstimator::SCALAR_CYAN(0, 255, 255);
const Scalar DirectionEstimator::SCALAR_BLACK(0, 0, 0);
const Scalar DirectionEstimator::SCALAR_WHITE(255, 255, 255);
const int DirectionEstimator::IMG_WIDTH = 640;
const int DirectionEstimator::IMG_HEIGHT = 480;

DirectionEstimator::DirectionEstimator()
{
	init();
	clear();
}

DirectionEstimator::~DirectionEstimator()
{
	delete tracker;
	delete vpEstimator;
}

void DirectionEstimator::init()
{
	isCameraLoop = false;
	isVPLoop = false;
	tracker = new Tracker();
	vpEstimator = new VanishingPointEstimator();
}

void DirectionEstimator::clear()
{
	isReset = false;
	frameCount = 0;
	queue<KeyFrame> empty;
	swap(keyFrameQueue, empty);
}

void DirectionEstimator::changeState(bool isSaveFrameImg)
{
	// lock_guardを使うと、スコープの終わりでlock()変数が破棄されるのにともなって、自動的にロックも解除される
	//lock_guard<mutex> lock(cameraLoopMutex);

	cameraLoopMutex.lock();
	isCameraLoop = !isCameraLoop;
	if (!isCameraLoop) isReset = true;
	cameraLoopMutex.unlock();

	vpLoopMutex.lock();
	isVPLoop = !isVPLoop;
	vpLoopMutex.unlock();

	if (isVPLoop ) {
		vpThread = thread(&DirectionEstimator::procCalcVP, this);
	} else {
		cout << "joining..." << endl;
		vpThread.join();
		vpEstimator->clear();
		cout << "joined!" << endl;
	}
}

void DirectionEstimator::estimate(Mat &rgbaImg, long long milliTime)
{
	// ループ開始チェック
	cameraLoopMutex.lock();
	bool loop = isCameraLoop;
	bool reset = isReset;
	if (reset) {
		tracker->clear();
		isReset = false;
	}
	cameraLoopMutex.unlock();
	if (!loop) return;

	//frameCount++;
	//chrono::system_clock::time_poeint  start, end;
	//start = chrono::system_clock::now();

	/* debug */
//	saveImg(rgbaImg, milliTime); // debug

	if (tracker->tracking(rgbaImg, milliTime)) {
		KeyFrame lastKF;
		lastKF.set(tracker->getLastKF());
		queueMutex.lock();
		keyFrameQueue.push(lastKF);
//		LOGE(",DE_KeyFrame,%lld", milliTime);
//		cout << "pushed" << endl;
		queueMutex.unlock();
	}
}

void DirectionEstimator::procCalcVP()
{
	vpEstimator->setOFStream(); // ログファイルのパスセット
	while (true) {
		// ループチェック
		vpLoopMutex.lock();
		bool loop = isVPLoop;
		vpLoopMutex.unlock();
		if (!loop) break;

		bool isPopped = false;
		KeyFrame lastKF;
		queueMutex.lock();
		if (keyFrameQueue.size() > 0) {
			lastKF = keyFrameQueue.front();
			keyFrameQueue.pop();
			isPopped = true;
		}
		queueMutex.unlock();
		if (!isPopped) continue;

		// start,endTime取得Threadとの同期
		vpCalcMutex.lock();
		vpEstimator->calcVP(lastKF);
		vpCalcMutex.unlock();
	}
}

void DirectionEstimator::getStartEndTime(long long startEndTime[])
{
	vpEstimator->getStartEndTime(startEndTime);
}

void DirectionEstimator::saveDebug(Mat &img, const vector<KeyPoint> &prev, const vector<KeyPoint> &cur, long long milliTime)
{
	// 特徴点描画
	for (int i = 0; i < prev.size(); ++i) {
		circle(img, prev[i].pt, POINT_SIZE, SCALAR_BLUE, -1);
	}
	for (int i = 0; i < cur.size(); ++i) {
		circle(img, cur[i].pt, POINT_SIZE, SCALAR_YELLOW, -1);
	}

	saveImg(img, milliTime);
}

void DirectionEstimator::saveDebug(Mat &img, const vector<DMatch> &matches, const vector<KeyPoint> &prev, const vector<KeyPoint> &cur, long long milliTime)
{
	// フロー描画
	for (int i = 0; i < matches.size(); ++i) {
		line(img, prev[matches[i].queryIdx].pt, cur[matches[i].trainIdx].pt, SCALAR_GREEN, 3);
	}
	// 特徴点描画
	for (int i = 0; i < prev.size(); ++i) {
		circle(img, prev[i].pt, POINT_SIZE, SCALAR_BLUE, -1);
	}
	for (int i = 0; i < cur.size(); ++i) {
		circle(img, cur[i].pt, POINT_SIZE, SCALAR_YELLOW, -1);
	}

	saveImg(img, milliTime);
}

void DirectionEstimator::drawPoints(Mat &rgbaImg, const vector<Point2f> &points, const Scalar color)
{
	if (points.size() > 0) {
		for (size_t i = 0, n = points.size(); i < n; ++i) {
			// 描画先画像, 円中心, 半径, 色, -1=塗りつぶし
			circle(rgbaImg, points[i], POINT_SIZE, color, -1);
		}
	}
}

void DirectionEstimator::saveImg(const Mat &rgbaImg, long long milliTime)
{
	// 画像保存
//	loopMutex.lock();
	//if (isSaveFrameImg) {
//		time_t long_time;
//		struct tm now_time;                 // ポインタから、変数実体に変更
//		time(&long_time);
//		localtime_s(&now_time, &long_time);  // 戻り値から引数に変更

		//time_t now = time(NULL);
		//struct tm *pnow = localtime_s(&now);

		char buff[256] = "";
		//		sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%06ld.jpg",
		//				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
		//				time_st->tm_min, time_st->tm_sec, myTime.tv_usec);
		//sprintf_s(buff, "C:/Users/admin/Documents/Visual Studio 2013/Projects/VirtualCameraPDR/VirtualCameraPDR/result/%04d-%02d-%02d_%02d;%02d;%02d;%lld.jpg",
		//	now_time.tm_year + 1900, now_time.tm_mon + 1, now_time.tm_mday, now_time.tm_hour,
		//	now_time.tm_min, now_time.tm_sec, milliTime);
		sprintf(buff, "/storage/emulated/0/negishi.deadreckoning/Feature Image/%lld.jpg", milliTime);
		Mat copy;
		//rgbaImg.copyTo(copy);
		cvtColor(rgbaImg, copy, COLOR_BGR2RGB); // なぜか色が変わるから対処
		//imshow("color", rgbaImg);
		imwrite(buff, copy);
	//}
//	loopMutex.unlock();
}

void DirectionEstimator::saveImg(Mat &rgbaImg, Mat &grayImg, long long milliTime)
{
	// 画像保存
//	loopMutex.lock();
	//if (isSaveFrameImg) {
	//	struct timeval myTime;
	//	struct tm *time_st;
	//	gettimeofday(&myTime, NULL);
	//	time_st = localtime(&myTime.tv_sec);

	//	char buff[256] = "";
	//	//		sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%06ld.jpg",
	//	//				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
	//	//				time_st->tm_min, time_st->tm_sec, myTime.tv_usec);
	//	sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%ld.jpg",
	//			time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
	//			time_st->tm_min, time_st->tm_sec, milliTime);
	//	Mat copy = Mat(Size(rgbaImg.cols, rgbaImg.rows), rgbaImg.type());
	//	rgbaImg.copyTo(copy);
	//	cvtColor(copy, copy, COLOR_BGR2RGB); // なぜか色が変わるから対処
	//	imwrite(buff, copy);

	//	char buff2[256] = "";
	//	sprintf(buff2, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%ld_Gray.jpg",
	//			time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
	//			time_st->tm_min, time_st->tm_sec, milliTime);
	//	Mat grayCopy = Mat(Size(grayImg.cols, grayImg.rows), grayImg.type());
	//	grayImg.copyTo(grayCopy);
	//	imwrite(buff2, grayCopy);
	//}
//	loopMutex.unlock();
}


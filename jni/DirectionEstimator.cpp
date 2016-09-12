#include "DirectionEstimator.h"
#include <jni.h>
#include <android/log.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/highgui/highgui.hpp>

//#include <stdio.h>
//#include <fstream>
#include <time.h>
#include <sys/time.h>
//#include <math.h>

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

const int DirectionEstimator::POINT_SIZE = 3;
const Scalar DirectionEstimator::SCALAR_RED(255, 0, 0);
const Scalar DirectionEstimator::SCALAR_GREEN(0, 255, 0);
const Scalar DirectionEstimator::SCALAR_BLUE(0, 0, 255);
const Scalar DirectionEstimator::SCALAR_YELLOW(255, 255, 0);
const Scalar DirectionEstimator::SCALAR_PURPLE(255, 0, 255);
const Scalar DirectionEstimator::SCALAR_CYAN(0, 255, 255);
const Scalar DirectionEstimator::SCALAR_BLACK(0, 0, 0);
const Scalar DirectionEstimator::SCALAR_WHITE(255, 255, 255);
const int DirectionEstimator::FLOW_LINE_MIN_LIMIT = 0;
const int DirectionEstimator::FLOW_LINE_MAX_LIMIT = 50;
const int DirectionEstimator::IMG_WIDTH = 640;
const int DirectionEstimator::IMG_HEIGHT = 480;
const int DirectionEstimator::ERROR_VP = -999;
const int DirectionEstimator::LEFT_VP = -1;
const int DirectionEstimator::NORMAL_VP = 0;
const int DirectionEstimator::RIGHT_VP = 1;

DirectionEstimator::DirectionEstimator()
{
	init();
	clear();
}

DirectionEstimator::~DirectionEstimator()
{
	lastImg.release();
	lastGrayImg.release();
	grayImg.release();
	prevDesc.release();
}

void DirectionEstimator::init()
{
	isLoop = false;
	isVPThreadLoop = false;
	lastImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC4);
	lastGrayImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
	grayImg = Mat(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
	pointDetector.init();
	vpPointDetector.init();
}

void DirectionEstimator::clear()
{
	frameCount = 0;
	sumElapsedTime = 0;
	isFirstFrame = true;
	isSaveFrameImg = false;
	isFindKeyFrame = false;
	currentKpts.clear();
//	prevPoints.clear();
//	vanishingPoint = Point2f(320, 240);
	prevKFTime = 0;
	prevKpts.clear();
	pointHistory.clear();
	pointHistoryMA.clear();
	maFilterX.clear();
	maFilterY.clear();
	vpStatus = NORMAL_VP;
}

void DirectionEstimator::changeState(bool isSaveFrameImg)
{
	// lock_guardを使うと、スコープの終わりでlock()変数が破棄されるのにともなって、自動的にロックも解除される
	lock_guard<mutex> lock(loopMutex);

	vpThreadMutex.lock();
	isLoop = !isLoop;
	isVPThreadLoop = !isVPThreadLoop;
	vpThreadMutex.unlock();
	if (isVPThreadLoop ) {
		vpCalcThread = thread(&DirectionEstimator::procCalcVP, this);
	} else {
		// TODO ここでMTQueueにシグナルを送ってpop操作から脱出させる
		keyFrameQueue.sendFinishPopSignal();
		LOGE("joining...");
		vpCalcThread.join();
		LOGE("joined!");
	}
	this->isSaveFrameImg = isSaveFrameImg;
}

void DirectionEstimator::estimate(Mat &rgbaImg, long milliTime)
{
	// ループ開始チェック
	loopMutex.lock();
	bool loop = isLoop;
	loopMutex.unlock();
	if (!loop) return;

	frameCount++;
	chrono::system_clock::time_point  start, end;
	start = chrono::system_clock::now();

	cvtColor(rgbaImg, grayImg, COLOR_BGR2GRAY); // グレースケール
	currentKpts.clear();
	pointDetector.detectFAST(grayImg, currentKpts);
	if (currentKpts.size() > 20) {
		// 20ポイント以上特徴点が取れれば,キーフレーム候補に
		rgbaImg.copyTo(lastImg);
		grayImg.copyTo(lastGrayImg);
		isFindKeyFrame = true;
		lastKF.set(milliTime, lastImg, lastGrayImg);

		//
		if (isFirstFrame) {
			prevKFTime = milliTime;
			isFirstFrame = false;
			isFindKeyFrame = false;
			keyFrameQueue.push(lastKF);
//			saveImg(lastKF.img, lastKF.timeStamp);
		}
	}

	if (!isFirstFrame && isFindKeyFrame && milliTime - prevKFTime >= 700) {
		// 700msec(0.7sec)間隔でキーフレーム候補を画像処理用キューに入れる
		prevKFTime = milliTime;
		isFindKeyFrame = false;
		keyFrameQueue.push(lastKF);
		// TODO ここでlastImgをzeroにすると,pushしたlastKFもzeroになる?
//		lastImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC4);
//		lastGrayImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
//		saveImg(lastKF.img, lastKF.timeStamp);
	}

	end = chrono::system_clock::now();
	long elapsed = chrono::duration_cast<chrono::milliseconds>(end - start).count();
//	LOGE("elapsed time(msec): %19ld", elapsed);

	sumElapsedTime += elapsed;
	long averageTime = sumElapsedTime / frameCount;
//	LOGE("average time(msec): %19ld", averageTime);

	// ループ停止 -> クリア
	loopMutex.lock();
	if (!isLoop) clear();
	loopMutex.unlock();
}

void DirectionEstimator::procCalcVP()
{

	while (true) {
		vpThreadMutex.lock();
		bool isLoop = isVPThreadLoop;
		vpThreadMutex.unlock();
		if (!isLoop) {
			break;
		}
		LOGE("poping...");
		KeyFrame currentKF = keyFrameQueue.pop();
		LOGE("poped!...");
		if (currentKF.timeStamp == -1) break; // pop操作を強制中断された場合
		// AKAZE detect&describe
		vpPointDetector.detectAKAZE(currentKF.grayImg, currentKF.kpts);
		vpPointDetector.describe(currentKF.grayImg, currentKF.kpts, currentKF.desc);
		LOGE("currentKF.kpts: %d", currentKF.kpts.size());
		LOGE("prevKpts: %d", prevKpts.size());
		if (prevKpts.size() == 0) {
			// current >> prev
			prevKpts = currentKF.kpts;
			currentKF.desc.copyTo(prevDesc);
			continue;
		}
		// matching
		vector<DMatch> inlierMatches = calcMatchingFlow(prevKpts, prevDesc, currentKF.kpts, currentKF.desc);

		/*** draw debug ***/
//		saveImg(currentKF.img, currentKF.grayImg, currentKF.timeStamp);
//		saveDebug(currentKF.img, prevKpts, currentKF.kpts, currentKF.timeStamp);
		saveDebug(currentKF.img, inlierMatches, prevKpts, currentKF.kpts, currentKF.timeStamp);
		/*** draw debug ***/

		LOGE("queue size: %d", keyFrameQueue.size());

		//--- 消失点計算 ----
		Point2f vp = getCrossPoint(inlierMatches, currentKF.kpts, prevKpts);
		pointHistory.push_back(vp); // 消失点計算
		if (vp.x == ERROR_VP && vp.y == ERROR_VP) {
			pointHistoryMA.push_back(vp);
		}
		else {
			// 消失点計算成功
			Point2f vpMA = Point2f(maFilterX.update(vp.x), maFilterY.update(vp.y));
			pointHistoryMA.push_back(vpMA);

			//--- 横向き判定 ---
			if (vpMA.x < 0) {
				vpStatus = LEFT_VP;
				LOGE("LEFT_VP: %ld", currentKF.timeStamp);
			} else if (vpMA.x > 640) {
				vpStatus = RIGHT_VP;
				LOGE("RIGHT_VP: %ld", currentKF.timeStamp);
			} else {
				vpStatus = NORMAL_VP;
			}
		}
		// current >> prev
		prevKpts = currentKF.kpts;
		currentKF.desc.copyTo(prevDesc);

		LOGE("-----------------");
	}
	LOGE("finish procCalcVP()");
}

vector<DMatch> DirectionEstimator::calcMatchingFlow(const vector<KeyPoint> &prevKpts, const Mat &prevDesc, const vector<KeyPoint> &currentKpts, const Mat &currentDesc)
{
	vector<DMatch> inlierMatches;
	vector<DMatch> matchVector, matchPrevToCur, matchCurToPrev;
	if (prevDesc.rows == 0 || currentDesc.rows == 0) return inlierMatches;

	// prev >> current, current >> prev 双方向でマッチング
	vpPointDetector.match(prevDesc, currentDesc, matchPrevToCur); // prev=query, current=train
	vpPointDetector.match(currentDesc, prevDesc, matchCurToPrev);
	if ( matchPrevToCur.size() == 0 || matchCurToPrev.size() == 0) return inlierMatches;

	// クロスチェック
	vector<Point2f> goodPrevPts, goodCurrentPts;
	for (int i = 0; i < matchPrevToCur.size(); i++) {
		DMatch forward = matchPrevToCur[i]; // prev=query, current=train
		DMatch backward = matchCurToPrev[forward.trainIdx];
		if (backward.trainIdx == forward.queryIdx) {
			matchVector.push_back(forward);
			goodPrevPts.push_back(prevKpts[forward.queryIdx].pt);
			goodCurrentPts.push_back(currentKpts[forward.trainIdx].pt);
		}
	}

	//ホモグラフィ行列推定
	Mat masks;
	Mat H;
	H = findHomography(goodPrevPts, goodCurrentPts, masks, RANSAC, 3.f);

	//RANSACで使われた対応点のみ抽出
	for (int i = 0; i < masks.rows; ++i) {
		uchar *inlier = masks.ptr<uchar>(i);
		if (inlier[0] == 1) {
			inlierMatches.push_back(matchVector[i]);
		}
	}
	return inlierMatches;
}

Point2f DirectionEstimator::getCrossPoint(const vector<DMatch>& matchVector,
	const vector<KeyPoint>& currentKpts, const vector<KeyPoint>& prevKpts)
{
	int flowNum = (int)matchVector.size();
	float a = 0;
	float b = 0;
	float p = 0;
	float c = 0;
	float d = 0;
	float q = 0;
	float bunbo = 0;
	for (int i = 0; i < flowNum; ++i) {
		Point2f p1 = currentKpts[matchVector[i].trainIdx].pt;
		Point2f p2 = prevKpts[matchVector[i].queryIdx].pt;

		// 連立方程式公式 - https://t-sv.sakura.ne.jp/text/num_ana/ren_eq22/ren_eq22.html
		//		sumX += 2*X * (p1.y - p2.y) * (p1.y - p2.y) + 2*Y * (p2.x - p1.x) * (p1.y - p2.y)
		//				+ 2 * (p1.x * p2.y - p2.x * p1.y) * (p1.y - p2.y); // = 0 偏微分X
		//		sumY += 2*Y * (p2.x - p1.x) * (p2.x - p1.x) + 2*X * (p2.x - p1.x) * (p1.y - p2.y)
		//				+ 2 * (p1.x * p2.y - p2.x * p1.y) * (p2.x - p1.x); // = 0 偏微分Y

		a += (p1.y - p2.y) * (p1.y - p2.y);
		b += (p2.x - p1.x) * (p1.y - p2.y);
		p += (p1.x * p2.y - p2.x * p1.y) * (p1.y - p2.y);
		c += (p2.x - p1.x) * (p1.y - p2.y);
		d += (p2.x - p1.x) * (p2.x - p1.x);
		q += (p1.x * p2.y - p2.x * p1.y) * (p2.x - p1.x);
	}
	p *= -1;
	q *= -1;
	bunbo = (a * d - b * c);
	if (bunbo == 0) return Point2f(ERROR_VP, ERROR_VP);
	float X = (d * p - b * q) / bunbo;
	float Y = (a * q - c * p) / bunbo;
	return Point2f(X, Y);
}

float DirectionEstimator::getDistance(const Point2f &pt1, const Point2f &pt2)
{
	float dx = pt2.x - pt1.x;
	float dy = pt2.y - pt1.y;
	return sqrt(dx * dx + dy * dy);
}

void DirectionEstimator::saveDebug(Mat &img, const vector<KeyPoint> &prev, const vector<KeyPoint> &cur, long milliTime)
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

void DirectionEstimator::saveDebug(Mat &img, const vector<DMatch> &matches, const vector<KeyPoint> &prev, const vector<KeyPoint> &cur, long milliTime)
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
		for (int i = 0, n = points.size(); i < n; ++i) {
			// 描画先画像, 円中心, 半径, 色, -1=塗りつぶし
			circle(rgbaImg, points[i], POINT_SIZE, color, -1);
		}
	}
}

void DirectionEstimator::saveImg(Mat &rgbaImg, long milliTime)
{
	// 画像保存
//	loopMutex.lock();
	if (isSaveFrameImg) {
		struct timeval myTime;
		struct tm *time_st;
		gettimeofday(&myTime, NULL);
		time_st = localtime(&myTime.tv_sec);

		char buff[128] = "";
		//		sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%06ld.jpg",
		//				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
		//				time_st->tm_min, time_st->tm_sec, myTime.tv_usec);
		sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%19ld.jpg",
				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
				time_st->tm_min, time_st->tm_sec, milliTime);
		Mat copy = Mat(Size(rgbaImg.cols, rgbaImg.rows), rgbaImg.type());
		rgbaImg.copyTo(copy);
		cvtColor(copy, copy, COLOR_BGR2RGB); // なぜか色が変わるから対処
		imwrite(buff, copy);
	}
//	loopMutex.unlock();
}

void DirectionEstimator::saveImg(Mat &rgbaImg, Mat &grayImg, long milliTime)
{
	// 画像保存
//	loopMutex.lock();
	if (isSaveFrameImg) {
		struct timeval myTime;
		struct tm *time_st;
		gettimeofday(&myTime, NULL);
		time_st = localtime(&myTime.tv_sec);

		char buff[128] = "";
		//		sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%06ld.jpg",
		//				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
		//				time_st->tm_min, time_st->tm_sec, myTime.tv_usec);
		sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%19ld.jpg",
				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
				time_st->tm_min, time_st->tm_sec, milliTime);
		Mat copy = Mat(Size(rgbaImg.cols, rgbaImg.rows), rgbaImg.type());
		rgbaImg.copyTo(copy);
		cvtColor(copy, copy, COLOR_BGR2RGB); // なぜか色が変わるから対処
		imwrite(buff, copy);

		char buff2[128] = "";
		sprintf(buff2, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%19ld_Gray.jpg",
				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
				time_st->tm_min, time_st->tm_sec, milliTime);
		Mat grayCopy = Mat(Size(grayImg.cols, grayImg.rows), grayImg.type());
		grayImg.copyTo(grayCopy);
		imwrite(buff2, grayCopy);
	}
//	loopMutex.unlock();
}


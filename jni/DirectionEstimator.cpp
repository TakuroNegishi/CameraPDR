#include "DirectionEstimator.h"
#include <jni.h>
#include <android/log.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
//#include <opencv2/highgui/highgui.hpp>

#include <stdio.h>
#include <fstream>
#include <time.h>
#include <sys/time.h>
#include <math.h>

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

const int DirectionEstimator::POINT_SIZE = 5;
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
//const int DirectionEstimator::FRAME_SPAN = 5;
const int DirectionEstimator::IMG_WIDTH = 640;
const int DirectionEstimator::IMG_HEIGHT = 480;

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
}

void DirectionEstimator::init()
{
	isLoop = false;
	lastImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC4);
	lastGrayImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
	grayImg = Mat(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
	pointDetector.init();
}

void DirectionEstimator::clear()
{
	frameCount = 0;
	sumElapsedTime = 0;
	isFirstFrame = true;
	isSaveFrameImg = false;
	isFindKeyFrame = false;
	currentKpts.clear();
	prevPoints.clear();
	vanishingPoint = Point2f(320, 240);
	prevKFTime = 0;

//	matchFrameCount = 0;
//	matchVector.clear();
}

void DirectionEstimator::changeState(bool isSaveFrameImg)
{
	// lock_guardを使うと、スコープの終わりでlock()変数が破棄されるのにともなって、自動的にロックも解除される
	lock_guard<mutex> lock(loopMutex);
	isLoop = !isLoop;
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
		lastKF.set(milliTime, rgbaImg, grayImg);

		//
		if (isFirstFrame) {
			prevKFTime = milliTime;
			isFirstFrame = false;
			isFindKeyFrame = false;
			keyFrameQueue.push(lastKF);
			saveImg(lastKF.img, lastKF.timeStamp);
		}
	}

	if (!isFirstFrame && isFindKeyFrame && milliTime - prevKFTime >= 500) {
		// 500msec(0.5sec)間隔でキーフレーム候補を画像処理用キューに入れる
		prevKFTime = milliTime;
		isFindKeyFrame = false;
		keyFrameQueue.push(lastKF);
		saveImg(lastKF.img, lastKF.timeStamp);
		lastImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC4);
		lastGrayImg = Mat::zeros(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
	}

/*	// 最初のフレームのみ,現在フレーム=過去フレーム
	if (isFirstFrame) {
		rgbaImg.copyTo(prevImg); // カラー画像
		grayImg.copyTo(prevGrayImg); // グレー
	}

	// 過去フレームの特徴点検出 -> オプティカルフローで現在フレームの特徴点検出
	prevKpts.clear();	// 特徴点リストクリア
	pointDetector.detect(prevGrayImg, prevKpts); // 1フレーム前の特徴点検出
	// 一旦,Point配列に移す
	int ptsSize = prevKpts.size();
	subPrevPoints.clear();
	subPrevPoints.resize(ptsSize);
	for (int i = 0; i < ptsSize; ++i) {
		subPrevPoints[i] = prevKpts[i].pt;
	}
	// 対応のある過去,現在フレームの特徴点検出(prevPoints, currentPoints) -> スタビライズのためのオプティカルフロー計算
	calcOpticalFlow(prevGrayImg, grayImg, subPrevPoints, prevPoints, currentPoints);
	LOGE("sub = %d, prev = %d, cur = %d", subPrevPoints.size(), prevPoints.size(), currentPoints.size());

	// スタビライズ&描画する前に、元画像を一旦コピー
	rgbaImg.copyTo(rgbaCopyImg);
	grayImg.copyTo(grayCopyImg);
	// カメラ画像(書き換わる), 1フレーム前の画像, 1フレーム前の特徴点, 現在フレームの特徴点, 最初のフレームかのフラグ
	stabilizer.estimate(rgbaImg, prevImg, prevPoints, currentPoints);
	// スタビライズ後の画像(rgbaImg)からスタビライズ後のグレー画像(grayImg)生成
	cvtColor(rgbaImg, grayImg, COLOR_BGR2GRAY); // グレースケール
	// 最初のフレームは,1フレーム前のスタビライズ後グレー画像=現在フレームのスタビライズ後グレー画像
	if (isFirstFrame) grayImg.copyTo(prevStabGrayImg); // スタビライズ後のグレー画像
	// スタビライズ後の1フレ前の特徴点検出
	prevKpts.clear();
	pointDetector.detect(prevStabGrayImg, prevKpts);
	// 一旦,Point配列に移す
	ptsSize = prevKpts.size();
	subPrevPoints.clear();
	subPrevPoints.resize(ptsSize);
	for (int i = 0; i < ptsSize; ++i) {
		subPrevPoints[i] = prevKpts[i].pt;
	}
	// スタビライズ後の画像でオプティカルフロー再計算
	calcOpticalFlow(prevStabGrayImg, grayImg, subPrevPoints, prevPoints, currentPoints);

	vanishingPoint = getCrossPoint2(prevPoints, currentPoints); // 消失点計算
	draw(rgbaImg); // 特徴点等の描画
*/

//	saveImg(rgbaImg, milliTime); // 特徴点を描画した画像を保存

	// 現在フレーム情報 -> 過去フレーム情報
//	rgbaCopyImg.copyTo(prevImg); // カラー画像
//	grayCopyImg.copyTo(prevGrayImg); // グレー画像
//	grayImg.copyTo(prevStabGrayImg); // スタビライズ後のグレー画像

	end = chrono::system_clock::now();
	long elapsed = chrono::duration_cast<chrono::milliseconds>(end - start).count();
	LOGE("elapsed time(msec): %19ld", elapsed);

	sumElapsedTime += elapsed;
	long averageTime = sumElapsedTime / frameCount;
	LOGE("average time(msec): %19ld", averageTime);

	// ループ停止 -> クリア
	loopMutex.lock();
	if (!isLoop) clear();
	loopMutex.unlock();
}

// 特徴点マッチング
//void DirectionEstimator::matchImg() {
//	matchFrameCount++;
//	matchVector.clear();
//	if (matchFrameCount == FRAME_SPAN) {
//		// 特徴量記述子
//		pointDetector.describe(grayImg, currentKpts, currentDescriptor);
//		// 特徴点マッチング
//		if (prevDescriptor.total() > 0) {
//			vector<DMatch> dmatch12, dmatch21;
//			pointDetector.match(currentDescriptor, prevDescriptor, dmatch12); // current -> prev
//			pointDetector.match(prevDescriptor, currentDescriptor, dmatch21); // prev -> current
//
//			for (size_t i = 0; i < dmatch12.size(); ++i) {
//				// img1 -> img2 と img2 -> img1の結果が一致しているか検証
//				DMatch m12 = dmatch12[i];
//				DMatch m21 = dmatch21[m12.trainIdx];
//				if (m21.trainIdx == m12.queryIdx)
//					matchVector.push_back(m12);
//			}
//
//			// queryDescriptor, trainDescriptor
////			pointDetector.match(currentDescriptor, prevDescriptor, matchVector);
//		}
//	}
//}

// オプティカルフロー計算
void DirectionEstimator::calcOpticalFlow(const Mat &prev, const Mat &cur,
		const vector<Point2f> &subPrevPoints, vector<Point2f> &prevPoints, vector<Point2f> &trackedPoints)
{
/*	if (subPrevPoints.size() > 0) {
		vector<float> errors;
		vector<unsigned char> status;	// オプティカルフロー追跡結果(1:成功, 0:失敗)
		vector<Point2f> vtracked;
		// 勾配(Lucas-Kanade)法
//		calcOpticalFlowPyrLK(prevGrayImg, grayImg, prevPoints, vtracked, status, errors, Size(15, 15), 3, cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 30, 0.01), 0.5, 1);
		calcOpticalFlowPyrLK(prev, cur, subPrevPoints, vtracked, status, errors);

		if (status.size() > 0) {
			// prevPoints, trackedPointsに過去,現在の特徴点を入れる
			prevPoints.clear();
			prevPoints.reserve(subPrevPoints.size());
			trackedPoints.clear();
			trackedPoints.reserve(vtracked.size());
			for (int i = 0; i < status.size(); ++i) {
				// 距離が離れすぎているフローは排除
				float dist = getDistance(subPrevPoints[i], vtracked[i]);
				if (status[i] == 0) continue;
				else if (dist >= FLOW_LINE_MIN_LIMIT && dist <= FLOW_LINE_MAX_LIMIT) {
					prevPoints.push_back(subPrevPoints[i]);
					trackedPoints.push_back(vtracked[i]);
				}
			}
		}
		LOGE("calOF: sub = %d, vtracked = %d, prevPoints = %d, trackedPoints = %d", subPrevPoints.size(), vtracked.size(), prevPoints.size(), trackedPoints.size());
	}*/
}

//
Point2f DirectionEstimator::getCrossPoint(Point2f &firstLinePoint1, Point2f &firstLinePoint2,
		Point2f &secondLinePoint1, Point2f &secondLinePoint2)
{
	// S1　= {(P4.X - P2.X) * (P1.Y - P2.Y) - (P4.Y - P2.Y) * (P1.X - P2.X)} / 2
	float s1 = ((secondLinePoint2.x - secondLinePoint1.x) * (firstLinePoint1.y - secondLinePoint1.y)
			- (secondLinePoint2.y - secondLinePoint1.y) * (firstLinePoint1.x - secondLinePoint1.x)) / 2;
	// S2 = {(P4.X - P2.X) * (P2.Y - P3.Y) - (P4.Y - P2.Y) * (P2.X - P3.X)} / 2
	float s2 = ((secondLinePoint2.x - secondLinePoint1.x) * (secondLinePoint1.y - firstLinePoint2.y)
			- (secondLinePoint2.y - secondLinePoint1.y) * (secondLinePoint1.x - firstLinePoint2.x)) / 2;

	// C1.X　= P1.X + (P3.X - P1.X) * S1 / (S1 + S2)
	float cx = firstLinePoint1.x + (firstLinePoint2.x - firstLinePoint1.x) * s1 / (s1 + s2);
	// C1.Y　= P1.Y + (P3.Y - P1.Y) * S1 / (S1 + S2)
	float cy = firstLinePoint1.y + (firstLinePoint2.y - firstLinePoint1.y) * s1 / (s1 + s2);

	return Point2f(cx, cy);
}

Point2f DirectionEstimator::getCrossPoint2(const vector<Point2f> &prevPoints, const vector<Point2f> &curPoints)
{
	float X = 320;
	float Y = 240;
	if (prevPoints.size() != curPoints.size()) {
		LOGE("crossP miss! prevPoints = %d, curPoints = %d", prevPoints.size(), curPoints.size());
		return Point2f(X, Y);
	}
	int flowNum = curPoints.size();
	float a = 0;
	float b = 0;
	float p = 0;
	float c = 0;
	float d = 0;
	float q = 0;
	for (int i = 0; i < flowNum; ++i) {
		Point2f p1 = curPoints[i];
		Point2f p2 = prevPoints[i];

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
	X = (d * p - b * q) / (a * d - b * c);
	Y = (a * q - c * p) / (a * d - b * c);
	return Point2f(X, Y);
}

float DirectionEstimator::getDistance(const Point2f &pt1, const Point2f &pt2)
{
	float dx = pt2.x - pt1.x;
	float dy = pt2.y - pt1.y;
	return sqrt(dx * dx + dy * dy);
}

// 描画処理
void DirectionEstimator::draw(Mat &rgbaImg)
{

	// １フレーム前の特徴点
	drawPoints(rgbaImg, prevPoints, SCALAR_BLUE);
	// 現在フレームの特徴点
//	drawPoints(rgbaImg, currentPoints, SCALAR_YELLOW);

	// フロー描画
//	if (prevPoints.size() == currentPoints.size()) {
//		int flowNum = prevPoints.size();
//		for (int i = 0; i < flowNum; ++i) {
//			line(rgbaImg, prevPoints[i], currentPoints[i], SCALAR_GREEN, 3);
//		}
//	}

	// 消失点描画
	circle(rgbaImg, vanishingPoint, POINT_SIZE*3, SCALAR_CYAN, -1);

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
	loopMutex.lock();
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
	loopMutex.unlock();
}



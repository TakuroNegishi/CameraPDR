#include "DirectionEstimator.h"
#include <jni.h>
#include <android/log.h>
#include <opencv2/imgproc/imgproc.hpp>
#include "opencv2/video/tracking.hpp"
#include "opencv2/highgui/highgui.hpp"

#include <stdio.h>
#include <fstream>
#include <time.h>
#include <sys/time.h>
#include <math.h>

#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

const int DirectionEstimator::POINT_SIZE = 5;
const cv::Scalar DirectionEstimator::SCALAR_RED(255, 0, 0);
const cv::Scalar DirectionEstimator::SCALAR_GREEN(0, 255, 0);
const cv::Scalar DirectionEstimator::SCALAR_BLUE(0, 0, 255);
const cv::Scalar DirectionEstimator::SCALAR_YELLOW(255, 255, 0);
const cv::Scalar DirectionEstimator::SCALAR_PURPLE(255, 0, 255);
const cv::Scalar DirectionEstimator::SCALAR_CYAN(0, 255, 255);
const cv::Scalar DirectionEstimator::SCALAR_BLACK(0, 0, 0);
const cv::Scalar DirectionEstimator::SCALAR_WHITE(255, 255, 255);
const int DirectionEstimator::FLOW_LINE_MIN_LIMIT = 0;
const int DirectionEstimator::FLOW_LINE_MAX_LIMIT = 50;
const int DirectionEstimator::FRAME_SPAN = 5;

DirectionEstimator::DirectionEstimator()
{
	init();
	clear();
}

DirectionEstimator::~DirectionEstimator()
{

}

void DirectionEstimator::init()
{
	isLoop = false;

	// RGBAとGrayのサイズ
	int width = 640;
	int height = 480;
	grayImg = cv::Mat(cv::Size(width, height), CV_8UC1);
	prevGrayImg = cv::Mat(cv::Size(width, height), CV_8UC1);
	pointDetector.init();
}

void DirectionEstimator::clear()
{
	frameCount = 0;
	isSaveFrameImg = false;
	currentKpts.clear();
	currentPoints.clear();
	prevPoints.clear();
	status.clear();
	vtracked.clear();
	vanishingPointMean = cv::Point2f(320, 240);

//	vanishingPoints.clear();
//	matchFrameCount = 0;
//	matchVector.clear();
}

void DirectionEstimator::changeState(bool isSaveFrameImg)
{
	// lock_guardを使うと、スコープの終わりでlock()変数が破棄されるのにともなって、自動的にロックも解除される
	std::lock_guard<std::mutex> lock(loopMutex);
	isLoop = !isLoop;
	this->isSaveFrameImg = isSaveFrameImg;
}

void DirectionEstimator::estimate(cv::Mat &rgbaImg, long nanoTime)
{
	// ループ開始チェック
	loopMutex.lock();
	bool loop = isLoop;
	loopMutex.unlock();
	if (!loop) return;

	cv::cvtColor(rgbaImg, grayImg, cv::COLOR_BGR2GRAY); // グレースケール
	currentKpts.clear();	// 特徴点リストクリア
	currentPoints.clear();
	pointDetector.detect(grayImg, currentKpts); // 特徴点検出
	// Point配列に移す
	int ptsSize = currentKpts.size();
	currentPoints.resize(ptsSize);
	for (int i = 0; i < ptsSize; ++i) {
		currentPoints[i] = currentKpts[i].pt;
	}

	// 特徴点マッチング
//	matchImg();

	// オプティカルフロー計算
	calcOpticalFlow();

	// 消失点計算
//	calcVanishingPoint();
	vanishingPointMean = getCrossPoint2();

	// 描画
	draw(rgbaImg);
	// 特徴点を描画した画像を保存
	saveImg(rgbaImg, nanoTime);

	// 現在フレーム情報 -> 前フレーム情報に移行
	grayImg.copyTo(prevGrayImg); // 画像
	// 特徴点
	if (prevPoints.size() > 0)
		prevPoints.clear();
	prevPoints.resize(ptsSize);
	for (int i = 0; i < ptsSize; ++i) {
		prevPoints[i] = currentPoints[i];
	}
	// 特徴量
//	if (matchFrameCount == FRAME_SPAN) {
//		currentDescriptor.copyTo(prevDescriptor);
//		// frameカウント数リセット
//		matchFrameCount = 0;
//	}

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
//			std::vector<cv::DMatch> dmatch12, dmatch21;
//			pointDetector.match(currentDescriptor, prevDescriptor, dmatch12); // current -> prev
//			pointDetector.match(prevDescriptor, currentDescriptor, dmatch21); // prev -> current
//
//			for (size_t i = 0; i < dmatch12.size(); ++i) {
//				// img1 -> img2 と img2 -> img1の結果が一致しているか検証
//				cv::DMatch m12 = dmatch12[i];
//				cv::DMatch m21 = dmatch21[m12.trainIdx];
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
void DirectionEstimator::calcOpticalFlow() {
	vtracked.clear();
	status.clear();

	if (prevPoints.size() > 0) {
		std::vector<float> errors;
		// 勾配(Lucas-Kanade)法
//		cv::calcOpticalFlowPyrLK(prevGrayImg, grayImg, prevPoints, vtracked, status, errors, cv::Size(15, 15), 3, cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 30, 0.01), 0.5, 1);
		cv::calcOpticalFlowPyrLK(prevGrayImg, grayImg, prevPoints, vtracked, status, errors);

		// ブロックマッチング法--------
//		calcOpticalFlowFarneback(prevGrayImg, grayImg, flow, 0.5, 1, 15, 1, 5, 1.1, OPTFLOW_FARNEBACK_GAUSSIAN);
		//--------------------

		// 距離が離れすぎてる点を削除
		if (status.size() > 0) {
			for (int i = 0; i < status.size(); ++i) {
				float dist = getDistance(prevPoints[i], vtracked[i]);
				if (status[i] == 0) continue;
				else if (dist < FLOW_LINE_MIN_LIMIT || dist > FLOW_LINE_MAX_LIMIT) {
					status[i] = 0;
				}
			}
		}
	}
}

//
cv::Point2f DirectionEstimator::getCrossPoint(cv::Point2f &firstLinePoint1, cv::Point2f &firstLinePoint2,
		cv::Point2f &secondLinePoint1, cv::Point2f &secondLinePoint2)
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

	return cv::Point2f(cx, cy);
}

cv::Point2f DirectionEstimator::getCrossPoint2()
{
	float X = 0;
	float Y = 0;
	if (status.size() > 0) {
		int flowNum = status.size();
		float a = 0;
		float b = 0;
		float p = 0;
		float c = 0;
		float d = 0;
		float q = 0;
		for (int i = 0; i < flowNum; ++i) {
			if (status[i] == 0) continue;
			cv::Point2f p1 = vtracked[i];
			cv::Point2f p2 = prevPoints[i];

			// 連立方程式公式 - https://t-sv.sakura.ne.jp/text/num_ana/ren_eq22/ren_eq22.html
//			sumX += 2*X * (p1.y - p2.y) * (p1.y - p2.y) + 2*Y * (p2.x - p1.x) * (p1.y - p2.y)
//					+ 2 * (p1.x * p2.y - p2.x * p1.y) * (p1.y - p2.y); // = 0 偏微分X
//			sumY += 2*Y * (p2.x - p1.x) * (p2.x - p1.x) + 2*X * (p2.x - p1.x) * (p1.y - p2.y)
//					+ 2 * (p1.x * p2.y - p2.x * p1.y) * (p2.x - p1.x); // = 0 偏微分Y

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
	}
	return cv::Point2f(X, Y);
}

float DirectionEstimator::getDistance(const cv::Point2f &pt1, const cv::Point2f &pt2)
{
	float dx = pt2.x - pt1.x;
	float dy = pt2.y - pt1.y;
	return sqrt(dx * dx + dy * dy);
}

// 描画処理
void DirectionEstimator::draw(cv::Mat &rgbaImg)
{

	// １フレーム前の特徴点
	if (prevPoints.size() > 0) {
		for (int i = 0, n = prevPoints.size(); i < n; ++i) {
			// 描画先画像, 円中心, 半径, 色, -1=塗りつぶし
			cv::circle(rgbaImg, prevPoints[i], POINT_SIZE, SCALAR_BLUE, -1);
		}
	}

	// 現在フレームの特徴点
	if (currentPoints.size() > 0) {
		for (int i = 0, n = currentPoints.size(); i < n; ++i) {
			// 描画先画像, 円中心, 半径, 色, -1=塗りつぶし
			cv::circle(rgbaImg, currentPoints[i], POINT_SIZE, SCALAR_YELLOW, -1);
		}
	}

	// フロー描画
	if (status.size() > 0) {
		int flowNum = status.size();
		for (int i = 0; i < flowNum; ++i) {
			if (status[i] == 0) continue;
			cv::line(rgbaImg, prevPoints[i], vtracked[i], SCALAR_GREEN, 3);
		}
	}

	// BMフロー

//	  for (i = 0; i < velx->width; i++) {
//	    for (j = 0; j < vely->height; j++) {
//	      dx = (int) cvGetReal2D (velx, j, i);
//	      dy = (int) cvGetReal2D (vely, j, i);
//	      cvLine (dst_img, cvPoint (i * block_size, j * block_size),
//	              cvPoint (i * block_size + dx, j * block_size + dy), CV_RGB (255, 0, 0), 1, CV_AA, 0);
//	    }
//	  }

	// 消失点描画
//	if (vanishingPoints.size() > 0) {
//		for (int i = 0, n = vanishingPoints.size(); i < n; ++i) {
//			// 描画先画像, 円中心, 半径, 色, -1=塗りつぶし
//			cv::circle(rgbaImg, vanishingPoints[i], POINT_SIZE, SCALAR_RED, -1);
//		}
//	}
	cv::circle(rgbaImg, vanishingPointMean, POINT_SIZE*3, SCALAR_CYAN, -1);

	// 特徴点マッチング描画
//	if (matchFrameCount == FRAME_SPAN && matchVector.size() > 0) {
//		for (int i = 0; i < matchVector.size(); ++i) {
//			cv::line(rgbaImg, prevPoints[matchVector[i].trainIdx], currentPoints[matchVector[i].queryIdx], SCALAR_RED, 3);
//		}
//	}
}

void DirectionEstimator::saveImg(cv::Mat &rgbaImg, long nanoTime)
{
	// 画像保存
	loopMutex.lock();
	if (isSaveFrameImg) {
		struct timeval myTime;
		struct tm *time_st;
		gettimeofday(&myTime, NULL);
		time_st = localtime(&myTime.tv_sec);

		char buff[128] = "";
		sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/%04d-%02d-%02d_%02d;%02d;%02d;%06ld.jpg",
				time_st->tm_year + 1900, time_st->tm_mon + 1, time_st->tm_mday, time_st->tm_hour,
				time_st->tm_min, time_st->tm_sec, myTime.tv_usec);
		cv::cvtColor(rgbaImg, rgbaImg, cv::COLOR_BGR2RGB);
		cv::imwrite(buff, rgbaImg);
	}
	loopMutex.unlock();
}



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
const int DirectionEstimator::FLOW_LINE_LIMIT = 30;
const int DirectionEstimator::FRAME_SPAN = 1;

DirectionEstimator::DirectionEstimator()
{
	clear();
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
	vtracked.clear();
	status.clear();
	vanishingPoint = cv::Point2f(0, 0);
	matchFrameCount = 0;
	matchVector.clear();
}

void DirectionEstimator::changeState(bool isSaveFrameImg)
{
	// lock_guardを使うと、スコープの終わりでlock()変数が破棄されるのにともなって、自動的にロックも解除される
	std::lock_guard<std::mutex> lock(loopMutex);
	isLoop = !isLoop;
	this->isSaveFrameImg = isSaveFrameImg;
}

void DirectionEstimator::estimate(cv::Mat &rgbaImg)
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
	matchImg();

	// オプティカルフロー計算
	calcOpticalFlow();

	// 描画
	draw(rgbaImg);

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
	if (matchFrameCount == FRAME_SPAN) {
		currentDescriptor.copyTo(prevDescriptor);
		// frameカウント数リセット
		matchFrameCount = 0;
	}

	// ループ停止 -> クリア
	loopMutex.lock();
	if (!isLoop) clear();
	loopMutex.unlock();
}

// 特徴点マッチング
void DirectionEstimator::matchImg() {
	matchFrameCount++;
	matchVector.clear();
	if (matchFrameCount == FRAME_SPAN) {
		// 特徴量記述子
		pointDetector.describe(grayImg, currentKpts, currentDescriptor);
		// 特徴点マッチング
		if (prevPoints.size() > 0) {
			// queryDescriptor, trainDescriptor
			pointDetector.match(currentDescriptor, prevDescriptor, matchVector);
		}
	}
}

// オプティカルフロー計算
void DirectionEstimator::calcOpticalFlow() {
	vtracked.clear();
	status.clear();
	if (prevPoints.size() > 0) {
		std::vector<float> errors;
//		cv::calcOpticalFlowPyrLK(prevGrayImg, grayImg, prevPoints, vtracked, status, errors, cv::Size(15, 15), 3, cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 30, 0.01), 0.5, 1);
		cv::calcOpticalFlowPyrLK(prevGrayImg, grayImg, prevPoints, vtracked, status, errors);

		// 距離離れすぎてる点を削除
		if (status.size() > 0) {
			for (int i = 0; i < status.size(); ++i) {
				float dist = getDistance(prevPoints[i], vtracked[i]);
				if (status[i] == 0) continue;
				else if (dist >= FLOW_LINE_LIMIT) {
//					LOGE("sqrt = %f", dist);
					status[i] = 0;
				}
			}
		}
	}
}

void DirectionEstimator::calcVanishingPoint() {
	// 特徴点のフロー(流れ)から消失点を計算
//	if (status.size() > 0) {
//		int flowNum = status.size();
//		for (int i = 0; i < flowNum; ++i) {
//			if (status[i] == 0) continue;
//			cv::line(rgbaImg, prevPoints[i], vtracked[i], SCALAR_GREEN, 3);
//		}
//	}
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
//	if (currentPoints.size() > 0) {
//		for (int i = 0, n = currentPoints.size(); i < n; ++i) {
//			// 描画先画像, 円中心, 半径, 色, -1=塗りつぶし
//			cv::circle(rgbaImg, currentPoints[i], POINT_SIZE, SCALAR_YELLOW, -1);
//		}
//	}

	// フロー描画
	if (status.size() > 0) {
		int flowNum = status.size();
		for (int i = 0; i < flowNum; ++i) {
			if (status[i] == 0) continue;
			cv::line(rgbaImg, prevPoints[i], vtracked[i], SCALAR_GREEN, 3);
		}
	}

	// 特徴点マッチング描画
	if (matchFrameCount == FRAME_SPAN && matchVector.size() > 0) {
		for (int i = 0; i < matchVector.size(); ++i) {
			matchVector[i].queryIdx;
			matchVector[i].trainIdx;
		}
	}

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
		cv::imwrite(buff, rgbaImg);
	}
	loopMutex.unlock();
}



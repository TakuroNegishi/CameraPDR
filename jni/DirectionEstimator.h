#pragma once

#include <mutex>
#include <opencv2/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include "PointDetector.h"
#include "MTQueue.h"

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
	void calcOpticalFlow(const Mat &prevGrayImg, const Mat &curGrayImg,
			const vector<Point2f> &subPrevPoints, vector<Point2f> &prevPoints, vector<Point2f> &trackedPoints);
	Point2f getCrossPoint(Point2f &firstLinePoint1, Point2f &firstLinePoint2,
			Point2f &secondLinePoint1, Point2f &secondLinePoint2);
	Point2f getCrossPoint2(const vector<Point2f> &prevPoints, const vector<Point2f> &curPoints);
	float getDistance(const Point2f &pt1, const Point2f &pt2);
	void draw(Mat &rgbaImg);
	void drawPoints(Mat &rgbaImg, const vector<Point2f> &points, const Scalar color);
	void saveImg(Mat &rgbaImg, long milliTime);

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
	vector<Point2f> prevPoints;	// 1フレーム前の特徴点

//	Mat currentDescriptor;	// 現在画像の特徴量
//	Mat prevDescriptor;		// 1フレーム前のの特徴量
//	int matchFrameCount;	// 特徴点マッチングを行うフレーム周期
//	vector<DMatch> matchVector; // 現在画像と1フレーム前の特徴点のマッチング結果
	Point2f vanishingPoint; // 現在フレームの消失点
};

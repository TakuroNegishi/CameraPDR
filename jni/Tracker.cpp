#include "Tracker.h"

#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>

using namespace std;
using namespace cv;

Tracker::Tracker()
{
	//	int threshold = 10,	bool nonmaxSuppression = true, int 	type = FastFeatureDetector::TYPE_9_16)
	/* non-maxsuppression = 「最大でない値は(すべて)値を抑える(=値をゼロにする)」 */
	fastDetector = FastFeatureDetector::create(40, true, FastFeatureDetector::TYPE_9_16);
	lastKF = new KeyFrame();
	clear();
}

Tracker::~Tracker()
{
	clear();
	grayImg.release();
	vector<KeyPoint>().swap(currentKpts);
	delete lastKF;
}

void Tracker::clear()
{
	isFirstFrame = true;
	isFindKeyFrame = false;
	prevKFTime = 0;
	currentKpts.clear();
	lastKF->init();
}


bool Tracker::tracking(const Mat &cameraImg, const long long milliTime)
{
	cvtColor(cameraImg, grayImg, COLOR_BGR2GRAY); // グレースケール
	detect(grayImg, currentKpts);
	if (currentKpts.size() > 20) {
		// 20ポイント以上特徴点が取れれば,キーフレーム候補に
		isFindKeyFrame = true;
		lastKF->set(milliTime, cameraImg, grayImg);

		if (isFirstFrame) {
			prevKFTime = milliTime;
			isFirstFrame = false;
			isFindKeyFrame = false;
			//keyFrameQueue.push(lastKF);
			return true;
		}
	}

	//cout << "sabun: " << (milliTime - prevKFTime) << endl;
	if (!isFirstFrame && isFindKeyFrame && (milliTime - prevKFTime) >= 500) {
		// 500msec(0.5sec)間隔でキーフレーム候補を画像処理用キューに入れる
		prevKFTime = milliTime;
		isFindKeyFrame = false;
		//keyFrameQueue.push(lastKF);
		return true;
	}

	return false;
}

KeyFrame Tracker::getLastKF()
{
	//KeyFrame kf;
	//kf.set(lastKF->timeStamp, lastKF->img, lastKF->grayImg);
	//return kf;

	return *lastKF;
}

void Tracker::detect(const Mat &img, vector<KeyPoint> &keyPoints) const
{
	fastDetector->detect(img, keyPoints);
}

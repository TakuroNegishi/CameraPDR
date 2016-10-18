#include "VanishingPointEstimator.h"
#include <iostream>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <time.h>

using namespace std;
using namespace cv;

const int VanishingPointEstimator::POINT_SIZE = 3;
const Scalar VanishingPointEstimator::SCALAR_RED(255, 0, 0);
const Scalar VanishingPointEstimator::SCALAR_GREEN(0, 255, 0);
const Scalar VanishingPointEstimator::SCALAR_BLUE(0, 0, 255);
const Scalar VanishingPointEstimator::SCALAR_YELLOW(255, 255, 0);
const float VanishingPointEstimator::ERROR_VP = -999.0f;
const int VanishingPointEstimator::LEFT_VP = -1;
const int VanishingPointEstimator::NORMAL_VP = 0;
const int VanishingPointEstimator::RIGHT_VP = 1;

VanishingPointEstimator::VanishingPointEstimator()
{
	// threshold=0.001(default)
	akazeDetector = AKAZE::create(5, 0, 3, 0.0005, 4, 4, 1);
	//	mAKAZEDetector = AKAZE::create(5, 0, 3, 0.0005, 4, 4, 1);
	matcher = DescriptorMatcher::create("BruteForce-Hamming");
	maFilterX = new MovingAverageFilter();
	maFilterY = new MovingAverageFilter();
	clear();
	ofs.open("/storage/emulated/0/negishi.deadreckoning/Feature Image/log.txt");
}


VanishingPointEstimator::~VanishingPointEstimator()
{
	clear();
	vector<KeyPoint>().swap(prevKpts);
	prevDesc.release();
	vector<Point2f>().swap(pointHistory);
	vector<Point2f>().swap(pointHistoryMA);
	delete maFilterX;
	delete maFilterY;
}

void VanishingPointEstimator::clear()
{
	isFirstProc = true;
	prevKpts.clear();
	pointHistory.clear();
	pointHistoryMA.clear();
	sidewayStatus = 0;

	// PDR側との排他制御
	sidewayMutex.lock();
	procCount = 0;
	startTime = 0;
	endTime = 0;
	startVPStatus = NORMAL_VP;
	sideStartTime = 0;
	sideEndTime = 0;
	sidewayMutex.unlock();

	maFilterX->clear();
	maFilterY->clear();
}

void VanishingPointEstimator::calcVP(KeyFrame &currentKF)
{
	sidewayMutex.lock();
	procCount++;
	sidewayMutex.unlock();
	cout << "calcVP()--------" << endl;
	cout << "poping..." << endl;
	//KeyFrame currentKF = keyFrameQueue.pop();
	cout << "poped!..." << endl;
	//if (currentKF.timeStamp == -1) break; // pop操作を強制中断された場合
	detect(currentKF.grayImg, currentKF.kpts);
	describe(currentKF.grayImg, currentKF.kpts, currentKF.desc);
	cout << "currentKF.timeStamp: " << currentKF.timeStamp << endl;
	cout << "currentKF.kpts: " << currentKF.kpts.size() << endl;
	cout << "prevKpts: " << prevKpts.size() << endl;
	if (isFirstProc) {
		// current >> prev
		prevKpts = currentKF.kpts;
		currentKF.desc.copyTo(prevDesc);
		isFirstProc = false;
		return;
	}
	// matching
	vector<DMatch> inlierMatches = calcMatchingFlow(prevKpts, prevDesc, currentKF.kpts, currentKF.desc);

	//--- 消失点計算 ----
	Point2f vp = getCrossPoint(inlierMatches, currentKF.kpts, prevKpts);
	Point2f vpMA;
	//pointHistory.push_back(vp); // 消失点計算
	int vpStatus;
	if (vp.x == ERROR_VP && vp.y == ERROR_VP) {
		// 消失点計算エラー
		//pointHistoryMA.push_back(vp);
		vpMA = Point2f(ERROR_VP, ERROR_VP);
		vpStatus = static_cast<int>(ERROR_VP);
	}
	else {
		// 消失点計算成功
		vpMA = Point2f(maFilterX->update(vp.x), maFilterY->update(vp.y));
		//pointHistoryMA.push_back(vpMA);

		if (vpMA.x < (0 + 50)) {
			vpStatus = LEFT_VP;
		} else if (vpMA.x > (640 - 50)) {
			vpStatus = RIGHT_VP;
		} else {
			vpStatus = NORMAL_VP;
		}
	}
	ofs << currentKF.timeStamp << "," << vp.x << "," << currentKF.timeStamp << "," << vpMA.x;
	if (vpStatus == ERROR_VP)
		ofs << ",ERROR" << endl;
	else if (vpStatus == NORMAL_VP)
		ofs << ",NORMAL" << endl;
	else if (vpStatus == RIGHT_VP)
		ofs << ",RIGHT" << endl;
	else if (vpStatus == LEFT_VP)
		ofs << ",LEFT" << endl;

	// 横向き歩き判定
	sidewayMutex.lock();
	if (sidewayStatus == 0 && startTime == 0 && endTime == 0) { // start,end==0 -> PDR側からの取得待ち
		if (vpStatus == LEFT_VP || vpStatus == RIGHT_VP) {
			// 左右どちらかに
			startTime = currentKF.timeStamp;
			startVPStatus = vpStatus;
			sidewayStatus = 1;
			cout << "sidewayStatus: 0 -> 1" << endl;
		}
	}
	else if (sidewayStatus == 1) {
		// 逆向き判定
		if (startVPStatus == LEFT_VP && vpStatus == RIGHT_VP || startVPStatus == RIGHT_VP && vpStatus == LEFT_VP) {
			sidewayStatus = 2;
			sideStartTime = currentKF.timeStamp; // 横向き区間開始
			cout << "sidewayStatus: 1 -> 2" << endl;
		}
		else if ((currentKF.timeStamp - startTime) >= 5000) {
			// 5秒経過しても逆向きにならない
			startTime = 0;
			startVPStatus = NORMAL_VP;
			sidewayStatus = 0;
			cout << "sidewayStatus: 1 -> 0" << endl;
		}
	}
	else if (sidewayStatus == 2) {
		// 横向き->正面に戻ってきた時
		if (vpStatus == NORMAL_VP) {
			endTime = currentKF.timeStamp;
			sideEndTime = currentKF.timeStamp - 500; // 横向き区間終了(正面向いた時の一個前 -> 500msec前)
			// TODO ここでPDR側にどうにかしてstartTime,endTimeを送る
			ofs << "SIDE_SECTION," << startTime << "," << endTime << endl;
			sidewayStatus = 0;
			cout << "sidewayStatus: 2 -> 0" << endl;
		}
	}
	sidewayMutex.unlock();

	/*** draw debug ***/
//	saveDebug(currentKF.img, inlierMatches, prevKpts, currentKF.kpts, currentKF.timeStamp, vpMA);
	/*** draw debug ***/

	// current >> prev
	prevKpts = currentKF.kpts;
	currentKF.desc.copyTo(prevDesc);

	//		end = chrono::system_clock::now();
	//		long elapsed = chrono::duration_cast<chrono::milliseconds>(end - start).count();
	//		LOGE("elapsed time(msec): %ld", elapsed);

	cout << "finish procCalcVP()-----------------" << endl;
}

vector<DMatch> VanishingPointEstimator::calcMatchingFlow(const vector<KeyPoint> &prevKpts, const Mat &prevDesc, const vector<KeyPoint> &currentKpts, const Mat &currentDesc)
{
	vector<DMatch> inlierMatches;
	vector<DMatch> matchVector, matchPrevToCur, matchCurToPrev;
	if (prevDesc.rows == 0 || currentDesc.rows == 0) return inlierMatches;

	// prev >> current, current >> prev 双方向でマッチング
	match(prevDesc, currentDesc, matchPrevToCur); // prev=query, current=train
	match(currentDesc, prevDesc, matchCurToPrev);
	if (matchPrevToCur.size() == 0 || matchCurToPrev.size() == 0) return inlierMatches;

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
	//H = findHomography(goodPrevPts, goodCurrentPts, masks, RANSAC, 3.f);
	H = findHomography(goodPrevPts, goodCurrentPts, masks, RANSAC, 6.f);

	//RANSACで使われた対応点のみ抽出
	for (int i = 0; i < masks.rows; ++i) {
		uchar *inlier = masks.ptr<uchar>(i);
		if (inlier[0] == 1) {
			inlierMatches.push_back(matchVector[i]);
		}
	}
	return inlierMatches;
}

Point2f VanishingPointEstimator::getCrossPoint(const vector<DMatch>& matchVector,
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

float VanishingPointEstimator::getDistance(const Point2f &pt1, const Point2f &pt2)
{
	float dx = pt2.x - pt1.x;
	float dy = pt2.y - pt1.y;
	return sqrt(dx * dx + dy * dy);
}

void VanishingPointEstimator::getStartEndTime(long long startEndTime[])
{
	sidewayMutex.lock();
	if (endTime == 0) {
		// 横向き期間未観測
		startEndTime[0] = 0;
		startEndTime[1] = 0;
	} else {
		startEndTime[0] = startTime;
		startEndTime[1] = endTime;
		// VPThread側に横向き判定の計測許可
		startTime = 0;
		endTime = 0;
	}
	startEndTime[2] = procCount; // 処理回数
	startEndTime[3] = sidewayStatus;
	startEndTime[4] = sideStartTime; // 横向き区間開始
	startEndTime[5] = sideEndTime;	 // 横向き区間終了
	sidewayMutex.unlock();
}

void VanishingPointEstimator::saveDebug(Mat &img, const vector<DMatch> &matches, const vector<KeyPoint> &prev, const vector<KeyPoint> &cur, long long milliTime, Point2f vp)
{
	Mat copy;
	img.copyTo(copy);

	// 特徴点描画
	for (int i = 0; i < prev.size(); ++i) {
		circle(copy, prev[i].pt, POINT_SIZE, SCALAR_BLUE, -1);
	}
	for (int i = 0; i < cur.size(); ++i) {
		circle(copy, cur[i].pt, POINT_SIZE, SCALAR_YELLOW, -1);
	}
	// フロー描画
	for (int i = 0; i < matches.size(); ++i) {
		line(copy, prev[matches[i].queryIdx].pt, cur[matches[i].trainIdx].pt, SCALAR_GREEN, 3);
	}
	// 消失点描画
	circle(copy, vp, POINT_SIZE*3, SCALAR_RED, -1);

	//char buff[128] = "";
	//sprintf_s(buff, "test_%lld", milliTime);
	//imshow(buff, copy);

	saveImg(copy, milliTime);
}

void VanishingPointEstimator::saveImg(const Mat &img, long long milliTime)
{
	//time_t long_time;
	//struct tm now_time;                 // ポインタから、変数実体に変更
	//time(&long_time);
	//localtime_s(&now_time, &long_time);  // 戻り値から引数に変更

	char buff[256] = "";
	sprintf(buff, "/storage/emulated/0/negishi.deadreckoning/Feature Image/%lld.jpg", milliTime);
	//cvtColor(rgbaImg, copy, COLOR_BGR2RGB); // なぜか色が変わるから対処
	//imshow("color", rgbaImg);
	imwrite(buff, img);
}

void VanishingPointEstimator::match(const Mat &query, const Mat &train, vector<DMatch> &vmatch) const
{
	matcher->match(query, train, vmatch);
}

void VanishingPointEstimator::describe(const Mat &img, vector<KeyPoint> &vkpt, Mat &vdesc) const
{
	akazeDetector->compute(img, vkpt, vdesc);
}

void VanishingPointEstimator::detect(const Mat &img, vector<KeyPoint> &vkpt) const
{
	akazeDetector->detect(img, vkpt);
}

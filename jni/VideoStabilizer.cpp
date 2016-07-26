/*!
@file		VideoStabilizer.cpp
@brief		functions in VideoStabilizer
*/
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
//#include <opencv2/highgui/highgui.hpp>
#include <math.h>
#include <fstream>

#include "VideoStabilizer.h"
#include <jni.h>
#include <android/log.h>
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

const int VideoStabilizer::HORIZONTAL_BORDER_CROP = 20; // In pixels. Crops the border to reduce the black borders from stabilisation being too noticeable.
const double VideoStabilizer::PSTD = 4e-3;
const double VideoStabilizer::CSTD = 0.25;
const Trajectory VideoStabilizer::Q(PSTD, PSTD, PSTD);
const Trajectory VideoStabilizer::R(CSTD, CSTD, CSTD);
const Mat VideoStabilizer::T(2, 3, CV_64F);
const int VideoStabilizer::VERT_BOODER = HORIZONTAL_BORDER_CROP * 480 / 640; // aspect ratio correct


VideoStabilizer::VideoStabilizer()
{

}

VideoStabilizer::~VideoStabilizer()
{
}

void VideoStabilizer::init()
{

}

void VideoStabilizer::clear()
{
	prev_to_cur_transform.clear();
	a = 0;
	x = 0;
	y = 0;
	trajectory.clear();
	smoothed_trajectory.clear();
	X.init();
	X_.init();
	P.init();
	P_.init();
	K.init();
	z.init();
	new_prev_to_cur_transform.clear();
	k = 1;
//	last_T.clear();
}

void VideoStabilizer::release()
{
	// 一時オブジェクトと交換してデストラクタを強制的に動かす
	vector<TransformParam>().swap(prev_to_cur_transform);
	vector<Trajectory>().swap(trajectory);
	vector<Trajectory>().swap(smoothed_trajectory);
	vector<TransformParam>().swap(new_prev_to_cur_transform);
	last_T.release();
}

void VideoStabilizer::estimate(Mat& cur, const Mat& prev, const vector<Point2f> prev_corner, const vector<Point2f> cur_corner)
{
	char buff[128] = "";
	sprintf(buff, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/old.txt");
	std::ofstream oldOfs(buff, ios::app);
	char buff2[128] = "";
	sprintf(buff2, "/storage/emulated/legacy/negishi.deadreckoning/Feature Image/new.txt");
	std::ofstream newOfs(buff2, ios::app);

	// translation + rotation only
	LOGE("%d: start estimateRigidTransform", k);
	LOGE("k = %d, prev_corner = %d, cur_corner = %d", k, prev_corner.size(), cur_corner.size());
	Mat T = estimateRigidTransform(prev_corner, cur_corner, false); // false = rigid transform, no scaling/shearing
	// in rare cases no transform is found. We'll just use the last known good transform.
	if(T.data == NULL) last_T.copyTo(T);
	T.copyTo(last_T);

	// decompose T
	double dx = T.at<double>(0, 2); // 行(y),列(x)
	double dy = T.at<double>(1, 2);
	double da = atan2(T.at<double>(1, 0), T.at<double>(0, 0));

	// prev_to_cur_transform.push_back(TransformParam(dx, dy, da));
	LOGE("k = %d, dx = %f, dy = %f, da = %f", k, dx, dy, da);
	oldOfs << k << "," << dx << "," << dy << "," << da << std::endl;
	// Accumulated frame to frame transform
	x += dx;
	y += dy;
	a += da;
	// trajectory.push_back(Trajectory(x,y,a));
//	LOGE("k = %d, x = %f, y = %f, a = %f", k, x, y, a);
	z = Trajectory(x, y, a);
	if (k == 1){
		// intial guesses
		X = Trajectory(0, 0, 0); //Initial estimate,  set 0
		P = Trajectory(1, 1, 1); //set error variance,set 1
	} else {
		//time update prediction
		X_ = X; //X_(k) = X(k-1);
		P_ = P + Q; //P_(k) = P(k-1)+Q;
		// measurement update£¨correction£©
		K = P_ / ( P_ + R ); //gain;K(k) = P_(k)/( P_(k)+R );
		X = X_ + K * (z - X_); //z-X_ is residual,X(k) = X_(k)+K(k)*(z(k)-X_(k));
		P = (Trajectory(1, 1, 1) - K) * P_; //P(k) = (1-K(k))*P_(k);
	}
	//smoothed_trajectory.push_back(X);
//	LOGE("k = %d, X.x = %f, X.y = %f, X.a = %f", k, X.x, X.y, X.a);
	// target - current
	double diff_x = X.x - x;
	double diff_y = X.y - y;
	double diff_a = X.a - a;
	dx = dx + diff_x;
	dy = dy + diff_y;
	da = da + diff_a;

	//new_prev_to_cur_transform.push_back(TransformParam(dx, dy, da));
	LOGE("k = %d, new dx = %f, dy = %f, da = %f", k, dx, dy, da);
	newOfs << k << "," << dx << "," << dy << "," << da << std::endl;
	T.at<double>(0,0) = cos(da);
	T.at<double>(0,1) = -sin(da);
	T.at<double>(1,0) = sin(da);
	T.at<double>(1,1) = cos(da);
	T.at<double>(0,2) = dx;
	T.at<double>(1,2) = dy;
	Mat cur2;
	// アフィン変換適応 >> 前景画像, 背景画像, 前景画像の変形行列, 出力サイズ
	// cur2がglobal motionを取り除いたcurに代わる現在フレームのカメラ画像になる
	warpAffine(prev, cur2, T, cur.size());
	// 外枠の欠落部分を埋めるように?
//	cur2 = cur2(Range(VERT_BOODER, cur2.rows - VERT_BOODER), Range(HORIZONTAL_BORDER_CROP, cur2.cols - HORIZONTAL_BORDER_CROP));
	// Resize cur2 back to cur size, for better side by side comparison
//	resize(cur2, cur2, cur.size());

//	LOGE("Frame: %d, good optical flow:  = %d", k, prev_corner.size());
	k++;
	cur2.copyTo(cur); // 正しいアフィン変換適用後の画像を現在フレームを表示するためカメラ画像にコピー
}

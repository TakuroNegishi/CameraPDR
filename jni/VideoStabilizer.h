/*!
@file		VideoStabilizer.h
@brief		header of VideoStabilizer
*/
#pragma once

#include <opencv2/core.hpp>

using namespace cv;
using namespace std;

struct TransformParam
{
    TransformParam() {}
    TransformParam(double _dx, double _dy, double _da) {
        dx = _dx;
        dy = _dy;
        da = _da;
    }

    double dx;
    double dy;
    double da; // angle
};

struct Trajectory
{
    Trajectory() {}
    Trajectory(double _x, double _y, double _a) {
        x = _x;
        y = _y;
        a = _a;
    }
    Trajectory init() {
        x = 0;
        y = 0;
        a = 0;
    }
	// "+"
	Trajectory operator+(const Trajectory  &c){
		return Trajectory(x + c.x, y + c.y, a + c.a);
	}
	// "-"
	Trajectory operator-(const Trajectory  &c){
		return Trajectory(x - c.x, y - c.y, a - c.a);
	}
	// "*"
	Trajectory operator*(const Trajectory  &c){
		return Trajectory(x * c.x, y * c.y, a * c.a);
	}
	// "/"
	Trajectory operator/(const Trajectory  &c){
		return Trajectory(x / c.x, y / c.y, a / c.a);
	}
	//"="
	Trajectory operator=(const Trajectory &rx){
		x = rx.x;
		y = rx.y;
		a = rx.a;
		return Trajectory(x, y, a);
	}

    double x;
    double y;
    double a; // angle
};

class VideoStabilizer
{
public:
	VideoStabilizer();
	~VideoStabilizer();

	void init();
	void clear();
	void release();
	void estimate(Mat& cur, const Mat& prev, const vector<Point2f> prev_corner, const vector<Point2f> cur_corner);

private:
	static const int HORIZONTAL_BORDER_CROP; // In pixels. Crops the border to reduce the black borders from stabilisation being too noticeable.
	static const double PSTD;
	static const double CSTD;
	static const Trajectory Q; // process noise covariance
	static const Trajectory R; // measurement noise covariance
	static const Mat T;
	static const int VERT_BOODER; // aspect ratio correct

	vector<TransformParam> prev_to_cur_transform; // previous to current
	// frame to frame transform
	double a;
	double x;
	double y;
	vector<Trajectory> trajectory; // trajectory at all frames
	vector<Trajectory> smoothed_trajectory; // trajectory at all frames

	Trajectory X;	// posteriori state estimate
	Trajectory X_;	// priori estimate
	Trajectory P;	// posteriori estimate error covariance
	Trajectory P_;	// priori estimate error covariance
	Trajectory K;	// gain
	Trajectory z;	// actual measurement

	vector<TransformParam> new_prev_to_cur_transform;
	int k; // loop num
	Mat last_T;

};


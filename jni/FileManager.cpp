#include "FileManager.h"
#include <fstream>
#include <chrono>
//#include <iostream>
#include <opencv2/imgproc/imgproc.hpp>
#include <android/log.h>
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

using namespace std;
using namespace cv;

// private

bool FileManager::writeMatBinary(ofstream& ofs, const Mat& out_mat, const vector<DMatch>& matchVector,
		const vector<KeyPoint>& currentKpts, const vector<KeyPoint>& prevKpts, const Point2f& vpMA)
{
	if(!ofs.is_open()){
		return false;
	}
	if(out_mat.empty()){
		int s = 0;
		ofs.write((const char*)(&s), sizeof(int));
		return true;
	}
	int type = out_mat.type();
	ofs.write((const char*)(&out_mat.rows), sizeof(int));
	ofs.write((const char*)(&out_mat.cols), sizeof(int));
	ofs.write((const char*)(&type), sizeof(int));
	ofs.write((const char*)(out_mat.data), out_mat.elemSize() * out_mat.total());
	// フローの数
	int flowNum = (int)(matchVector.size());
	ofs.write((const char*)(&flowNum), sizeof(int));
	for (int i = 0; i < flowNum; ++i) {
		Point2f prevP = prevKpts[matchVector[i].queryIdx].pt;
		Point2f currentP = currentKpts[matchVector[i].trainIdx].pt;
		ofs.write((const char*)(&prevP), sizeof(Point2f));
		ofs.write((const char*)(&currentP), sizeof(Point2f));
	}
	ofs.write((const char*)(&vpMA), sizeof(Point2f));

	return true;
}

// public

bool FileManager::SaveMatBinary(const string& filename, const Mat& output, const vector<DMatch>& matchVector,
		const vector<KeyPoint>& currentKpts, const vector<KeyPoint>& prevKpts, const Point2f& vpMA)
{
	chrono::system_clock::time_point  start;
	start = chrono::system_clock::now();

	Mat copy;
	cvtColor(output, copy, COLOR_BGR2RGB); // なぜか色が変わるから対処
	ofstream ofs(filename, ios::binary);
	bool success = writeMatBinary(ofs, copy, matchVector, currentKpts, prevKpts, vpMA);
	//処理に要した時間をミリ秒に変換
	double elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now() - start).count();
	LOGE("elapsed: %lf msec", elapsed);

	return success;
}

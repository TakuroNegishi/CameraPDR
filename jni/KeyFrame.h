/*!
@file		KeyFrame.h
@brief		header of KeyFrame
*/
#pragma once

#include <opencv2/core.hpp>

class KeyFrame
{
public:
	KeyFrame();
	~KeyFrame();

	void init();
	void clear();
	void release();
	void set(const long& time, const cv::Mat& a_img, const cv::Mat& a_grayImg);
	long timeStamp;
	cv::Mat img;
	cv::Mat grayImg;
private:
	static const int IMG_WIDTH;
	static const int IMG_HEIGHT;


	std::vector<cv::KeyPoint> currentKpts;
	cv::Mat currentDesc;
	std::vector<cv::DMatch> matchVector;
	cv::Point vanishingPoint;
	cv::Point vanishingFltPoint;
};


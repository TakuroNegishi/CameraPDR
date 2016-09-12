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
	void set(long time, cv::Mat a_img, cv::Mat a_grayImg);
	long timeStamp;
	cv::Mat img;
	cv::Mat grayImg;
	std::vector<cv::KeyPoint> kpts;
	cv::Mat desc;
private:
	static const int IMG_WIDTH;
	static const int IMG_HEIGHT;

	std::vector<cv::DMatch> matchVector;
	cv::Point vanishingPoint;
	cv::Point vanishingFltPoint;
};


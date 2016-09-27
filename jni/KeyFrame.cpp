/*!
@file		KeyFrame.cpp
@brief		functions in KeyFrame
*/
#include "KeyFrame.h"

using namespace cv;
using namespace std;

const int KeyFrame::IMG_WIDTH = 640;
const int KeyFrame::IMG_HEIGHT = 480;

KeyFrame::KeyFrame()
{
	init();
}

KeyFrame::~KeyFrame()
{
	release();
}

// コピーコンストラクタ
KeyFrame::KeyFrame(const KeyFrame &obj)
{
	this->set(obj.timeStamp, obj.img, obj.grayImg);
}

void KeyFrame::init()
{
	clear();
}

void KeyFrame::clear()
{
	timeStamp = -1;
	//img = Mat(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC4);
	//grayImg = Mat(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
	kpts.clear();
	matchVector.clear();
}

void KeyFrame::release()
{
	clear();
	img.release();
	grayImg.release();
	desc.release();
	vector<KeyPoint>().swap(kpts);
	vector<DMatch>().swap(matchVector);
}

void KeyFrame::set(long long time, const cv::Mat &a_img, const cv::Mat &a_grayImg)
{
	timeStamp = time;
	a_img.copyTo(img);
	a_grayImg.copyTo(grayImg);
}

void KeyFrame::set(const KeyFrame &obj)
{
	set(obj.timeStamp, obj.img, obj.grayImg);
}

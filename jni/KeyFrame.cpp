/*!
@file		KeyFrame.cpp
@brief		functions in KeyFrame
*/
#include "KeyFrame.h"
#include <jni.h>
#include <android/log.h>
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

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

void KeyFrame::init()
{
	clear();
}

void KeyFrame::clear()
{
	timeStamp = -1;
	img = Mat(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC4);
	grayImg = Mat(Size(IMG_WIDTH, IMG_HEIGHT), CV_8UC1);
	kpts.clear();
	matchVector.clear();
}

void KeyFrame::release()
{
	img.release();
	grayImg.release();
	desc.release();
}

void KeyFrame::set(long time, cv::Mat a_img, cv::Mat a_grayImg)
{
	timeStamp = time;
	img = a_img;
	grayImg = a_grayImg;
}

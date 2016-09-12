/*!
@file		MTQueue.h
@brief		header of MTQueue
*/
#pragma once

#include <queue>
#include <mutex>
#include <condition_variable>
#include "KeyFrame.h"

class MTQueue
{
public:
	MTQueue();
	void clear();
	void push(KeyFrame kf);
	KeyFrame pop();
	void sendFinishPopSignal();
	int size();

private:
	static const int CAPACITY;
	std::queue<KeyFrame> keyframeQueue;
	std::mutex mtx;
	std::condition_variable cvNoFull;   // 満杯でなくなった
	std::condition_variable cvNoEmpty;  // 空キューでなくなった
};


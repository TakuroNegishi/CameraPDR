/*!
@file		MTQueue.cpp
@brief		functions in MTQueue
*/
#include "MTQueue.h"
#include <jni.h>
#include <android/log.h>
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __FILE__, __VA_ARGS__))

using namespace std;

const int MTQueue::CAPACITY = 99999;

MTQueue::MTQueue()
{
	clear();
}

void MTQueue::clear()
{
//	keyframeQueue.clear();
	// 空のコンテナとswapして疑似クリア
	queue<KeyFrame> empty;
	swap(keyframeQueue, empty);
}

void MTQueue::push(KeyFrame kf)
{
	unique_lock<mutex> lk(mtx);  // ロック獲得
	while (keyframeQueue.size() == CAPACITY) {
		LOGE("CAPACITY MAX...");
		cvNoFull.wait(lk);    // "満杯でなくなった"シグナルがあるまで待機
	}
	bool emptySignal = keyframeQueue.empty();  // 操作前が空キューのときのみ
	keyframeQueue.push(kf);
	if (emptySignal)
	  cvNoEmpty.notify_all();   // 「空キューでなくなった」通知
	// mtxロック解放
}

KeyFrame MTQueue::pop()
{
	unique_lock<mutex> lk(mtx);  // ロック獲得
	while (keyframeQueue.empty()) {
		cvNoEmpty.wait(lk);    // "空でなくなった"シグナルがあるまで待機
	}
	if (keyframeQueue.size() == 0) {
		// pop命令中止処理
		return KeyFrame();
	}

	bool fullSignal = (keyframeQueue.size() == CAPACITY);	// 操作前が満杯のときのみ
	KeyFrame data = keyframeQueue.front();
	keyframeQueue.pop();
	if (fullSignal)
		cvNoFull.notify_all();  // 要素数変更を通知
	return data;
	// mtxロック解放
}

// popの待機を中止してpop命令を終了させるシグナルを送る
void MTQueue::sendFinishPopSignal()
{
	LOGE("sendFinishPopSignal...");
	unique_lock<mutex> lk(mtx);
	cvNoEmpty.notify_all();
	LOGE("sendFinishPopSignal() finish!");
	// mtxロック解放
}

int MTQueue::size()
{
	return keyframeQueue.size();
}



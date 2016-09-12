#include "MovingAverageFilter.h"

MovingAverageFilter::MovingAverageFilter() : total(0.0), pointer(0)
{
	clear();
}


MovingAverageFilter::~MovingAverageFilter()
{
}

float MovingAverageFilter::update(const float measurement)
{
	total -= data[pointer];
	total += measurement;
	data[pointer] = measurement;
	pointer++;
	if (pointer == WIN_SIZE) {
		pointer = 0;
		isFilledDataArray = true;
	}
	// dataが埋まらない最初の方は、data数で平均
	if (isFilledDataArray)
		return total / WIN_SIZE;
	else
		return total / pointer;
}

void MovingAverageFilter::clear()
{
	for (int i = 0; i < WIN_SIZE; i++) data[i] = 0.0;
	isFilledDataArray = false;
}

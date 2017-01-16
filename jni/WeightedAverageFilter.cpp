#include "WeightedAverageFilter.h"

// クラス外でないと値を代入できない
const float WeightedAverageFilter::FILTER_WEIGHT = 0.3f;

WeightedAverageFilter::WeightedAverageFilter()
{
	clear();
}


WeightedAverageFilter::~WeightedAverageFilter()
{
//	std::cout << "WeightedAverageFilter destractor" << std::endl;
}

float WeightedAverageFilter::update(const float measurement)
{
	if (isFirstData) {
		isFirstData = false;
		prevData = measurement * FILTER_WEIGHT + measurement * (1.0f - FILTER_WEIGHT);
	}
	else {
		prevData = measurement * FILTER_WEIGHT + prevData * (1.0f - FILTER_WEIGHT);
	}
	return prevData;
}

void WeightedAverageFilter::clear()
{
	isFirstData = true;
	prevData = 0.0;
}

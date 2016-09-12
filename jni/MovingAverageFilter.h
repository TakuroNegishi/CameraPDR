#ifndef MOVING_AVERAGE_FILTER_H
#define MOVING_AVERAGE_FILTER_H

class MovingAverageFilter
{
private:
	const static int WIN_SIZE = 4; // ウインドウサイズ
	float data[WIN_SIZE]; // 
	float total;
	int pointer;
	bool isFilledDataArray;
public:
	MovingAverageFilter();
	~MovingAverageFilter();
	float update(const float measurement);
	void clear();
};

#endif // MOVING_AVERAGE_FILTER_H

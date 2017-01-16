#ifndef FILE_MANAGER_H
#define FILE_MANAGER_H

#include <opencv2/core.hpp>
#include "opencv2/features2d.hpp"

class FileManager
{
private:
	static bool writeMatBinary(std::ofstream& ofs, const cv::Mat& out_mat,
			const std::vector<cv::DMatch>& matchVector, const std::vector<cv::KeyPoint>& currentKpts,
			const std::vector<cv::KeyPoint>& prevKpts, const cv::Point2f& vpMA);
public:
	static bool SaveMatBinary(const std::string& filename, const cv::Mat& output,
			const std::vector<cv::DMatch>& matchVector, const std::vector<cv::KeyPoint>& currentKpts,
			const std::vector<cv::KeyPoint>& prevKpts, const cv::Point2f& vpMA);
};

#endif // FILE_MANAGER_H

#pragma once
#include <string>
#include <chrono>
#include <thread>
#include <vector>
#include <algorithm>
#include <cmath>

namespace ravex {
namespace benchmark {

class Timer {
public:
    Timer();
    void reset();
    double elapsedMs() const;
    double elapsedSec() const;
private:
    std::chrono::high_resolution_clock::time_point start;
};

class Statistics {
public:
    void addSample(double val);
    double min() const;
    double max() const;
    double mean() const;
    double median() const;
    double stddev() const;
    int count() const;
private:
    std::vector<double> samples;
};

std::string formatSize(double bytes);
std::string formatNumber(double n);
std::string formatMs(double ms);
std::string cpuVendor();
std::string cpuModel();
int cpuCores();
int cpuThreads();
size_t totalRamMb();
std::string osName();
std::string osVersion();

}
}

#pragma once
#include "benchmark.hpp"
#include <string>
#include <fstream>

namespace ravex {
namespace benchmark {

class DiskBench {
public:
    DiskBench();
    ~DiskBench();
    double runSequentialRead(const std::string& path, size_t sizeMb);
    double runSequentialWrite(const std::string& path, size_t sizeMb);
    double runRandomRead4K(const std::string& path, int ops);
    double runRandomWrite4K(const std::string& path, int ops);
    BenchResult runAll(const std::string& path);

private:
    double measureTime(std::function<void()> fn);
    std::string tempFile;
    void cleanup();
};

}
}

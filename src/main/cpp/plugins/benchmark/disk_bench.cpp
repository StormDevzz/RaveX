#include "include/disk_bench.hpp"
#include "include/bench_common.hpp"
#include <cstring>
#include <cstdio>

namespace ravex {
namespace benchmark {

DiskBench::DiskBench() {
    tempFile = "ravex_bench_tmp_" + std::to_string(rand()) + ".dat";
}

DiskBench::~DiskBench() {
    cleanup();
}

void DiskBench::cleanup() {
    std::remove(tempFile.c_str());
}

double DiskBench::measureTime(std::function<void()> fn) {
    Timer timer;
    fn();
    return timer.elapsedMs();
}

double DiskBench::runSequentialWrite(const std::string& path, size_t sizeMb) {
    std::string filePath = path + "/" + tempFile;
    size_t size = sizeMb * 1024 * 1024;
    std::vector<char> buf(65536, 0);
    auto work = [&]() {
        std::ofstream ofs(filePath, std::ios::binary);
        if (!ofs) return;
        size_t written = 0;
        while (written < size) {
            size_t chunk = std::min(buf.size(), size - written);
            ofs.write(buf.data(), chunk);
            written += chunk;
        }
        ofs.close();
    };
    double ms = measureTime(work);
    std::remove(filePath.c_str());
    return ms;
}

double DiskBench::runSequentialRead(const std::string& path, size_t sizeMb) {
    std::string filePath = path + "/" + tempFile;
    size_t size = sizeMb * 1024 * 1024;
    {
        std::ofstream ofs(filePath, std::ios::binary);
        std::vector<char> buf(65536, 0);
        size_t written = 0;
        while (written < size) {
            size_t chunk = std::min(buf.size(), size - written);
            ofs.write(buf.data(), chunk);
            written += chunk;
        }
    }
    volatile char sink = 0;
    auto work = [&]() {
        std::ifstream ifs(filePath, std::ios::binary);
        if (!ifs) return;
        std::vector<char> buf(65536);
        while (ifs.read(buf.data(), buf.size())) {
            sink += buf[0];
        }
        ifs.close();
    };
    double ms = measureTime(work);
    (void)sink;
    std::remove(filePath.c_str());
    return ms;
}

double DiskBench::runRandomRead4K(const std::string& path, int ops) {
    std::string filePath = path + "/" + tempFile;
    size_t fileSize = 64 * 1024 * 1024;
    {
        std::ofstream ofs(filePath, std::ios::binary);
        std::vector<char> buf(4096, 0);
        for (size_t i = 0; i < fileSize / 4096; i++) {
            ofs.write(buf.data(), 4096);
        }
    }
    std::ifstream ifs(filePath, std::ios::binary);
    if (!ifs) return -1;
    volatile char sink = 0;
    Timer timer;
    for (int i = 0; i < ops; i++) {
        size_t offset = (static_cast<size_t>(rand()) * 4096) % fileSize;
        ifs.seekg(offset);
        char buf[4096];
        ifs.read(buf, 4096);
        sink += buf[0];
    }
    double ms = timer.elapsedMs();
    (void)sink;
    ifs.close();
    std::remove(filePath.c_str());
    return ms;
}

double DiskBench::runRandomWrite4K(const std::string& path, int ops) {
    std::string filePath = path + "/" + tempFile;
    size_t fileSize = 64 * 1024 * 1024;
    {
        std::ofstream ofs(filePath, std::ios::binary);
        std::vector<char> buf(4096, 0);
        for (size_t i = 0; i < fileSize / 4096; i++) {
            ofs.write(buf.data(), 4096);
        }
    }
    std::fstream fs(filePath, std::ios::binary | std::ios::in | std::ios::out);
    if (!fs) return -1;
    Timer timer;
    for (int i = 0; i < ops; i++) {
        size_t offset = (static_cast<size_t>(rand()) * 4096) % fileSize;
        fs.seekp(offset);
        char buf[4096];
        std::memset(buf, static_cast<char>(i & 0xFF), 4096);
        fs.write(buf, 4096);
    }
    double ms = timer.elapsedMs();
    fs.close();
    std::remove(filePath.c_str());
    return ms;
}

BenchResult DiskBench::runAll(const std::string& path) {
    BenchResult r;
    r.type = BenchType::Disk;
    r.name = "Disk";
    double ws = runSequentialWrite(path, 64);
    double rs = runSequentialRead(path, 64);
    double rr = runRandomRead4K(path, 5000);
    double rw = runRandomWrite4K(path, 5000);
    r.timeMs = ws + rs + rr + rw;
    r.score = 256.0 / (r.timeMs + 0.001) * 1000.0;
    r.unit = "points";
    return r;
}

}
}

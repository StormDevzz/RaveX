#include "include/bench_common.hpp"
#include <iomanip>

#ifdef _WIN32
#include <windows.h>
#include <intrin.h>
#else
#include <unistd.h>
#include <sys/sysinfo.h>
#include <fstream>
#include <cctype>
#include <algorithm>
#endif

namespace ravex {
namespace benchmark {

Timer::Timer() : start(std::chrono::high_resolution_clock::now()) {}
void Timer::reset() { start = std::chrono::high_resolution_clock::now(); }
double Timer::elapsedMs() const {
    auto now = std::chrono::high_resolution_clock::now();
    return std::chrono::duration<double, std::milli>(now - start).count();
}
double Timer::elapsedSec() const {
    auto now = std::chrono::high_resolution_clock::now();
    return std::chrono::duration<double>(now - start).count();
}

void Statistics::addSample(double val) { samples.push_back(val); }
double Statistics::min() const { return samples.empty() ? 0 : *std::min_element(samples.begin(), samples.end()); }
double Statistics::max() const { return samples.empty() ? 0 : *std::max_element(samples.begin(), samples.end()); }
double Statistics::mean() const {
    if (samples.empty()) return 0;
    double sum = 0;
    for (auto s : samples) sum += s;
    return sum / samples.size();
}
double Statistics::median() const {
    if (samples.empty()) return 0;
    std::vector<double> sorted = samples;
    std::sort(sorted.begin(), sorted.end());
    return sorted[sorted.size() / 2];
}
double Statistics::stddev() const {
    if (samples.size() < 2) return 0;
    double m = mean();
    double sum = 0;
    for (auto s : samples) sum += (s - m) * (s - m);
    return std::sqrt(sum / (samples.size() - 1));
}
int Statistics::count() const { return static_cast<int>(samples.size()); }

std::string formatSize(double bytes) {
    const char* units[] = {"B", "KB", "MB", "GB", "TB"};
    int unit = 0;
    while (bytes >= 1024.0 && unit < 4) { bytes /= 1024.0; unit++; }
    std::stringstream ss;
    ss << std::fixed << std::setprecision(1) << bytes << " " << units[unit];
    return ss.str();
}

std::string formatNumber(double n) {
    std::stringstream ss;
    if (n >= 1000000) ss << std::fixed << std::setprecision(2) << n / 1000000.0 << "M";
    else if (n >= 1000) ss << std::fixed << std::setprecision(2) << n / 1000.0 << "K";
    else ss << std::fixed << std::setprecision(2) << n;
    return ss.str();
}

std::string formatMs(double ms) {
    if (ms >= 60000) {
        int min = static_cast<int>(ms) / 60000;
        double sec = (ms - min * 60000) / 1000.0;
        std::stringstream ss;
        ss << min << "m " << std::fixed << std::setprecision(1) << sec << "s";
        return ss.str();
    } else if (ms >= 1000) {
        std::stringstream ss;
        ss << std::fixed << std::setprecision(2) << ms / 1000.0 << "s";
        return ss.str();
    } else {
        std::stringstream ss;
        ss << std::fixed << std::setprecision(0) << ms << "ms";
        return ss.str();
    }
}

std::string cpuVendor() {
#ifdef _WIN32
    int info[4] = {0};
    __cpuid(info, 0);
    char vendor[13] = {0};
    std::memcpy(vendor, &info[1], 4);
    std::memcpy(vendor + 4, &info[3], 4);
    std::memcpy(vendor + 8, &info[2], 4);
    return std::string(vendor);
#else
    std::ifstream cpuinfo("/proc/cpuinfo");
    std::string line;
    while (std::getline(cpuinfo, line)) {
        if (line.find("vendor_id") != std::string::npos) {
            size_t colon = line.find(':');
            if (colon != std::string::npos) return line.substr(colon + 2);
        }
    }
    return "Unknown";
#endif
}

std::string cpuModel() {
#ifdef _WIN32
    int info[4] = {0};
    char brand[49] = {0};
    __cpuid(info, 0x80000002);
    std::memcpy(brand, info, sizeof(info));
    __cpuid(info, 0x80000003);
    std::memcpy(brand + 16, info, sizeof(info));
    __cpuid(info, 0x80000004);
    std::memcpy(brand + 32, info, sizeof(info));
    std::string result(brand);
    while (!result.empty() && result.back() == ' ') result.pop_back();
    return result.empty() ? "Unknown" : result;
#else
    std::ifstream cpuinfo("/proc/cpuinfo");
    std::string line;
    while (std::getline(cpuinfo, line)) {
        if (line.find("model name") != std::string::npos) {
            size_t colon = line.find(':');
            if (colon != std::string::npos) {
                std::string name = line.substr(colon + 2);
                while (!name.empty() && name.back() == ' ') name.pop_back();
                return name;
            }
        }
    }
    return "Unknown";
#endif
}

int cpuCores() {
#ifdef _WIN32
    SYSTEM_INFO sysInfo;
    GetSystemInfo(&sysInfo);
    return sysInfo.dwNumberOfProcessors;
#else
    long n = sysconf(_SC_NPROCESSORS_CONF);
    return static_cast<int>(n);
#endif
}

int cpuThreads() {
#ifdef _WIN32
    SYSTEM_INFO sysInfo;
    GetSystemInfo(&sysInfo);
    return sysInfo.dwNumberOfProcessors;
#else
    long n = sysconf(_SC_NPROCESSORS_ONLN);
    return static_cast<int>(n);
#endif
}

size_t totalRamMb() {
#ifdef _WIN32
    MEMORYSTATUSEX mem;
    mem.dwLength = sizeof(mem);
    GlobalMemoryStatusEx(&mem);
    return static_cast<size_t>(mem.ullTotalPhys / (1024 * 1024));
#else
    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        return static_cast<size_t>((si.totalram * si.mem_unit) / (1024 * 1024));
    }
    return 0;
#endif
}

std::string osName() {
#ifdef _WIN32
    OSVERSIONINFOEX osvi;
    ZeroMemory(&osvi, sizeof(osvi));
    osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);
    #pragma warning(push)
    #pragma warning(disable: 4996)
    GetVersionEx((OSVERSIONINFO*)&osvi);
    #pragma warning(pop)
    return "Windows " + std::to_string(osvi.dwMajorVersion) + "." + std::to_string(osvi.dwMinorVersion);
#else
    std::ifstream osrelease("/etc/os-release");
    std::string line;
    while (std::getline(osrelease, line)) {
        if (line.find("PRETTY_NAME") != std::string::npos) {
            size_t eq = line.find('=');
            if (eq != std::string::npos) {
                std::string name = line.substr(eq + 1);
                if (!name.empty() && name.front() == '"') name = name.substr(1);
                if (!name.empty() && name.back() == '"') name.pop_back();
                return name;
            }
        }
    }
    return "Linux";
#endif
}

std::string osVersion() {
#ifdef _WIN32
    return std::to_string(GetVersion());
#else
    std::ifstream version("/proc/version");
    std::string line;
    std::getline(version, line);
    return line;
#endif
}

}
}

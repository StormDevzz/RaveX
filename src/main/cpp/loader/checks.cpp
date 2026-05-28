#include "checks.h"
#include <fstream>
#include <sstream>
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <dirent.h>
#include <unistd.h>
#include <sys/sysinfo.h>
#include <sys/statvfs.h>
#include <sys/utsname.h>
#include <thread>

namespace ravex {
namespace loader {

SystemReport SystemChecks::runAll() {
    SystemReport r;
    r.cpuCores = 0;
    r.cpuLoad = 0;
    r.cpuTemp = -1;
    r.totalRamKB = 0;
    r.freeRamKB = 0;
    r.availRamKB = 0;
    r.swapTotalKB = 0;
    r.swapFreeKB = 0;
    r.diskFreeKB = 0;
    r.diskTotalKB = 0;
    r.selfRSSKB = 0;
    r.processCount = 0;
    r.loadAvg1m = 0;
    r.score = 0;

    struct utsname uts;
    if (uname(&uts) == 0) {
        r.osName = uts.sysname;
        r.osVersion = uts.release;
        r.osArch = uts.machine;
    }
    r.cpuCores = (int)sysconf(_SC_NPROCESSORS_ONLN);
    r.cpuGovernor = readCPUGovernor();
    r.cpuTemp = readCPUTemp();

    uint64_t swapTotal = 0, swapFree = 0;
    readProcMemInfo(r.totalRamKB, r.freeRamKB, r.availRamKB, swapTotal, swapFree);
    r.swapTotalKB = swapTotal;
    r.swapFreeKB = swapFree;

    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        if (r.totalRamKB == 0) r.totalRamKB = si.totalram * si.mem_unit / 1024;
        if (r.freeRamKB == 0) r.freeRamKB = si.freeram * si.mem_unit / 1024;
        r.loadAvg1m = si.loads[0] / 65536.0;
    } else {
        r.loadAvg1m = readLoadAvg();
    }

    struct statvfs vfs;
    if (statvfs(".", &vfs) == 0) {
        r.diskTotalKB = (uint64_t)vfs.f_blocks * vfs.f_frsize / 1024;
        r.diskFreeKB = (uint64_t)vfs.f_bfree * vfs.f_frsize / 1024;
    }

    // CPU load (simple delta)
    {
        std::ifstream stat("/proc/stat");
        std::string line;
        if (std::getline(stat, line)) {
            long u, n, s, id;
            if (sscanf(line.c_str(), "cpu %ld %ld %ld %ld", &u, &n, &s, &id) == 4) {
                long t1 = u + n + s + id;
                std::this_thread::sleep_for(std::chrono::milliseconds(200));
                stat.seekg(0);
                if (std::getline(stat, line) && sscanf(line.c_str(), "cpu %ld %ld %ld %ld", &u, &n, &s, &id) == 4) {
                    long t2 = u + n + s + id;
                    long idle = id;
                    if (t2 > t1) r.cpuLoad = 1.0 - (double)(idle - (t1 - (u + n + s + id))) / (t2 - t1);
                }
            }
        }
    }

    r.selfRSSKB = readSelfRSS();
    r.processCount = countProcesses();
    readTopProcesses(r.topProcesses);
    calcScore(r);
    return r;
}

void SystemChecks::readProcMemInfo(uint64_t& total, uint64_t& free, uint64_t& avail,
                                    uint64_t& swapTotal, uint64_t& swapFree) {
    std::ifstream f("/proc/meminfo");
    if (!f.is_open()) return;
    std::string line;
    while (std::getline(f, line)) {
        if (line.find("MemTotal:") == 0) sscanf(line.c_str(), "MemTotal: %lu kB", &total);
        else if (line.find("MemFree:") == 0) sscanf(line.c_str(), "MemFree: %lu kB", &free);
        else if (line.find("MemAvailable:") == 0) sscanf(line.c_str(), "MemAvailable: %lu kB", &avail);
        else if (line.find("SwapTotal:") == 0) sscanf(line.c_str(), "SwapTotal: %lu kB", &swapTotal);
        else if (line.find("SwapFree:") == 0) sscanf(line.c_str(), "SwapFree: %lu kB", &swapFree);
    }
}

double SystemChecks::readCPUTemp() {
    std::ifstream f("/sys/class/thermal/thermal_zone0/temp");
    if (!f.is_open()) return -1;
    int millideg;
    f >> millideg;
    return millideg / 1000.0;
}

std::string SystemChecks::readCPUGovernor() {
    std::ifstream f("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
    if (!f.is_open()) return "unknown";
    std::string gov;
    f >> gov;
    return gov;
}

double SystemChecks::readLoadAvg() {
    std::ifstream f("/proc/loadavg");
    if (!f.is_open()) return 0;
    double l1, l5, l15;
    f >> l1 >> l5 >> l15;
    return l1;
}

void SystemChecks::readTopProcesses(std::vector<ProcessInfo>& out) {
    DIR* dir = opendir("/proc");
    if (!dir) return;

    struct dirent* entry;
    while ((entry = readdir(dir))) {
        if (entry->d_type != DT_DIR) continue;
        bool isNum = true;
        for (char* p = entry->d_name; *p; p++) {
            if (*p < '0' || *p > '9') { isNum = false; break; }
        }
        if (!isNum) continue;

        int pid = atoi(entry->d_name);
        std::string statPath = "/proc/" + std::to_string(pid) + "/status";
        std::ifstream sf(statPath);
        if (!sf.is_open()) continue;

        ProcessInfo pi;
        pi.pid = pid;
        pi.memMB = 0;
        pi.cpuPct = 0;

        std::string line;
        while (std::getline(sf, line)) {
            if (line.find("Name:") == 0) {
                pi.name = line.substr(5);
                pi.name.erase(0, pi.name.find_first_not_of(" \t"));
            } else if (line.find("VmRSS:") == 0) {
                unsigned long kb;
                if (sscanf(line.c_str(), "VmRSS: %lu kB", &kb) == 1) {
                    pi.memMB = kb / 1024.0;
                }
            }
        }
        if (!pi.name.empty() && pi.memMB > 10) {
            out.push_back(pi);
        }
    }
    closedir(dir);

    std::sort(out.begin(), out.end(), [](const ProcessInfo& a, const ProcessInfo& b) {
        return a.memMB > b.memMB;
    });
    if (out.size() > 15) out.resize(15);
}

int SystemChecks::countProcesses() {
    DIR* dir = opendir("/proc");
    if (!dir) return 0;
    int count = 0;
    struct dirent* entry;
    while ((entry = readdir(dir))) {
        if (entry->d_type != DT_DIR) continue;
        bool isNum = true;
        for (char* p = entry->d_name; *p; p++) {
            if (*p < '0' || *p > '9') { isNum = false; break; }
        }
        if (isNum) count++;
    }
    closedir(dir);
    return count;
}

uint64_t SystemChecks::readSelfRSS() {
    std::ifstream f("/proc/self/statm");
    if (!f.is_open()) return 0;
    uint64_t rssPages = 0;
    f >> rssPages;
    f >> rssPages;
    return rssPages * sysconf(_SC_PAGESIZE) / 1024;
}

void SystemChecks::calcScore(SystemReport& r) {
    int sc = 100;

    if (r.cpuLoad > 0.8) sc -= 20;
    else if (r.cpuLoad > 0.6) sc -= 10;

    if (r.totalRamKB > 0) {
        int freePct = (int)(r.availRamKB * 100 / r.totalRamKB);
        if (freePct < 10) { sc -= 20; r.warnings.push_back("Critical: <10% RAM free (" + std::to_string(freePct) + "%)"); }
        else if (freePct < 20) { sc -= 10; r.warnings.push_back("Low memory: " + std::to_string(freePct) + "% free"); }
        else if (freePct < 30) sc -= 5;
    }

    if (r.swapTotalKB > 0) {
        int usedPct = (int)((r.swapTotalKB - r.swapFreeKB) * 100 / r.swapTotalKB);
        if (usedPct > 80) { sc -= 10; r.warnings.push_back("High swap usage: " + std::to_string(usedPct) + "%"); }
    }

    if (r.diskTotalKB > 0) {
        int freePct = (int)(r.diskFreeKB * 100 / r.diskTotalKB);
        if (freePct < 5) { sc -= 15; r.warnings.push_back("Critical: disk " + std::to_string(freePct) + "% free"); }
        else if (freePct < 10) sc -= 5;
    }

    if (r.processCount > 500) sc -= 10;
    else if (r.processCount > 300) sc -= 5;

    if (r.loadAvg1m > r.cpuCores) sc -= 10;

    if (r.cpuTemp > 85) { sc -= 10; r.warnings.push_back("CPU overheating: " + std::to_string((int)r.cpuTemp) + "°C"); }
    else if (r.cpuTemp > 75) sc -= 5;

    if (!r.topProcesses.empty() && r.topProcesses[0].memMB > 2048) {
        sc -= 5;
    }

    r.score = std::max(sc, 0);
}

void SystemChecks::printReport(const SystemReport& r) {
    auto fmtKB = [](uint64_t kb) -> std::string {
        if (kb >= 1048576) return std::to_string(kb / 1048576) + " GB";
        if (kb >= 1024) return std::to_string(kb / 1024) + " MB";
        return std::to_string(kb) + " KB";
    };

    printf("\n===== RaveX System Report =====\n");
    printf("OS:       %s %s [%s]\n", r.osName.c_str(), r.osVersion.c_str(), r.osArch.c_str());
    printf("CPU:      %d cores | %.0f%% load | %s governor | %.1f°C\n",
           r.cpuCores, r.cpuLoad * 100, r.cpuGovernor.c_str(), r.cpuTemp);
    printf("RAM:      %s total | %s free | %s avail\n",
           fmtKB(r.totalRamKB).c_str(), fmtKB(r.freeRamKB).c_str(), fmtKB(r.availRamKB).c_str());
    printf("Swap:     %s total | %s free\n",
           fmtKB(r.swapTotalKB).c_str(), fmtKB(r.swapFreeKB).c_str());
    printf("Load avg: %.2f (1m) | Processes: %d\n", r.loadAvg1m, r.processCount);
    printf("Disk:     %s total | %s free\n",
           fmtKB(r.diskTotalKB).c_str(), fmtKB(r.diskFreeKB).c_str());
    printf("Self RSS: %s\n", fmtKB(r.selfRSSKB).c_str());
    printf("Score:    %d/100\n", r.score);

    if (!r.topProcesses.empty()) {
        printf("\nTop processes (by RSS):\n");
        for (auto& p : r.topProcesses) {
            printf("  %-6d %-24s %.0f MB\n", p.pid, p.name.c_str(), p.memMB);
        }
    }

    if (!r.warnings.empty()) {
        printf("\nWarnings:\n");
        for (auto& w : r.warnings) printf("  ! %s\n", w.c_str());
    }
    printf("===============================\n\n");
}

} // namespace loader
} // namespace ravex

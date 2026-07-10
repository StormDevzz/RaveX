#include <iostream>
#include <string>
#include <vector>
#include <fstream>
#include <unistd.h>
#include <sys/resource.h>
#include <sched.h>
#include <malloc.h>

void printSysInfo() {
    std::ifstream meminfo("/proc/meminfo");
    std::string line;
    long totalMem = 0;
    long freeMem = 0;
    while (std::getline(meminfo, line)) {
        if (line.rfind("MemTotal:", 0) == 0) {
            sscanf(line.c_str(), "MemTotal: %ld", &totalMem);
        } else if (line.rfind("MemFree:", 0) == 0) {
            sscanf(line.c_str(), "MemFree: %ld", &freeMem);
        }
    }
    std::cout << "{\"status\":\"ok\",\"total_kb\":" << totalMem << ",\"free_kb\":" << freeMem << "}" << std::endl;
}

void optimizeProcess() {

    struct sched_param param;
    param.sched_priority = 0;
    sched_setscheduler(0, SCHED_OTHER, &param);
    setpriority(PRIO_PROCESS, 0, -10);


    malloc_trim(0);

    std::cout << "{\"status\":\"optimized\",\"native_memory_trimmed\":true,\"process_priority\":-10}" << std::endl;
}

int main() {
    std::string cmd;
    while (std::getline(std::cin, cmd)) {
        if (cmd == "sysinfo") {
            printSysInfo();
        } else if (cmd == "optimize") {
            optimizeProcess();
        } else if (cmd == "exit") {
            break;
        } else {
            std::cout << "{\"status\":\"error\",\"message\":\"unknown_command\"}" << std::endl;
        }
    }
    return 0;
}

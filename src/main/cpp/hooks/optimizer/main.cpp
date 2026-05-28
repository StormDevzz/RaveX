#include <iostream>
#include <string>
#include <nlohmann/json.hpp>
#include "optimizer.h"

using json = nlohmann::json;

int main() {
    std::string cmd;
    while (std::getline(std::cin, cmd)) {
        if (cmd == "sysinfo") {
            auto info = ravex::Memory::readMemInfo();
            json resp = {
                {"status", "ok"},
                {"total_kb", info.total_kb},
                {"free_kb", info.free_kb},
                {"avail_kb", info.avail_kb},
                {"cached_kb", info.cached_kb}
            };
            std::cout << resp.dump() << std::endl;

        } else if (cmd.rfind("optimize ", 0) == 0) {
            std::string mode = cmd.substr(9);
            auto result = ravex::Optimizer::run(mode);
            json resp = {
                {"status", result.success ? "optimized" : "error"},
                {"message", result.message},
                {"free_kb", result.freeMemoryKb}
            };
            std::cout << resp.dump() << std::endl;

        } else if (cmd == "exit") {
            break;

        } else {
            json resp = {{"status", "error"}, {"message", "unknown_command"}};
            std::cout << resp.dump() << std::endl;
        }
    }
    return 0;
}

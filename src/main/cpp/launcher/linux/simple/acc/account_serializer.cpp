#include "include/account_serializer.hpp"
#include <fstream>
#include <iostream>

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {

void deserialize_accounts(LauncherState *state) {
    std::string path = state->kickx_dir + "/accounts.json";
    std::ifstream file(path);
    if (!file.is_open()) return;

    state->accounts.clear();
    state->active_account_index = -1;

    std::string line;
    Account current_acc;
    bool in_accounts = false;
    
    auto trim = [](const std::string& str) {
        size_t first = str.find_first_not_of(" \t\r\n\",");
        if (std::string::npos == first) return std::string("");
        size_t last = str.find_last_not_of(" \t\r\n\",");
        return str.substr(first, (last - first + 1));
    };

    while (std::getline(file, line)) {
        if (line.find("\"active_index\"") != std::string::npos) {
            size_t colon = line.find(":");
            if (colon != std::string::npos) {
                std::string val = line.substr(colon + 1);
                size_t comma = val.find(",");
                if (comma != std::string::npos) val = val.substr(0, comma);
                state->active_account_index = std::stoi(trim(val));
            }
        }
        if (line.find("\"accounts\"") != std::string::npos) {
            in_accounts = true;
            continue;
        }
        if (in_accounts) {
            if (line.find("{") != std::string::npos) {
                current_acc = Account();
            }
            if (line.find("\"username\"") != std::string::npos) {
                size_t colon = line.find(":");
                if (colon != std::string::npos) {
                    current_acc.username = trim(line.substr(colon + 1));
                }
            }
            if (line.find("\"uuid\"") != std::string::npos) {
                size_t colon = line.find(":");
                if (colon != std::string::npos) {
                    current_acc.uuid = trim(line.substr(colon + 1));
                }
            }
            if (line.find("\"token\"") != std::string::npos) {
                size_t colon = line.find(":");
                if (colon != std::string::npos) {
                    current_acc.token = trim(line.substr(colon + 1));
                }
            }
            if (line.find("\"is_microsoft\"") != std::string::npos) {
                size_t colon = line.find(":");
                if (colon != std::string::npos) {
                    std::string val = trim(line.substr(colon + 1));
                    current_acc.is_microsoft = (val.find("true") != std::string::npos);
                }
            }
            if (line.find("}") != std::string::npos) {
                if (!current_acc.username.empty()) {
                    state->accounts.push_back(current_acc);
                }
            }
        }
    }
    file.close();
}

void serialize_accounts(LauncherState *state) {
    std::string path = state->kickx_dir + "/accounts.json";
    std::ofstream file(path);
    if (!file.is_open()) return;
    
    file << "{\n  \"active_index\": " << state->active_account_index << ",\n  \"accounts\": [\n";
    for (size_t i = 0; i < state->accounts.size(); ++i) {
        const auto& acc = state->accounts[i];
        file << "    {\n";
        file << "      \"username\": \"" << acc.username << "\",\n";
        file << "      \"uuid\": \"" << acc.uuid << "\",\n";
        file << "      \"token\": \"" << acc.token << "\",\n";
        file << "      \"is_microsoft\": " << (acc.is_microsoft ? "true" : "false") << "\n";
        file << "    }" << (i + 1 < state->accounts.size() ? "," : "") << "\n";
    }
    file << "  ]\n}\n";
    file.close();
}

} 
} 
} 
} 

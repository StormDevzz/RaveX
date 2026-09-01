#pragma once
#include <string>

namespace ravex::game {

struct ServerInfo {
    std::string motd;
    int onlinePlayers = 0;
    int maxPlayers = 0;
    std::string version;
    bool online = false;
    long long latencyMs = 0;
};

bool pingServer(const std::string& address, ServerInfo* out, std::string* error);

}

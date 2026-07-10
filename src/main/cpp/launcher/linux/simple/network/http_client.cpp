#include "include/http_client.hpp"
#include <cstdio>
#include <cstdlib>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

std::string http_get(const std::string& url) {
    std::string cmd = "curl -sL -H \"User-Agent: RaveX-Launcher/1.0\" \"" + url + "\"";
    char buffer[128];
    std::string result = "";
    FILE* pipe = popen(cmd.c_str(), "r");
    if (!pipe) return "";
    while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
        result += buffer;
    }
    pclose(pipe);
    return result;
}

bool http_download(const std::string& url, const std::string& dest_path) {

    size_t last_slash = dest_path.find_last_of('/');
    if (last_slash != std::string::npos) {
        std::string dir = dest_path.substr(0, last_slash);
        std::string cmd_mkdir = "mkdir -p \"" + dir + "\"";
        system(cmd_mkdir.c_str());
    }
    std::string cmd = "curl -sL -H \"User-Agent: RaveX-Launcher/1.0\" -o \"" + dest_path + "\" \"" + url + "\" 2>/dev/null";
    return (system(cmd.c_str()) == 0);
}

}
}
}
}

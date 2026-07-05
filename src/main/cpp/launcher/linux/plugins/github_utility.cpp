#include "github_utility.hpp"
#include <iostream>
#include <cstdio>
#include <cstdlib>

namespace ravex {
namespace launcher {
namespace plugins {

static std::string fetchUrl(const std::string& url) {
    std::string cmd = "curl -sL -H \"User-Agent: RaveX-Launcher\" \"" + url + "\"";
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

static std::string extractJsonValue(const std::string& json, const std::string& key) {
    size_t pos = json.find("\"" + key + "\":");
    if (pos == std::string::npos) return "";
    size_t startQuote = json.find("\"", pos + key.length() + 2);
    if (startQuote == std::string::npos) return "";
    size_t endQuote = json.find("\"", startQuote + 1);
    if (endQuote == std::string::npos) return "";
    return json.substr(startQuote + 1, endQuote - startQuote - 1);
}

ReleaseInfo GithubUtility::getLatestRelease() {
    ReleaseInfo info;
    info.success = false;
    std::string json = fetchUrl("https://api.github.com/repos/StormDevzz/RaveX/releases/latest");
    if (json.empty()) return info;

    info.tagName = extractJsonValue(json, "tag_name");

    {
        std::string body = extractJsonValue(json, "body");
        
        {
            size_t pos = 0;
            while (true) {
                pos = body.find("MC_VERSION=", pos);
                if (pos == std::string::npos) break;
                size_t start = pos + 11;
                if (start >= body.size()) break;
                size_t end = body.find_first_of("\n\r ", start);
                if (end == std::string::npos) end = body.size();
                info.minecraftVersion = body.substr(start, end - start);
                break;
            }
        }

    }

    size_t assets_pos = json.find("\"assets\":");
    if (assets_pos != std::string::npos) {
        size_t url_pos = json.find("\"browser_download_url\":", assets_pos);
        if (url_pos != std::string::npos) {
            size_t start = json.find("\"", url_pos + 22);
            size_t end = json.find("\"", start + 1);
            if (start != std::string::npos && end != std::string::npos) {
                info.downloadUrl = json.substr(start + 1, end - start - 1);
            }
        }
    }

    if (!info.tagName.empty() && !info.downloadUrl.empty()) {
        info.success = true;
    }
    return info;
}

bool GithubUtility::downloadFile(const std::string& url, const std::string& destPath) {
    std::cout << "[RaveX Launcher] downloading: " << url << " -> " << destPath << std::endl;
    std::string cmd = "curl -sL -o \"" + destPath + "\" \"" + url + "\" 2>/dev/null";
    int code = system(cmd.c_str());
    return (code == 0);
}

} 
} 
} 

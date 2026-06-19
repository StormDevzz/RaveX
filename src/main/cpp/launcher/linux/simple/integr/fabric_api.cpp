#include "include/fabric_api.h"
#include "../../network/include/http_client.h"
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

// Helper: find a JSON string value for a given key, handling optional whitespace
// e.g., both "key":"value" and "key": "value" work.
static std::string findJsonStringValue(const std::string& json, const std::string& key) {
    // Try with a space after colon first, then without
    std::string searchKey = "\"" + key + "\":";
    size_t pos = json.find(searchKey);
    if (pos == std::string::npos) return "";

    pos += searchKey.length();
    // skip whitespace before the value
    while (pos < json.length() && (json[pos] == ' ' || json[pos] == '\t' || json[pos] == '\n' || json[pos] == '\r'))
        pos++;
    // expect opening quote
    if (pos >= json.length() || json[pos] != '"') return "";
    pos++;
    // find closing quote
    size_t end = json.find('"', pos);
    if (end == std::string::npos) return "";
    return json.substr(pos, end - pos);
}

// Known working loader versions for specific MC versions (fallback)
static std::string fallbackLoaderVersion(const std::string& mcVersion) {
    if (mcVersion == "1.21.11") return "0.19.3";
    if (mcVersion == "1.21.4")  return "0.16.10";
    if (mcVersion == "1.21.3")  return "0.16.9";
    if (mcVersion == "1.21.1")  return "0.16.2";
    if (mcVersion == "1.21")    return "0.16.2";
    if (mcVersion == "1.20.4")  return "0.15.11";
    if (mcVersion == "1.20.1")  return "0.15.11";
    if (mcVersion == "1.19.4")  return "0.14.24";
    if (mcVersion == "1.19.2")  return "0.14.24";
    if (mcVersion == "1.18.2")  return "0.14.24";
    if (mcVersion == "1.17.1")  return "0.14.24";
    if (mcVersion == "1.16.5")  return "0.14.24";
    if (mcVersion == "1.12.2")  return "0.15.11";
    return "";
}

std::string getLatestFabricLoader(const std::string& mcVersion) {
    std::string url = "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion;
    std::string json = network::http_get(url);
    if (!json.empty() && json != "[]" && json != "[\n]") {
        std::string ver = findJsonStringValue(json, "version");
        if (!ver.empty()) return ver;
    }

    // API failed or parsing failed — try fallback
    return fallbackLoaderVersion(mcVersion);
}

bool fetchFabricProfileJson(const std::string& mcVersion, const std::string& loaderVersion, std::string& outJson) {
    std::string url = "https://meta.fabricmc.net/v2/versions/loader/"
                      + mcVersion + "/" + loaderVersion + "/profile/json";
    outJson = network::http_get(url);
    return !outJson.empty();
}

FabricProfile getFabricProfile(const std::string& mcVersion, const std::string& loaderVersion) {
    FabricProfile profile;
    profile.loaderVersion = loaderVersion;

    std::string json;
    if (!fetchFabricProfileJson(mcVersion, loaderVersion, json))
        return profile;

    profile.mainClass = findJsonStringValue(json, "mainClass");

    // find "libraries" array start
    std::string libKey = "\"libraries\":";
    size_t libStart = json.find(libKey);
    if (libStart == std::string::npos) return profile;
    libStart += libKey.length();
    // skip whitespace
    while (libStart < json.length() && (json[libStart] == ' ' || json[libStart] == '\t' || json[libStart] == '\n' || json[libStart] == '\r'))
        libStart++;
    if (libStart >= json.length() || json[libStart] != '[') return profile;

    size_t cursor = libStart;
    while (true) {
        // find "name" within this library object
        size_t namePos = json.find("\"name\":", cursor);
        if (namePos == std::string::npos || namePos > json.find(']', cursor))
            break;

        std::string mavenCoord = findJsonStringValue(json.substr(namePos), "name");
        if (mavenCoord.empty()) { cursor = namePos + 7; continue; }
        cursor = namePos + 7 + mavenCoord.length();

        // find "url" for this library
        std::string remnant = json.substr(cursor);
        std::string urlVal = findJsonStringValue(remnant, "url");
        if (urlVal.empty()) continue;

        // make sure url is within the same library object (before next } or ])
        size_t objEnd = json.find('}', cursor);
        size_t urlInObj = json.find("\"url\":", cursor);
        if (urlInObj == std::string::npos || urlInObj > objEnd)
            continue;

        FabricLibrary lib;
        lib.name = mavenCoord;
        lib.url = urlVal;
        profile.libraries.push_back(lib);
    }

    return profile;
}

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex

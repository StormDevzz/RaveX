#pragma once
#include <string>
#include "net/include/http.hpp"

namespace ravex::game {

bool isFabricInstalled(const std::string& mcVersion);
bool isForgeInstalled(const std::string& mcVersion);
bool isQuiltInstalled(const std::string& mcVersion);
bool ensureFabric(const std::string& mcVersion, const std::string& loaderVersion, std::string* error, const std::function<void(const net::Progress&)>& progress, const bool* cancelled);
bool ensureForge(const std::string& mcVersion, const std::string& loaderVersion, std::string* error, const std::function<void(const net::Progress&)>& progress, const bool* cancelled);
bool ensureQuilt(const std::string& mcVersion, const std::string& loaderVersion, std::string* error, const std::function<void(const net::Progress&)>& progress, const bool* cancelled);
void removeLoaderVersion(const std::string& mcVersion, const std::string& loader);
std::vector<std::string> fetchFabricLoaderVersions(const std::string& mcVersion);
std::vector<std::string> fetchForgeLoaderVersions(const std::string& mcVersion);
std::vector<std::string> fetchQuiltLoaderVersions(const std::string& mcVersion);

}

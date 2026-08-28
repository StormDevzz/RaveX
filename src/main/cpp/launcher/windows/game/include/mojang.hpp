#pragma once
#include <string>
#include "net/include/http.hpp"

namespace ravex::game {

bool ensureMinecraft(const std::string& version, std::string* error, const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* outIndexId = nullptr);
bool isMinecraftInstalled(const std::string& version);
std::string getFallbackAssetIndexId(const std::string& version);
bool quickIntegrityCheck(const std::string& version, const std::string& assetIndexId, std::string* error);

}

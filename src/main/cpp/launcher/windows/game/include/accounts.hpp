#pragma once

#include <functional>
#include <string>

#include "core/include/config.hpp"

namespace ravex::game {

bool createOfflineAccount(const std::string& name, ravex::Account* out);
bool loginMicrosoft(ravex::Account* out, std::string* error, const std::function<void(const std::string&)>& status);

}
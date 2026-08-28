#pragma once
#include <functional>
#include <string>
#include "net/include/http.hpp"

namespace ravex::game {

bool ensureJava(int version, std::wstring& outPath, std::string* error,
                const std::function<void(const std::string&)>& status,
                const bool* cancelled);

}

#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

// парсинг ссылки из json-манифеста
std::string parse_json_value(const std::string& json, const std::string& key);

} // namespace network
} // namespace simple
} // namespace launcher
} // namespace ravex

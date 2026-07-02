#include "include/mojang_parser.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

std::string parse_json_value(const std::string& json, const std::string& key) {
    size_t pos = json.find("\"" + key + "\":");
    if (pos == std::string::npos) return "";
    size_t startQuote = json.find("\"", pos + key.length() + 2);
    if (startQuote == std::string::npos) return "";
    size_t endQuote = json.find("\"", startQuote + 1);
    if (endQuote == std::string::npos) return "";
    return json.substr(startQuote + 1, endQuote - startQuote - 1);
}

} 
} 
} 
} 

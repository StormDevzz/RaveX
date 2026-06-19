#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_parser_result {
    bool success;
    std::string message;
    std::string data;
};

ver_parser_result ver_parser_execute(const std::string& input);
double ver_parser_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex

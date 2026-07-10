#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct hash_check_result {
    bool success;
    std::string message;
    std::string data;
};

hash_check_result hash_check_execute(const std::string& input);
double hash_check_calculate(const std::string& input);

}
}
}
}

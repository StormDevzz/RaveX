#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_db_result {
    bool success;
    std::string message;
    std::string data;
};

ver_db_result ver_db_execute(const std::string& input);
double ver_db_calculate(const std::string& input);

}
}
}
}

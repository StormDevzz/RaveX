#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

struct NetworkResponse {
    std::string body;
    int status_code = 0;
    bool success = false;
};

}
}
}
}

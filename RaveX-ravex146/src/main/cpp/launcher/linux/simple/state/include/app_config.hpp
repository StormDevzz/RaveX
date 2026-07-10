#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace state {


struct AppConfig {
    std::string java_path = "java";
    bool check_updates_on_start = true;
};

}
}
}
}

#pragma once
#include <string>

namespace ravex {
namespace addon {

struct AddonMeta {
    std::string name;
    std::string author;
    std::string version;
    int apiLevel;
};

}
}

#pragma once
#include <string>
#include <unordered_map>

namespace ravex {
namespace addon {

class AddonConfig {
private:
    std::unordered_map<std::string, std::string> settings;
public:
    void set(const std::string& key, const std::string& val);
    std::string get(const std::string& key, const std::string& def);
};

}
}

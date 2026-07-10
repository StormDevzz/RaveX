#pragma once

#include "../../include/packet_types.hpp"
#include <vector>
#include <string>
#include <functional>

namespace packet {
namespace filter {

enum class RuleAction : uint8_t {
    Allow, Drop, Log, Highlight
};

struct Rule {
    std::string name;
    RuleAction action = RuleAction::Allow;
    int32_t packetId = -1;
    bool matchOutgoing = true;
    bool matchIncoming = true;
    size_t minSize = 0;
    size_t maxSize = 0;
};

class Filter {
public:
    void addRule(const Rule& rule);
    void clear();
    bool matches(const Packet& pkt, RuleAction& action) const;
    void loadFromFile(const std::string& path);
    void saveToFile(const std::string& path) const;

private:
    std::vector<Rule> rules_;
};

}
}

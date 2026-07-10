#include "filter.hpp"

namespace packet {
namespace filter {

void Filter::addRule(const Rule& rule) {
    rules_.push_back(rule);
}

void Filter::clear() {
    rules_.clear();
}

bool Filter::matches(const Packet& pkt, RuleAction& action) const {
    for (auto& rule : rules_) {
        if (rule.packetId >= 0 && rule.packetId != pkt.id) continue;
        if (pkt.outgoing && !rule.matchOutgoing) continue;
        if (!pkt.outgoing && !rule.matchIncoming) continue;
        if (rule.minSize > 0 && pkt.data.size() < rule.minSize) continue;
        if (rule.maxSize > 0 && pkt.data.size() > rule.maxSize) continue;
        action = rule.action;
        return true;
    }
    return false;
}

void Filter::loadFromFile(const std::string& path) {
    (void)path;
}

void Filter::saveToFile(const std::string& path) const {
    (void)path;
}

}
}

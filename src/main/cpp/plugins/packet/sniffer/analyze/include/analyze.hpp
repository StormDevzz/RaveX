#pragma once

#include "../../include/packet_types.hpp"
#include "../../include/stats.hpp"

namespace packet {
namespace analyze {

struct AnalysisResult {
    int64_t totalPackets = 0;
    int64_t totalBytes = 0;
    double avgSize = 0;
    int64_t uniquePacketTypes = 0;
    std::array<int64_t, 256> typeDistribution = {};
};

AnalysisResult analyzePackets(const std::vector<Packet>& packets);
AnalysisResult analyzeStats(const stats::SnifferStats& stats);
void printAnalysis(const AnalysisResult& result);

} 
} 

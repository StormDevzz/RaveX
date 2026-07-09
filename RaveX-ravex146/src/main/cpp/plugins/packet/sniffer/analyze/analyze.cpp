#include "analyze.hpp"
#include <iostream>
#include <iomanip>
#include <map>
#include "../../include/packet_utils.hpp"

namespace packet {
namespace analyze {

AnalysisResult analyzePackets(const std::vector<Packet>& packets) {
    AnalysisResult r;
    r.totalPackets = static_cast<int64_t>(packets.size());

    for (auto& pkt : packets) {
        r.totalBytes += static_cast<int64_t>(pkt.data.size());
        int idx = pkt.id & 0xFF;
        if (idx >= 0 && idx < 256) {
            r.typeDistribution[idx]++;
            if (r.typeDistribution[idx] == 1) r.uniquePacketTypes++;
        }
    }

    if (r.totalPackets > 0)
        r.avgSize = static_cast<double>(r.totalBytes) / r.totalPackets;

    return r;
}

AnalysisResult analyzeStats(const stats::SnifferStats& stats) {
    AnalysisResult r;
    r.totalPackets = stats.packetsCaptured;
    r.totalBytes = stats.bytesCaptured;
    for (int i = 0; i < 256; ++i) {
        r.typeDistribution[i] = stats.packetTypeCounts[i];
        if (stats.packetTypeCounts[i] > 0) r.uniquePacketTypes++;
    }
    if (r.totalPackets > 0)
        r.avgSize = static_cast<double>(r.totalBytes) / r.totalPackets;
    return r;
}

void printAnalysis(const AnalysisResult& result) {
    std::cout << "\n=== Packet Analysis ===\n"
              << "Total packets: " << result.totalPackets << "\n"
              << "Total bytes:   " << result.totalBytes << "\n"
              << "Avg size:      " << std::fixed << std::setprecision(1) << result.avgSize << " B\n"
              << "Unique types:  " << result.uniquePacketTypes << "\n"
              << "Top packets:\n";

    std::multimap<int64_t, int> sorted;
    for (int i = 0; i < 256; ++i) {
        if (result.typeDistribution[i] > 0)
            sorted.insert({result.typeDistribution[i], i});
    }

    int count = 0;
    for (auto it = sorted.rbegin(); it != sorted.rend() && count < 10; ++it, ++count) {
        std::cout << "  0x" << std::hex << it->second << std::dec
                  << " (" << utils::packetName(it->second) << "): "
                  << it->first << "\n";
    }
}

} 
} 

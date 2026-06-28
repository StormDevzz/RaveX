#pragma once

#include "packet_types.h"
#include <cstdint>

namespace packet {
namespace stats {

struct SnifferStats {
    int64_t packetsCaptured = 0;
    int64_t bytesCaptured = 0;
    int64_t packetsDropped = 0;
    int64_t packetsFiltered = 0;
    int64_t packetsParsed = 0;
    int64_t errors = 0;

    double packetsPerSec = 0;
    double bytesPerSec = 0;
    int64_t startTime = 0;
    int64_t lastTime = 0;
    int64_t lastPacketCount = 0;

    std::array<int64_t, 256> packetTypeCounts = {};
};

inline void reset(SnifferStats& s) {
    s = SnifferStats{};
}

inline void record(SnifferStats& s, const Packet& pkt) {
    s.packetsCaptured++;
    s.bytesCaptured += static_cast<int64_t>(pkt.data.size());
    int idx = pkt.id & 0xFF;
    if (idx >= 0 && idx < 256) s.packetTypeCounts[idx]++;
}

inline void updateRate(SnifferStats& s, int64_t now) {
    if (s.lastTime == 0) { s.lastTime = now; s.startTime = now; return; }
    double dt = static_cast<double>(now - s.lastTime) / 1000.0;
    if (dt > 0.1) {
        s.packetsPerSec = (s.packetsCaptured - s.lastPacketCount) / dt;
        s.lastTime = now;
        s.lastPacketCount = s.packetsCaptured;
    }
}

} // namespace stats
} // namespace packet

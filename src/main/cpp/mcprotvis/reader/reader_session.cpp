#include "../include/pcap_reader.hpp"
#include <cstdio>
#include <map>

struct SessionKey {
    uint16_t cPort, sPort;
    bool operator<(const SessionKey& o) const {
        if (cPort != o.cPort) return cPort < o.cPort;
        return sPort < o.sPort;
    }
};

struct SessionStats {
    int packets = 0;
    int bytes = 0;
    double firstSeen = 0;
    double lastSeen = 0;
};

static std::map<SessionKey, SessionStats> gSessions;

void trackSession(const PacketRecord& rec) {
    SessionKey k;
    if (rec.fromServer) {
        k.sPort = rec.srcPort;
        k.cPort = rec.dstPort;
    } else {
        k.cPort = rec.srcPort;
        k.sPort = rec.dstPort;
    }

    auto& s = gSessions[k];
    s.packets++;
    s.bytes += int(rec.data.size());
    if (s.firstSeen == 0) s.firstSeen = rec.timestamp;
    s.lastSeen = rec.timestamp;
}

void printSessionStats() {
    if (gSessions.empty()) return;
    printf("\n  Sessions:\n");
    for (auto& [k, s] : gSessions) {
        double dur = s.lastSeen - s.firstSeen;
        printf("    C:%u <-> S:%u  |  %d packets, %d bytes, %.1fs duration\n",
               k.cPort, k.sPort, s.packets, s.bytes, dur);
    }
}

void resetSessions() {
    gSessions.clear();
}

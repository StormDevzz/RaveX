#include "../include/packet_analysis.hpp"
#include <cstring>

static int gPacketCounts[256] = {0};
static int gTotalBytes = 0;
static int gPacketBytes[256] = {0};

void trackPacketStats(const PacketInfo& pkt) {
    if (pkt.id >= 0 && pkt.id < 256) {
        gPacketCounts[pkt.id]++;
        gPacketBytes[pkt.id] += pkt.length;
    }
    gTotalBytes += pkt.length;
}

void printPacketStats() {
    printf("\n  Packet Type Distribution:\n");
    int totalPkts = 0;
    for (int i = 0; i < 256; i++) totalPkts += gPacketCounts[i];

    for (int i = 0; i < 256; i++) {
        if (gPacketCounts[i] > 0) {
            double pct = 100.0 * gPacketCounts[i] / totalPkts;
            printf("    0x%02x  %-30s  %5d (%5.1f%%)  %s\n",
                   i, lookupPacketName(i),
                   gPacketCounts[i], pct,
                   lookupPacketState(i));
        }
    }
    printf("  Total: %d packets, %d bytes\n", totalPkts, gTotalBytes);
}

void resetPacketStats() {
    memset(gPacketCounts, 0, sizeof(gPacketCounts));
    memset(gPacketBytes, 0, sizeof(gPacketBytes));
    gTotalBytes = 0;
}

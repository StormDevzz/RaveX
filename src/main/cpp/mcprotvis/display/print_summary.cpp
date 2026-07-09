#include "../include/display.hpp"
#include <cstdio>

void printSummary(int totalPkts, int mcPkts, int serverPkts, int clientPkts) {
    printf("  ─────────────────────────────────────────────────────────────────\n");
    printf("  Summary:\n");
    printf("    Total packets (MC port):  %d\n", totalPkts);
    printf("    Minecraft packets:        %d\n", mcPkts);
    printf("    Server → Client:          %d (%.1f%%)\n",
           serverPkts, mcPkts > 0 ? 100.f * serverPkts / mcPkts : 0.f);
    printf("    Client → Server:          %d (%.1f%%)\n",
           clientPkts, mcPkts > 0 ? 100.f * clientPkts / mcPkts : 0.f);
    printf("  ─────────────────────────────────────────────────────────────────\n");
}

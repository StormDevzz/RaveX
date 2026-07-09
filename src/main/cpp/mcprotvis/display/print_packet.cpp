#include "../include/display.hpp"
#include <cstdio>
#include <ctime>
#include <algorithm>

void printHeader() {
    printf("\n");
    printf("  ╔════════════════════════════════════════════════════════════════╗\n");
    printf("  ║         Minecraft Protocol Visualizer - mcprotvis             ║\n");
    printf("  ╚════════════════════════════════════════════════════════════════╝\n");
    printf("  Legend:  ◀ S→C  (server->client)   ▶ C→S  (client->server)\n");
    printf("  ─────────────────────────────────────────────────────────────────\n");
}

void printPacket(int idx, const PacketRecord& rec, const PacketInfo* info, bool showHex) {
    char timeBuf[32];
    time_t sec = time_t(rec.timestamp);
    struct tm* tm = localtime(&sec);
    strftime(timeBuf, sizeof(timeBuf), "%H:%M:%S", tm);
    int msec = int((rec.timestamp - double(sec)) * 1000);

    if (info) {
        const char* arrow = rec.fromServer ? " ◀ " : " ▶ ";
        printf("  #%-4d %s.%03d %s %-32s  len=%-5d id=0x%02x\n",
               idx, timeBuf, msec, arrow,
               info->name.c_str(), info->length, info->id);
        if (showHex && rec.data.size() <= 256) {
            printHexDump(rec.data.data(), rec.data.size());
        }
    } else {
        printf("  #%-4d %s.%03d  [?] Non-MC packet (%u bytes)\n",
               idx, timeBuf, msec, rec.rawLen);
    }
}

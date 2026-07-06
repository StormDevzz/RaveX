#include "sniffer.hpp"
#include "../include/config.hpp"
#include "../include/logger.hpp"
#include "../include/packet_platform.hpp"
#include <iostream>
#include <cstring>
#include <csignal>
#include <thread>
#include <chrono>
#include "../include/packet_utils.hpp"

static packet::Sniffer* gSniffer = nullptr;

static void onSignal(int) {
    if (gSniffer) gSniffer->stop();
}

static void printPacket(const packet::Packet& pkt) {
    const char* dir = pkt.outgoing ? "C->S" : "S->C";
    auto name = packet::utils::packetName(pkt.id);
    std::cout << "[" << packet::platform::timestamp() << "] "
              << dir << " id=0x" << std::hex << pkt.id << std::dec
              << " (" << name << ") "
              << pkt.data.size() << " bytes\n";
}

int main(int argc, char** argv) {
    auto cfg = packet::Config::fromArgs(argc, argv);

    std::signal(SIGINT, onSignal);
    std::signal(SIGTERM, onSignal);

    if (!packet::platform::isRoot()) {
        packet::log::warn("not running as root, pcap may fail");
    }

    packet::PcapSniffer sniffer;
    gSniffer = &sniffer;

    sniffer.onPacket(printPacket);

    packet::SnifferConfig sCfg;
    sCfg.interface = cfg.interface;
    sCfg.filterExp = cfg.filter;

    if (sniffer.start(sCfg)) {
        packet::log::info("capturing... press Ctrl+C to stop");
        while (sniffer.running()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }

    auto& stats = sniffer.getStats();
    std::cout << "\n--- stats ---\n"
              << "packets: " << stats.packetsCaptured << "\n"
              << "bytes:   " << stats.bytesCaptured << "\n"
              << "errors:  " << stats.errors << "\n";

    return 0;
}

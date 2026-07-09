#pragma once
#include "../include/pcap_reader.hpp"
#include "../include/packet_analysis.hpp"

void printHeader();
void printPacket(int idx, const PacketRecord& rec, const PacketInfo* info, bool showHex);
void printSummary(int totalPkts, int mcPkts, int serverPkts, int clientPkts);
void printHexDump(const uint8_t* data, size_t len, int bytesPerLine = 16);

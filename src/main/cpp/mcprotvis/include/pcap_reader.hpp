#pragma once
#include <cstdint>
#include <cstdio>
#include <string>
#include <vector>

struct PacketRecord {
    double timestamp;
    uint32_t rawLen;
    uint32_t origLen;
    uint16_t srcPort;
    uint16_t dstPort;
    bool     fromServer;
    std::vector<uint8_t> data;
};

struct PcapReader {
    FILE* fp = nullptr;
    bool  bigEndian = false;
    uint32_t linkType = 0;
    uint64_t packetsRead = 0;

    bool open(const std::string& path);
    bool readGlobalHeader();
    bool nextPacket(PacketRecord& out);
    void close();

    ~PcapReader() { close(); }
};


bool filterMcPacket(PacketRecord& rec, uint32_t linkType);

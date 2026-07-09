#include "../include/pcap_reader.hpp"
#include <cstdint>

extern uint16_t r16(const uint8_t* p, bool be);
extern uint32_t r32(const uint8_t* p, bool be);

// Extract TCP payload from raw pcap packet data.
// Returns false if not IPv4/TCP or not on Minecraft port.
// On success fills srcPort, dstPort, fromServer, and shrinks data to TCP payload.
bool filterMcPacket(PacketRecord& rec, uint32_t linkType) {
    const uint8_t* data = rec.data.data();
    uint32_t len = rec.rawLen;
    uint32_t off = 0;

    if (linkType == 1) { // Ethernet
        if (len < 14) return false;
        uint16_t ethType = uint16_t(data[12] << 8 | data[13]);
        off = 14;
        if (ethType == 0x8100) { // VLAN
            if (off + 4 > len) return false;
            ethType = uint16_t(data[off+2] << 8 | data[off+3]);
            off += 4;
        }
        if (ethType != 0x0800) return false;
    }

    // IP header
    if (off + 20 > len) return false;
    uint8_t ihl = data[off] & 0x0F;
    if (data[off + 9] != 6) return false; // not TCP
    uint32_t ipEnd = off + ihl * 4;
    if (ipEnd + 20 > len) return false;

    // TCP header: source/dest ports are first 4 bytes
    rec.srcPort = uint16_t(data[ipEnd] << 8 | data[ipEnd + 1]);
    rec.dstPort = uint16_t(data[ipEnd + 2] << 8 | data[ipEnd + 3]);
    if (rec.srcPort != 25565 && rec.dstPort != 25565) return false;

    uint8_t tcpOff = (data[ipEnd + 12] >> 4) & 0x0F;
    uint32_t payloadOff = ipEnd + tcpOff * 4;
    if (payloadOff > len) return false;

    rec.fromServer = (rec.srcPort == 25565);
    // Remove link/IP/TCP headers, keep only payload
    rec.data.erase(rec.data.begin(), rec.data.begin() + payloadOff);
    return true;
}

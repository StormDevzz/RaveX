#include "../include/packet_analysis.hpp"

int readVarInt(const uint8_t* data, size_t len, size_t& pos, int& out) {
    out = 0;
    int shift = 0;
    size_t start = pos;
    while (pos < len) {
        uint8_t b = data[pos++];
        out |= (b & 0x7F) << shift;
        shift += 7;
        if (!(b & 0x80)) return int(pos - start);
    }
    return -1;
}

bool isMcPacket(const uint8_t* data, size_t len) {
    if (len < 2) return false;
    size_t pos = 0;
    int pktLen = 0;
    int bytes = readVarInt(data, len, pos, pktLen);
    if (bytes < 1 || pktLen < 1) return false;
    if (pos + pktLen > len) return false;
    size_t idPos = pos;
    int id = 0;
    if (readVarInt(data, len, idPos, id) < 1) return false;
    return true;
}

bool parsePacket(const uint8_t* data, size_t len, size_t& pos, PacketInfo& out) {
    if (pos >= len) return false;
    size_t start = pos;

    int pktLen = 0;
    if (readVarInt(data, len, pos, pktLen) < 1 || pktLen < 1) {
        pos = start;
        return false;
    }

    size_t payloadEnd = pos + pktLen;
    if (payloadEnd > len) { pos = start; return false; }

    int id = 0;
    if (readVarInt(data, len, pos, id) < 1) { pos = start; return false; }

    out.id = id;
    out.length = pktLen;
    out.name = lookupPacketName(id);
    out.state = lookupPacketState(id);
    pos = payloadEnd;
    return true;
}

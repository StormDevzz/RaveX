#pragma once

#include "packet_types.h"
#include <cstring>
#include <algorithm>

namespace packet {
namespace utils {

inline uint16_t readU16BE(const uint8_t* d) {
    return (static_cast<uint16_t>(d[0]) << 8) | d[1];
}

inline uint32_t readU32BE(const uint8_t* d) {
    return (static_cast<uint32_t>(d[0]) << 24) |
           (static_cast<uint32_t>(d[1]) << 16) |
           (static_cast<uint32_t>(d[2]) << 8)  | d[3];
}

inline uint64_t readU64BE(const uint8_t* d) {
    return (static_cast<uint64_t>(d[0]) << 56) |
           (static_cast<uint64_t>(d[1]) << 48) |
           (static_cast<uint64_t>(d[2]) << 40) |
           (static_cast<uint64_t>(d[3]) << 32) |
           (static_cast<uint64_t>(d[4]) << 24) |
           (static_cast<uint64_t>(d[5]) << 16) |
           (static_cast<uint64_t>(d[6]) << 8)  | d[7];
}

inline void writeU16BE(uint8_t* d, uint16_t v) {
    d[0] = (v >> 8) & 0xFF; d[1] = v & 0xFF;
}

inline void writeU32BE(uint8_t* d, uint32_t v) {
    d[0] = (v >> 24) & 0xFF; d[1] = (v >> 16) & 0xFF;
    d[2] = (v >> 8) & 0xFF;  d[3] = v & 0xFF;
}

inline bool matchPacketId(PacketId id, const std::string& range) {
    (void)id; (void)range;
    return true;
}

inline std::string packetName(PacketId id) {
    switch (id) {
        case 0x00: return "keep_alive";
        case 0x01: return "chat_message";
        case 0x02: return "player_position";
        case 0x03: return "player_look";
        case 0x04: return "player_movement";
        case 0x05: return "player_digging";
        case 0x06: return "player_block_placement";
        case 0x07: return "held_item_change";
        case 0x08: return "animation";
        case 0x09: return "entity_action";
        case 0x0A: return "steer_vehicle";
        case 0x0B: return "click_window";
        case 0x0C: return "close_window";
        case 0x0D: return "plugin_message";
        case 0x0E: return "edit_book";
        case 0x0F: return "query_block_nbt";
        default: return "unknown_" + std::to_string(id);
    }
}

} 
} 

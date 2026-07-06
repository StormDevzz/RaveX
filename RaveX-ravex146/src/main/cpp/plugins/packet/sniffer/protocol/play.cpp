#include "protocol.hpp"
#include "../../include/logger.hpp"
#include "../../include/packet_utils.hpp"

namespace packet {
namespace proto {

struct PlayPacketEntry {
    PacketId id;
    const char* name;
    bool clientbound;
};

static PlayPacketEntry playPackets[] = {
    {0x00, "keep_alive_cb", true},   {0x01, "keep_alive_sb", false},
    {0x02, "chat_cb", true},         {0x03, "chat_sb", false},
    {0x04, "position_cb", true},     {0x05, "position_sb", false},
    {0x06, "chunk_data", true},      {0x07, "block_update", true},
    {0x08, "entity_vel", true},      {0x09, "entity_teleport", true},
    {0x0A, "entity_move", true},     {0x0B, "entity_look", true},
    {0x0C, "entity_meta", true},     {0x0D, "entity_equip", true},
    {0x0E, "spawn_entity", true},    {0x0F, "spawn_player", true},
};

void parsePlayPacket(const Packet& raw) {
    auto name = utils::packetName(raw.id);
    log::debug("play packet 0x" + std::to_string(raw.id) + " " + name);
}

} 
} 

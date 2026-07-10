#include "../include/packet_analysis.hpp"

struct PacketNameEntry {
    int id;
    const char* name;
    const char* state;
};

static const PacketNameEntry PACKET_NAMES[] = {
    {0x00, "Keep Alive (C->S)",       "play"},
    {0x01, "Chat Message (C)",        "play"},
    {0x03, "Player Position",         "play"},
    {0x04, "Player Pos & Rot",        "play"},
    {0x05, "Player Rotation",         "play"},
    {0x09, "Plugin Message (C)",      "play"},
    {0x0D, "Player Abilities (C)",    "play"},
    {0x11, "Held Item Change (C)",    "play"},
    {0x15, "Entity Action",           "play"},
    {0x1A, "Client Status",           "play"},
    {0x1B, "Client Settings",         "play"},
    {0x20, "Click Window",            "play"},
    {0x24, "Entity NBT Request",      "play"},
    {0x25, "Interact Entity",         "play"},
    {0x27, "Keep Alive (S->C)",       "play"},
    {0x2A, "Player Digging",          "play"},
    {0x2F, "Animation (C)",           "play"},
    {0x31, "Creative Inventory",      "play"},
    {0x36, "Update Sign",             "play"},
    {0x3F, "Block Placement",         "play"},
    {0x00, "Spawn Mob",               "play"},
    {0x01, "Spawn Exp Orb",           "play"},
    {0x06, "Spawn Global Entity",     "play"},
    {0x09, "Block Update",            "play"},
    {0x0E, "Chat Message (S)",        "play"},
    {0x0F, "Multi Block Change",      "play"},
    {0x10, "Sync Player Pos",         "play"},
    {0x1C, "Entity Velocity",         "play"},
    {0x1E, "Entity Equipment",        "play"},
    {0x1F, "Entity Status",           "play"},
    {0x21, "Entity Properties",       "play"},
    {0x22, "Entity Effect",           "play"},
    {0x2B, "Change Game State",       "play"},
    {0x2E, "Open Container",          "play"},
    {0x30, "Container Set Content",   "play"},
    {0x31, "Container Set Slot",      "play"},
    {0x37, "Disconnect (Play)",        "play"},
    {0x41, "Scoreboard Objective",    "play"},
    {0x46, "Teams",                   "play"},
    {0x47, "Particle",                "play"},
    {0x48, "Ping",                    "play"},
    {0x49, "Time Update",             "play"},
    {0x4A, "Title",                   "play"},
    {0x4C, "Health Update",           "play"},
    {0x50, "Respawn",                 "play"},
    {0x56, "Entity Animation",        "play"},
    {0x58, "Entity Destroy",          "play"},
    {0x5E, "Map Data",                "play"},
    {0x5F, "Block Entity Data",       "play"},
    {0x62, "Statistics",              "play"},
    {0x64, "Player Abilities (S)",    "play"},
    {0x69, "Unload Chunk",            "play"},
    {0x6A, "Game Event",              "play"},
    {0x6D, "Difficulty",              "play"},
    {0x6E, "Set Experience",          "play"},

    {0x00, "Login Start",             "login"},
    {0x02, "Login Plugin Response",   "login"},
    {0x03, "Login Acknowledged",      "login"},
    {0x00, "Disconnect (Login)",      "login"},
    {0x01, "Encryption Request",      "login"},
    {0x02, "Login Success",           "login"},
    {0x03, "Login Plugin Request",    "login"},

    {0x00, "Status Request",          "status"},
    {0x01, "Ping Request",            "status"},
    {0x00, "Status Response",         "status"},
    {0x01, "Pong Response",           "status"},

    {0x00, "Handshake",               "handshake"},
};

static constexpr size_t NUM_NAMES = sizeof(PACKET_NAMES) / sizeof(PACKET_NAMES[0]);

const char* lookupPacketName(int id) {
    for (size_t i = 0; i < NUM_NAMES; i++) {
        if (PACKET_NAMES[i].id == id)
            return PACKET_NAMES[i].name;
    }
    return "Unknown";
}

const char* lookupPacketState(int id) {
    for (size_t i = 0; i < NUM_NAMES; i++) {
        if (PACKET_NAMES[i].id == id)
            return PACKET_NAMES[i].state;
    }
    return "play";
}

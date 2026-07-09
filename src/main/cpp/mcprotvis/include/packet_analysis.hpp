#pragma once
#include <cstdint>
#include <string>
#include <vector>

struct PacketInfo {
    int id;
    std::string name;
    std::string state;
    int length;
};

bool isMcPacket(const uint8_t* data, size_t len);
int  readVarInt(const uint8_t* data, size_t len, size_t& pos, int& out);
bool parsePacket(const uint8_t* data, size_t len, size_t& pos, PacketInfo& out);
const char* lookupPacketName(int id);
const char* lookupPacketState(int id);

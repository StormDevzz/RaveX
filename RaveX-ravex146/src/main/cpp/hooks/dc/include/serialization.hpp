#pragma once
#include <string>
#include <vector>
#include "discord_rpc.hpp"

namespace DiscordSerialization {
    std::string serializeHandshake(const std::string& clientId);
    std::string serializeActivity(const DiscordRichPresence& presence, int pid, const std::string& nonce);
    std::string serializeClearActivity(int pid, const std::string& nonce);
}

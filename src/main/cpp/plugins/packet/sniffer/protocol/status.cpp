#include "protocol.h"
#include "../../include/logger.h"
#include "../../include/packet_utils.h"

namespace packet {
namespace proto {

std::vector<uint8_t> StatusRequest::encode() {
    return {};
}

StatusResponse StatusResponse::decode(const uint8_t* data, size_t size) {
    PacketReader r(data, size);
    StatusResponse s;
    s.json = r.readString();
    return s;
}

void parseStatus(const Packet& pkt) {
    if (!pkt.outgoing && pkt.id == 0x00) {
        auto resp = StatusResponse::decode(pkt.data.data(), pkt.data.size());
        log::info("server info: " + resp.json);
    }
}

} // namespace proto
} // namespace packet

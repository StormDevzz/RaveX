#include "protocol.h"

namespace packet {
namespace proto {

std::vector<uint8_t> Handshake::encode() {
    PacketWriter w;
    w.writeVar(protocolVersion);
    w.writeString(serverAddress);
    w.writeU16(port);
    w.writeVar(nextState);
    return w.buf;
}

Handshake Handshake::decode(const uint8_t* data, size_t size) {
    PacketReader r(data, size);
    Handshake h;
    h.protocolVersion = r.readVar();
    h.serverAddress = r.readString();
    h.port = r.readU16();
    h.nextState = r.readVar();
    return h;
}

} // namespace proto
} // namespace packet

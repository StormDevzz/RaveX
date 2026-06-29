#include "protocol.h"

namespace packet {
namespace proto {

LoginStart LoginStart::decode(const uint8_t* data, size_t size) {
    PacketReader r(data, size);
    LoginStart l;
    l.username = r.readString();
    return l;
}

std::vector<uint8_t> LoginStart::encode() {
    PacketWriter w;
    w.writeString(username);
    return w.buf;
}

LoginSuccess LoginSuccess::decode(const uint8_t* data, size_t size) {
    PacketReader r(data, size);
    LoginSuccess l;
    l.uuid = r.readString();
    l.username = r.readString();
    return l;
}

EncryptionRequest EncryptionRequest::decode(const uint8_t* data, size_t size) {
    PacketReader r(data, size);
    EncryptionRequest e;
    e.serverId = r.readString();
    int kl = r.readVar(); e.publicKey = r.readBytes(kl);
    int vl = r.readVar(); e.verifyToken = r.readBytes(vl);
    return e;
}

EncryptionResponse EncryptionResponse::decode(const uint8_t* data, size_t size) {
    PacketReader r(data, size);
    EncryptionResponse e;
    int sl = r.readVar(); e.sharedSecret = r.readBytes(sl);
    int vl = r.readVar(); e.verifyToken = r.readBytes(vl);
    return e;
}

std::vector<uint8_t> EncryptionResponse::encode() {
    PacketWriter w;
    w.writeVar(static_cast<int32_t>(sharedSecret.size()));
    w.writeBytes(sharedSecret.data(), sharedSecret.size());
    w.writeVar(static_cast<int32_t>(verifyToken.size()));
    w.writeBytes(verifyToken.data(), verifyToken.size());
    return w.buf;
}

std::vector<uint8_t> CompressionRequest::encode() {
    PacketWriter w;
    w.writeVar(threshold);
    return w.buf;
}

CompressionRequest CompressionRequest::decode(const uint8_t* data, size_t size) {
    PacketReader r(data, size);
    return {r.readVar()};
}

} 
} 

#pragma once

#include "../../include/packet_types.hpp"

namespace packet {

class CaptureBackend {
public:
    virtual ~CaptureBackend() = default;
    virtual bool init(const SnifferConfig& cfg) = 0;
    virtual bool nextPacket(Packet& pkt) = 0;
    virtual void shutdown() = 0;
    virtual const char* name() const = 0;
};

}

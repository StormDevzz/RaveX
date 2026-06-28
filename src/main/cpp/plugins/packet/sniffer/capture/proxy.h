#pragma once

#include "../../include/packet_types.h"
#include <functional>

namespace packet {

class Proxy {
public:
    using Callback = std::function<void(const Packet&)>;

    bool start(const Address& target, uint16_t listenPort = 25566);
    bool acceptClient();
    void stop();
    bool running() const { return running_; }

    bool relayPacket(int from, int to, bool outgoing);
    void onPacket(Callback cb) { callback_ = std::move(cb); }

private:
    bool running_ = false;
    int serverFd_ = -1;
    int clientFd_ = -1;
    int remoteFd_ = -1;
    Address target_;
    Callback callback_;
};

} // namespace packet

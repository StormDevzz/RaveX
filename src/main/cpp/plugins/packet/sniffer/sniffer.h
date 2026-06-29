#pragma once

#include "../include/packet_types.h"
#include "../include/packet_errors.h"
#include "../include/stats.h"
#include <functional>
#include <memory>

namespace packet {

using PacketCallback = std::function<void(const Packet&)>;

class Sniffer {
public:
    virtual ~Sniffer() = default;
    virtual bool start(const SnifferConfig& cfg) = 0;
    virtual void stop() = 0;
    virtual bool running() const = 0;
    virtual const stats::SnifferStats& getStats() const = 0;

    void onPacket(PacketCallback cb) { callback_ = std::move(cb); }

    stats::SnifferStats stats_;
    PacketCallback callback_;
};

class PcapSniffer : public Sniffer {
public:
    bool start(const SnifferConfig& cfg) override;
    void stop() override;
    bool running() const override { return running_; }
    const stats::SnifferStats& getStats() const override { return stats_; }

private:
    bool running_ = false;
    void* pcapHandle_ = nullptr;
};

class ProxySniffer : public Sniffer {
public:
    bool start(const SnifferConfig& cfg) override;
    void stop() override;
    bool running() const override { return running_; }
    const stats::SnifferStats& getStats() const override { return stats_; }

private:
    bool running_ = false;
    int serverFd_ = -1;
    int clientFd_ = -1;
};

} 

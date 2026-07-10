#include "sniffer.hpp"
#include "../include/packet_platform.hpp"
#include "../include/logger.hpp"
#include <thread>
#include <unistd.h>

namespace packet {



#if defined(HAVE_PCAP)
#include <pcap/pcap.h>

static void pcapCallback(u_char* user, const struct pcap_pkthdr* h, const u_char* bytes) {
    auto* sniffer = reinterpret_cast<PcapSniffer*>(user);
    if (!sniffer || !h || !bytes) return;

    Packet pkt;
    pkt.time = static_cast<Timestamp>(h->ts.tv_sec * 1000 + h->ts.tv_usec / 1000);
    pkt.data.assign(bytes, bytes + h->len);
    stats::record(const_cast<stats::SnifferStats&>(sniffer->getStats()), pkt);

    if (sniffer->callback_)
        sniffer->callback_(pkt);
}

bool PcapSniffer::start(const SnifferConfig& cfg) {
    char errbuf[PCAP_ERRBUF_SIZE];
    auto* handle = pcap_open_live(cfg.interface.c_str(), cfg.snapLen,
                                  cfg.promiscuous ? 1 : 0, cfg.timeout, errbuf);
    if (!handle) {
        log::error(std::string("pcap_open_live: ") + errbuf);
        return false;
    }

    if (!cfg.filterExp.empty()) {
        struct bpf_program fp;
        if (pcap_compile(handle, &fp, cfg.filterExp.c_str(), 0, PCAP_NETMASK_UNKNOWN) == -1) {
            log::error("pcap_compile failed");
            pcap_close(handle);
            return false;
        }
        if (pcap_setfilter(handle, &fp) == -1) {
            log::error("pcap_setfilter failed");
            pcap_close(handle);
            return false;
        }
    }

    pcapHandle_ = handle;
    running_ = true;
    log::info("pcap capture started on " + cfg.interface);
    pcap_loop(handle, -1, pcapCallback, reinterpret_cast<u_char*>(this));
    return true;
}

void PcapSniffer::stop() {
    running_ = false;
    if (pcapHandle_) {
        auto* handle = static_cast<pcap_t*>(pcapHandle_);
        pcap_breakloop(handle);
        pcap_close(handle);
        pcapHandle_ = nullptr;
    }
}

#else
bool PcapSniffer::start(const SnifferConfig&) {
    log::error("pcap not available (install libpcap)");
    return false;
}
void PcapSniffer::stop() { running_ = false; }
#endif



bool ProxySniffer::start(const SnifferConfig& cfg) {
    log::info("proxy sniffer not yet implemented");
    (void)cfg;
    return false;
}

void ProxySniffer::stop() {
    running_ = false;
    if (serverFd_ >= 0) { ::close(serverFd_); serverFd_ = -1; }
    if (clientFd_ >= 0) { ::close(clientFd_); clientFd_ = -1; }
}

}

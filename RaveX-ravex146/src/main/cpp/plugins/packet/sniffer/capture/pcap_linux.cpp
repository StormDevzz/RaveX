#include "capture.hpp"
#include "../../include/logger.hpp"

#if defined(HAVE_PCAP)
#include <pcap/pcap.h>

namespace packet {

class PcapBackend : public CaptureBackend {
public:
    bool init(const SnifferConfig& cfg) override {
        char errbuf[PCAP_ERRBUF_SIZE];
        handle_ = pcap_open_live(cfg.interface.c_str(), cfg.snapLen,
                                  cfg.promiscuous ? 1 : 0, cfg.timeout, errbuf);
        if (!handle_) {
            log::error(std::string("pcap: ") + errbuf);
            return false;
        }
        if (!cfg.filterExp.empty()) {
            struct bpf_program fp;
            if (pcap_compile(handle_, &fp, cfg.filterExp.c_str(), 0, PCAP_NETMASK_UNKNOWN) < 0)
                return false;
            pcap_setfilter(handle_, &fp);
        }
        return true;
    }

    bool nextPacket(Packet& pkt) override {
        if (!handle_) return false;
        struct pcap_pkthdr* hdr;
        const u_char* data;
        int r = pcap_next_ex(handle_, &hdr, &data);
        if (r <= 0) return false;
        pkt.time = hdr->ts.tv_sec * 1000 + hdr->ts.tv_usec / 1000;
        pkt.data.assign(data, data + hdr->len);
        return true;
    }

    void shutdown() override {
        if (handle_) { pcap_close(handle_); handle_ = nullptr; }
    }

    const char* name() const override { return "pcap"; }

private:
    pcap_t* handle_ = nullptr;
};

}
#else
namespace packet {
class PcapBackend : public CaptureBackend {
public:
    bool init(const SnifferConfig&) override { return false; }
    bool nextPacket(Packet&) override { return false; }
    void shutdown() override {}
    const char* name() const override { return "pcap (unavailable)"; }
};
}
#endif

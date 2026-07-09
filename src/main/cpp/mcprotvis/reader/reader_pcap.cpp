#include "../include/pcap_reader.hpp"
#include <cstdio>
#include <cstring>

uint16_t r16(const uint8_t* p, bool be) {
    return be ? uint16_t(p[0]<<8 | p[1]) : uint16_t(p[0] | p[1]<<8);
}
uint32_t r32(const uint8_t* p, bool be) {
    return be ? uint32_t(p[0]<<24 | p[1]<<16 | p[2]<<8 | p[3])
              : uint32_t(p[0] | p[1]<<8 | p[2]<<16 | p[3]<<24);
}

bool PcapReader::open(const std::string& path) {
    fp = fopen(path.c_str(), "rb");
    if (!fp) { perror("fopen"); return false; }
    return readGlobalHeader();
}

bool PcapReader::readGlobalHeader() {
    if (!fp) return false;
    uint8_t hdr[24];
    if (fread(hdr, 1, 24, fp) != 24) {
        fprintf(stderr, "Not a pcap file\n");
        return false;
    }
    uint32_t magic = r32(hdr, false);
    if (magic == 0xa1b2c3d4) bigEndian = false;
    else if (magic == 0xd4c3b2a1) bigEndian = true;
    else { fprintf(stderr, "Bad pcap magic: 0x%08x\n", magic); return false; }
    linkType = r32(hdr + 20, bigEndian);
    return true;
}

bool PcapReader::nextPacket(PacketRecord& out) {
    if (!fp) return false;
    uint8_t pkhdr[16];
    if (fread(pkhdr, 1, 16, fp) != 16) return false;

    out.timestamp = double(r32(pkhdr, bigEndian)) + double(r32(pkhdr + 4, bigEndian)) / 1e6;
    out.rawLen = r32(pkhdr + 8, bigEndian);
    out.origLen = r32(pkhdr + 12, bigEndian);

    if (out.rawLen > 65535 || out.rawLen == 0) {
        fseek(fp, out.rawLen, SEEK_CUR);
        return false;
    }

    std::vector<uint8_t> pkt(out.rawLen);
    if (fread(pkt.data(), 1, out.rawLen, fp) != out.rawLen) return false;
    out.data = std::move(pkt);
    packetsRead++;
    return true;
}

void PcapReader::close() {
    if (fp) { fclose(fp); fp = nullptr; }
}

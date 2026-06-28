#include "output.h"
#include "../../include/packet_utils.h"
#include <iostream>
#include <sstream>

namespace packet {
namespace output {

TextOutput::TextOutput(const std::string& path) {
    if (!path.empty()) {
        file_.open(path);
        useFile_ = file_.is_open();
    }
}

TextOutput::~TextOutput() { flush(); }

void TextOutput::write(const Packet& pkt) {
    const char* dir = pkt.outgoing ? "C->S" : "S->C";
    auto name = utils::packetName(pkt.id);

    std::ostringstream ss;
    ss << "[" << pkt.time << "] " << dir
       << " id=0x" << std::hex << pkt.id << std::dec
       << " (" << name << ") " << pkt.data.size() << "B";

    if (useFile_) file_ << ss.str() << std::endl;
    else std::cout << ss.str() << std::endl;
}

void TextOutput::flush() { if (useFile_) file_.flush(); }

// ---- JsonOutput -----------------------------------------------------------

JsonOutput::JsonOutput(const std::string& path) {
    if (!path.empty()) {
        file_.open(path);
        useFile_ = file_.is_open();
    }
}

JsonOutput::~JsonOutput() { end(); flush(); }

void JsonOutput::begin() {
    if (useFile_) file_ << "{\"packets\":[\n";
    else std::cout << "{\"packets\":[\n";
    first_ = true;
}

void JsonOutput::write(const Packet& pkt) {
    const char* dir = pkt.outgoing ? "C2S" : "S2C";
    auto name = utils::packetName(pkt.id);

    std::ostringstream ss;
    if (!first_) ss << ",\n";
    first_ = false;
    ss << "  {\"time\":" << pkt.time
       << ",\"dir\":\"" << dir << "\""
       << ",\"id\":0x" << std::hex << pkt.id << std::dec
       << ",\"name\":\"" << name << "\""
       << ",\"size\":" << pkt.data.size()
       << ",\"hex\":\"";

    for (size_t i = 0; i < pkt.data.size() && i < 64; ++i) {
        char buf[4]; std::snprintf(buf, 4, "%02x", pkt.data[i]);
        ss << buf;
    }
    if (pkt.data.size() > 64) ss << "...";
    ss << "\"}";

    if (useFile_) file_ << ss.str();
    else std::cout << ss.str();
}

void JsonOutput::end() {
    if (useFile_) file_ << "\n]}\n";
    else std::cout << "\n]}\n";
}

void JsonOutput::flush() { if (useFile_) file_.flush(); }

} // namespace output
} // namespace packet

#pragma once

#include "../../include/packet_types.hpp"
#include <string>
#include <vector>
#include <fstream>

namespace packet {
namespace output {

class OutputHandler {
public:
    virtual ~OutputHandler() = default;
    virtual void begin() {}
    virtual void write(const Packet& pkt) = 0;
    virtual void end() {}
    virtual void flush() {}
};

class TextOutput : public OutputHandler {
public:
    explicit TextOutput(const std::string& path = "");
    ~TextOutput() override;
    void write(const Packet& pkt) override;
    void flush() override;

private:
    std::ofstream file_;
    bool useFile_ = false;
};

class JsonOutput : public OutputHandler {
public:
    explicit JsonOutput(const std::string& path = "");
    ~JsonOutput() override;
    void begin() override;
    void write(const Packet& pkt) override;
    void end() override;
    void flush() override;

private:
    std::ofstream file_;
    bool useFile_ = false;
    bool first_ = true;
};

}
}

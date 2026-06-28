#pragma once

#include <string>
#include <exception>

namespace packet {

enum class ErrorCode : int {
    Success = 0,
    InvalidArg = -1,
    NoInterface = -2,
    PermDenied = -3,
    ReadError = -4,
    WriteError = -5,
    ParseError = -6,
    ProtoError = -7,
    EncryptError = -8,
    DecryptError = -9,
    CompressError = -10,
    DecompressError = -11,
    AuthFailed = -12,
    Timeout = -13,
    BufferFull = -14,
    NotConnected = -15,
};

class Error : public std::exception {
public:
    explicit Error(ErrorCode c, std::string m = {})
        : code_(c), msg_(m.empty() ? def(c) : m) {}
    ErrorCode code() const { return code_; }
    const char* what() const noexcept override { return msg_.c_str(); }
private:
    static std::string def(ErrorCode c) {
        switch (c) {
            case ErrorCode::Success: return "ok";
            case ErrorCode::InvalidArg: return "invalid argument";
            case ErrorCode::NoInterface: return "no such interface";
            case ErrorCode::PermDenied: return "permission denied (need root)";
            case ErrorCode::ReadError: return "read error";
            case ErrorCode::WriteError: return "write error";
            case ErrorCode::ParseError: return "parse error";
            case ErrorCode::ProtoError: return "protocol error";
            case ErrorCode::AuthFailed: return "auth failed";
            case ErrorCode::Timeout: return "timeout";
            default: return "unknown error";
        }
    }
    ErrorCode code_;
    std::string msg_;
};

} // namespace packet

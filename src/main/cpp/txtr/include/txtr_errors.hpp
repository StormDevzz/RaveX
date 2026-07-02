#pragma once

#include <string>
#include <exception>

namespace txtr {

enum class ErrorCode : int {
    Success = 0,
    InvalidArgument = -1,
    UnsupportedFormat = -2,
    FileNotFound = -3,
    FileReadError = -4,
    FileWriteError = -5,
    OutOfMemory = -6,
    InvalidData = -7,
    NotImplemented = -8,
    DecodeError = -9,
    EncodeError = -10,
    InvalidDimensions = -11,
    FormatMismatch = -12,
    PlatformError = -13,
};

class Error : public std::exception {
public:
    explicit Error(ErrorCode code, std::string msg = {})
        : code_(code), msg_(msg.empty() ? defaultMessage(code) : msg) {}

    ErrorCode code() const { return code_; }
    const char* what() const noexcept override { return msg_.c_str(); }

    static std::string defaultMessage(ErrorCode code) {
        switch (code) {
            case ErrorCode::Success: return "success";
            case ErrorCode::InvalidArgument: return "invalid argument";
            case ErrorCode::UnsupportedFormat: return "unsupported image format";
            case ErrorCode::FileNotFound: return "file not found";
            case ErrorCode::FileReadError: return "file read error";
            case ErrorCode::FileWriteError: return "file write error";
            case ErrorCode::OutOfMemory: return "out of memory";
            case ErrorCode::InvalidData: return "invalid image data";
            case ErrorCode::NotImplemented: return "not implemented";
            case ErrorCode::DecodeError: return "decode error";
            case ErrorCode::EncodeError: return "encode error";
            case ErrorCode::InvalidDimensions: return "invalid image dimensions";
            case ErrorCode::FormatMismatch: return "format mismatch";
            case ErrorCode::PlatformError: return "platform error";
            default: return "unknown error";
        }
    }

private:
    ErrorCode code_;
    std::string msg_;
};

#define TXTR_TRY(expr) do { auto _r = (expr); if (_r != ErrorCode::Success) return _r; } while(0)
#define TXTR_THROW(expr) do { if ((expr) != ErrorCode::Success) throw Error(expr); } while(0)

} 

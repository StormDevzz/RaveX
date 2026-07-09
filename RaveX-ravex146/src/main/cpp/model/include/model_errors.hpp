#pragma once

#include <string>
#include <exception>

namespace model {

enum class ErrorCode : int {
    Success = 0,
    InvalidArgument = -1,
    UnsupportedFormat = -2,
    FileNotFound = -3,
    FileReadError = -4,
    FileWriteError = -5,
    InvalidData = -6,
    DecodeError = -7,
    EncodeError = -8,
    MissingTexture = -9,
    InvalidBone = -10,
    AnimationError = -11,
    JsonError = -12,
    NotImplemented = -13,
};

class Error : public std::exception {
public:
    explicit Error(ErrorCode code, std::string msg = {})
        : code_(code), msg_(msg.empty() ? defMsg(code) : msg) {}

    ErrorCode code() const { return code_; }
    const char* what() const noexcept override { return msg_.c_str(); }

private:
    static std::string defMsg(ErrorCode c) {
        switch (c) {
            case ErrorCode::Success: return "success";
            case ErrorCode::InvalidArgument: return "invalid argument";
            case ErrorCode::UnsupportedFormat: return "unsupported model format";
            case ErrorCode::FileNotFound: return "file not found";
            case ErrorCode::FileReadError: return "file read error";
            case ErrorCode::FileWriteError: return "file write error";
            case ErrorCode::InvalidData: return "invalid model data";
            case ErrorCode::DecodeError: return "decode error";
            case ErrorCode::EncodeError: return "encode error";
            case ErrorCode::MissingTexture: return "missing texture";
            case ErrorCode::InvalidBone: return "invalid bone";
            case ErrorCode::AnimationError: return "animation error";
            case ErrorCode::JsonError: return "JSON parse error";
            case ErrorCode::NotImplemented: return "not implemented";
            default: return "unknown error";
        }
    }
    ErrorCode code_;
    std::string msg_;
};

} 

#include "core/include/sha256.hpp"
#include <windows.h>
#include <bcrypt.h>
#include <vector>

namespace ravex {

static const char* hexDigits = "0123456789abcdef";

static std::string hexEncode(const unsigned char* data, std::size_t len) {
    std::string out;
    out.reserve(len * 2);
    for (std::size_t i = 0; i < len; ++i) {
        out.push_back(hexDigits[data[i] >> 4]);
        out.push_back(hexDigits[data[i] & 0x0F]);
    }
    return out;
}

static bool hashInit(BCRYPT_ALG_HANDLE& alg, BCRYPT_HASH_HANDLE& hash) {
    if (BCryptOpenAlgorithmProvider(&alg, BCRYPT_SHA256_ALGORITHM, MS_PRIMITIVE_PROVIDER, 0) != 0) return false;
    if (BCryptCreateHash(alg, &hash, nullptr, 0, nullptr, 0, 0) != 0) {
        BCryptCloseAlgorithmProvider(alg, 0);
        return false;
    }
    return true;
}

static void hashFinish(BCRYPT_ALG_HANDLE alg, BCRYPT_HASH_HANDLE hash, unsigned char digest[32]) {
    BCryptFinishHash(hash, digest, 32, 0);
    BCryptDestroyHash(hash);
    BCryptCloseAlgorithmProvider(alg, 0);
}

static void hashAbort(BCRYPT_ALG_HANDLE alg, BCRYPT_HASH_HANDLE hash) {
    BCryptDestroyHash(hash);
    BCryptCloseAlgorithmProvider(alg, 0);
}

std::string sha256Data(const void* data, std::size_t len) {
    BCRYPT_ALG_HANDLE alg = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    if (!hashInit(alg, hash)) return std::string();
    if (BCryptHashData(hash, reinterpret_cast<PUCHAR>(const_cast<void*>(data)), static_cast<ULONG>(len), 0) != 0) {
        hashAbort(alg, hash);
        return std::string();
    }
    unsigned char digest[32] = {};
    hashFinish(alg, hash, digest);
    return hexEncode(digest, 32);
}

std::string sha256File(const std::wstring& path) {
    HANDLE hFile = CreateFileW(path.c_str(), GENERIC_READ, FILE_SHARE_READ, nullptr, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (hFile == INVALID_HANDLE_VALUE) return std::string();
    BCRYPT_ALG_HANDLE alg = nullptr;
    BCRYPT_HASH_HANDLE hash = nullptr;
    if (!hashInit(alg, hash)) {
        CloseHandle(hFile);
        return std::string();
    }
    std::vector<unsigned char> buffer(65536);
    bool ok = true;
    DWORD read = 0;
    while (ReadFile(hFile, buffer.data(), static_cast<DWORD>(buffer.size()), &read, nullptr) && read > 0) {
        if (BCryptHashData(hash, buffer.data(), read, 0) != 0) {
            ok = false;
            break;
        }
    }
    CloseHandle(hFile);
    if (!ok) {
        hashAbort(alg, hash);
        return std::string();
    }
    unsigned char digest[32] = {};
    hashFinish(alg, hash, digest);
    return hexEncode(digest, 32);
}

}
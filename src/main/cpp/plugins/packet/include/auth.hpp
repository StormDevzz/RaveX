#pragma once

#include <string>
#include <vector>

namespace packet {
namespace auth {

struct AuthResult {
    std::string username;
    std::string uuid;
    std::string accessToken;
    bool success = false;
};

AuthResult mojangAuth(const std::string& username, const std::string& password);
AuthResult microsoftAuth(const std::string& token);
AuthResult offlineAuth(const std::string& username);

bool verifySession(const std::string& serverId, const std::string& sharedSecret,
                   const std::string& publicKey);

}
}

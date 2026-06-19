#pragma once
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {

// проверяет валидность токена аккаунта
inline bool verify_account_token(const Account& acc) {
    return !acc.token.empty();
}

} // namespace acc
} // namespace simple
} // namespace launcher
} // namespace ravex

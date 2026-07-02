#pragma once
#include "../../state/include/launcher_state.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {


inline bool verify_account_token(const Account& acc) {
    return !acc.token.empty();
}

} 
} 
} 
} 

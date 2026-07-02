#include "include/account_manager.hpp"
#include "include/account_serializer.hpp"
#include "include/offline_auth.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {

void load_accounts(LauncherState *state) {
    deserialize_accounts(state);

    for (auto &acc : state->accounts) {
        if (!acc.is_microsoft) {
            std::string proper = generate_offline_uuid(acc.username);
            acc.uuid = proper;
        }
    }
    save_accounts(state);
}

void save_accounts(LauncherState *state) {
    serialize_accounts(state);
}

void add_offline_account(LauncherState *state, const std::string& username) {
    if (username.empty()) return;
    
    
    for (size_t i = 0; i < state->accounts.size(); ++i) {
        if (state->accounts[i].username == username) {
            state->active_account_index = i;
            save_accounts(state);
            return;
        }
    }

    Account acc;
    acc.username = username;
    acc.uuid = generate_offline_uuid(username);
    acc.token = "offline_token";
    acc.is_microsoft = false;

    state->accounts.push_back(acc);
    state->active_account_index = state->accounts.size() - 1;
    save_accounts(state);
}

void remove_account(LauncherState *state, int index) {
    if (index < 0 || index >= static_cast<int>(state->accounts.size())) return;
    
    state->accounts.erase(state->accounts.begin() + index);
    if (state->accounts.empty()) {
        state->active_account_index = -1;
    } else {
        state->active_account_index = 0;
    }
    save_accounts(state);
}

} 
} 
} 
} 

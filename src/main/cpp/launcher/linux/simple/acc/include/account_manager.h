#pragma once
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {


void load_accounts(LauncherState *state);


void save_accounts(LauncherState *state);


void add_offline_account(LauncherState *state, const std::string& username);


void remove_account(LauncherState *state, int index);

} 
} 
} 
} 

#pragma once
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {

// сериализация списка аккаунтов в JSON
void serialize_accounts(LauncherState *state);

// десериализация списка аккаунтов из JSON
void deserialize_accounts(LauncherState *state);

} // namespace acc
} // namespace simple
} // namespace launcher
} // namespace ravex

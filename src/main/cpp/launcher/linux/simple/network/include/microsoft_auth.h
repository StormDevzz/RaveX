#pragma once
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

// запуск процесса авторизации через учетную запись microsoft
// возвращает true при успешном добавлении аккаунта
bool login_microsoft_account(LauncherState *state);

} // namespace network
} // namespace simple
} // namespace launcher
} // namespace ravex

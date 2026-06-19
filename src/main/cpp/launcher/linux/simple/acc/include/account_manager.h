#pragma once
#include "../../state/include/launcher_state.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {

// загрузить аккаунты из файла
void load_accounts(LauncherState *state);

// сохранить аккаунты в файл
void save_accounts(LauncherState *state);

// добавить оффлайн аккаунт
void add_offline_account(LauncherState *state, const std::string& username);

// удалить аккаунт по индексу
void remove_account(LauncherState *state, int index);

} // namespace acc
} // namespace simple
} // namespace launcher
} // namespace ravex

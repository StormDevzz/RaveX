#pragma once
#include <gtk/gtk.h>
#include <string>
#include <vector>
#include <atomic>
#include <sys/types.h>

namespace ravex {
namespace launcher {
namespace simple {

// структура аккаунта игрока
struct Account {
    std::string username;
    std::string uuid;
    std::string token;
    bool is_microsoft = false;
};

// состояние лаунчера kickx
struct LauncherState {
    GtkWidget *window = nullptr;
    GtkWidget *status_label = nullptr;
    GtkWidget *version_label = nullptr;
    GtkWidget *progress_bar = nullptr;
    GtkWidget *btn_launch = nullptr;
    GtkWidget *btn_update = nullptr;
    GtkWidget *combo_accounts = nullptr; // комбобокс выбора аккаунтов

    std::string mods_dir;
    std::string ravex_dir;
    std::string kickx_dir; // директория ~/.kickx

    // список аккаунтов
    std::vector<Account> accounts;
    int active_account_index = -1;

    // переменные для управления запущенным процессом
    std::atomic<pid_t> game_pid{-1};
    std::atomic<bool> game_running{false};
    std::atomic<bool> update_active{false};

    // консоль
    GtkWidget *console_window = nullptr;
    GtkWidget *console_text = nullptr;
    int console_pipe = -1;
};

} // namespace simple
} // namespace launcher
} // namespace ravex

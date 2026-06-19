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

// структура инстанса
struct InstanceInfo {
    std::string name;
    std::string dir;
    std::string mc_version = "1.21.11";
    std::string icon_path;
    int ram_mb = 4096;
};

// состояние лаунчера kickx
struct LauncherState {
    GtkWidget *window = nullptr;
    GtkWidget *notebook = nullptr;
    GtkWidget *status_label = nullptr;
    GtkWidget *version_label = nullptr;
    GtkWidget *progress_bar = nullptr;
    GtkWidget *btn_launch = nullptr;
    GtkWidget *btn_update = nullptr;
    GtkWidget *combo_accounts = nullptr; // комбобокс выбора аккаунтов
    GtkWidget *combo_instances = nullptr; // комбобокс выбора инстанса
    GtkWidget *instances_box = nullptr;  // контейнер для карточек инстансов
    GtkWidget *instance_list = nullptr;  // flowbox или контейнер списка

    std::string mods_dir;
    std::string ravex_dir;
    std::string kickx_dir;

    // список аккаунтов
    std::vector<Account> accounts;
    int active_account_index = -1;

    // список инстансов
    std::vector<InstanceInfo> instances;
    int active_instance_index = 0;

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

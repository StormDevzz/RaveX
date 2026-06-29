#pragma once
#include <gtk/gtk.h>
#include <string>
#include <vector>
#include <atomic>
#include <sys/types.h>

namespace ravex {
namespace launcher {
namespace simple {


struct Account {
    std::string username;
    std::string uuid;
    std::string token;
    bool is_microsoft = false;
};


struct InstanceInfo {
    std::string name;
    std::string dir;
    std::string mc_version = "1.21.11";
    std::string icon_path;
    int ram_mb = 4096;
};


struct LauncherState {
    GtkWidget *window = nullptr;
    GtkWidget *notebook = nullptr;
    GtkWidget *status_label = nullptr;
    GtkWidget *version_label = nullptr;
    GtkWidget *progress_bar = nullptr;
    GtkWidget *btn_launch = nullptr;
    GtkWidget *btn_update = nullptr;
    GtkWidget *combo_accounts = nullptr; 
    GtkWidget *combo_instances = nullptr; 
    GtkWidget *instances_box = nullptr;  
    GtkWidget *instance_list = nullptr;  

    std::string mods_dir;
    std::string ravex_dir;
    std::string kickx_dir;

    
    std::vector<Account> accounts;
    int active_account_index = -1;

    
    std::vector<InstanceInfo> instances;
    int active_instance_index = 0;

    
    std::atomic<pid_t> game_pid{-1};
    std::atomic<bool> game_running{false};
    std::atomic<bool> update_active{false};

    
    GtkWidget *console_window = nullptr;
    GtkWidget *console_text = nullptr;
    int console_pipe = -1;
};

} 
} 
} 

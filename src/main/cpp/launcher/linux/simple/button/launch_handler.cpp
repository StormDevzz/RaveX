#include "include/launch_handler.hpp"
#include "../network/include/mojang_api.hpp"
#include "../integr/include/fabric_launcher.hpp"
#include <vector>
#include <sys/wait.h>
#include <unistd.h>
#include <signal.h>
#include <filesystem>
#include <fstream>
#include <iterator>

namespace fs = std::filesystem;

namespace ravex {
namespace launcher {
namespace simple {
namespace button {


static std::string read_asset_index_id(const std::string& kickx_dir, const std::string& version) {
    std::string path = kickx_dir + "/versions/" + version + "/" + version + ".json";
    std::ifstream file(path);
    if (!file.is_open()) return "1.21";

    std::string content((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());
    file.close();

    size_t ai_pos = content.find("\"assetIndex\":");
    if (ai_pos == std::string::npos) return "1.21";
    size_t id_pos = content.find("\"id\":", ai_pos);
    if (id_pos == std::string::npos) return "1.21";
    size_t start = content.find("\"", id_pos + 5);
    size_t end = content.find("\"", start + 1);
    if (start == std::string::npos || end == std::string::npos) return "1.21";
    return content.substr(start + 1, end - start - 1);
}

static std::string build_classpath(LauncherState *state, const std::string& version, bool &has_fabric) {
    std::string classpath = "";
    std::string libs_dir = state->kickx_dir + "/libraries";

    has_fabric = false;
    if (fs::exists(libs_dir)) {
        for (const auto& entry : fs::recursive_directory_iterator(libs_dir)) {
            if (entry.is_regular_file() && entry.path().extension() == ".jar") {
                std::string path_str = entry.path().string();
                classpath += path_str + ":";
                if (integr::detectFabricInClasspath(path_str)) {
                    has_fabric = true;
                }
            }
        }
    }

    classpath += state->kickx_dir + "/versions/" + version + "/" + version + ".jar";

    std::string mods_dir = state->mods_dir;
    if (fs::exists(mods_dir)) {
        for (const auto& entry : fs::directory_iterator(mods_dir)) {
            if (entry.is_regular_file() && entry.path().extension() == ".jar") {
                classpath += ":" + entry.path().string();
            }
        }
    }

    return classpath;
}

pid_t launch_minecraft_direct(LauncherState *state, const std::string& version, bool &success, int *pipe_fds) {
    std::string java_path = network::detect_java_path();
    bool has_fabric = false;
    std::string classpath = build_classpath(state, version, has_fabric);
    std::string main_class;
    if (has_fabric) {
        main_class = integr::getFabricMainClass();
    } else {
        main_class = "net.minecraft.client.main.Main";
    }

    Account active_acc;
    if (state->active_account_index >= 0 && state->active_account_index < static_cast<int>(state->accounts.size())) {
        active_acc = state->accounts[state->active_account_index];
    } else {
        active_acc.username = "KickXPlayer";
        active_acc.uuid = "kickx-default-uuid";
        active_acc.token = "offline_token";
    }

    std::string inst_dir = state->kickx_dir + "/instances/default";
    std::string assets_dir = state->kickx_dir + "/assets";
    std::string natives_dir = state->ravex_dir + "/natives";

    std::vector<std::string> args = {
        java_path,
        "-Djava.library.path=" + natives_dir,
        "-cp", classpath,
        main_class,
        "--username", active_acc.username,
        "--uuid", active_acc.uuid,
        "--accessToken", active_acc.token,
        "--version", version,
        "--gameDir", inst_dir,
        "--assetsDir", assets_dir,
        "--assetIndex", read_asset_index_id(state->kickx_dir, version)
    };

    success = true;
    pid_t pid = fork();
    if (pid < 0) {
        success = false;
        return -1;
    }

    if (pid == 0) {
        setpgid(0, 0);

        if (pipe_fds) {
            close(pipe_fds[0]);
            dup2(pipe_fds[1], STDOUT_FILENO);
            dup2(pipe_fds[1], STDERR_FILENO);
            close(pipe_fds[1]);
        }

        std::vector<char*> c_args;
        for (const auto& arg : args) {
            c_args.push_back(const_cast<char*>(arg.c_str()));
        }
        c_args.push_back(nullptr);

        execvp(c_args[0], c_args.data());
        exit(1);
    }

    if (pipe_fds) {
        close(pipe_fds[1]);
    }

    return pid;
}

void monitor_game_process(LauncherState *state, pid_t pid) {
    int status;
    waitpid(pid, &status, 0);

    g_idle_add([](gpointer user_data) -> gboolean {
        LauncherState *s = static_cast<LauncherState*>(user_data);
        s->game_running = false;
        s->game_pid = -1;

        gtk_label_set_text(GTK_LABEL(s->status_label), "status: ready");
        gtk_button_set_label(GTK_BUTTON(s->btn_launch), "Launch Game");
        gtk_widget_set_sensitive(s->btn_update, TRUE);
        gtk_widget_set_sensitive(s->btn_launch, TRUE);
        return FALSE;
    }, state);
}

}
}
}
}

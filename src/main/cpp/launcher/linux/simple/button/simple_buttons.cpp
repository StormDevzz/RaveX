#include "include/simple_buttons.h"
#include "include/update_handler.h"
#include "include/launch_handler.h"
#include "../../checks/system_checks.h"
#include "../../plugins/github_utility.h"
#include "../../plugins/sys_optimize.h"
#include "../network/include/mojang_api.h"
#include "../instances/console/include/console_window.h"
#include <thread>
#include <vector>
#include <fstream>
#include <iostream>
#include <unistd.h>
#include <signal.h>
#include <cstdlib>
#include <cstring>

namespace ravex {
namespace launcher {
namespace simple {
namespace button {


static void clearOldJars(const std::string& modsDir) {
    std::string cmd = "rm -f \"" + modsDir + "/ravex\"*.jar";
    system(cmd.c_str());
}


static gboolean pulse_progressbar(gpointer user_data) {
    ProgressPulseData *data = static_cast<ProgressPulseData*>(user_data);
    if (!*(data->active)) {
        delete data;
        return FALSE;
    }
    gtk_progress_bar_pulse(GTK_PROGRESS_BAR(data->progress_bar));
    return TRUE;
}


static void download_update_thread(UpdateTaskData *data) {
    g_idle_add([](gpointer w) -> gboolean {
        gtk_label_set_text(GTK_LABEL(w), "status: downloading mod from github...");
        return FALSE;
    }, data->state->status_label);

    bool pulse_active = true;
    ProgressPulseData *pulse_data = new ProgressPulseData{data->state->progress_bar, &pulse_active};
    g_timeout_add(100, pulse_progressbar, pulse_data);

    clearOldJars(data->state->mods_dir);

    std::string destJar = data->state->mods_dir + "/ravex-" + data->remote_version + ".jar";
    bool success = ravex::launcher::plugins::GithubUtility::downloadFile(data->download_url, destJar);

    if (success) {
        std::vector<std::string> assets = {
            "font/sf_medium.ttf",
            "font/sf_bold.ttf",
            "font/comfortaa.ttf",
            "natives/libravex_jni.so"
        };
        std::string remoteAssetBase = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/src/main/resources/assets/ravex/";
        for (const auto& asset : assets) {
            std::string localPath = data->state->ravex_dir + "/" + asset;
            ravex::launcher::plugins::GithubUtility::downloadFile(remoteAssetBase + asset, localPath);
        }

        std::string versionPath = data->state->ravex_dir + "/version.txt";
        std::ofstream outVFile(versionPath);
        if (outVFile.is_open()) {
            outVFile << data->remote_version;
            outVFile.close();
        }
    }

    pulse_active = false;

    g_idle_add([](gpointer user_data) -> gboolean {
        UpdateTaskData *d = static_cast<UpdateTaskData*>(user_data);
        if (d->remote_version.empty()) {
            gtk_label_set_text(GTK_LABEL(d->state->status_label), "status: github download failed!");
            gtk_progress_bar_set_fraction(GTK_PROGRESS_BAR(d->state->progress_bar), 0.0);
        } else {
            gtk_label_set_text(GTK_LABEL(d->state->status_label), "status: mod updated!");
            std::string text = "local version: " + d->remote_version;
            gtk_label_set_text(GTK_LABEL(d->state->version_label), text.c_str());
            gtk_progress_bar_set_fraction(GTK_PROGRESS_BAR(d->state->progress_bar), 1.0);
        }
        d->state->update_active = false;
        gtk_widget_set_sensitive(d->state->btn_update, TRUE);
        gtk_widget_set_sensitive(d->state->btn_launch, TRUE);
        delete d;
        return FALSE;
    }, data);
}


static std::string get_local_version_internal(const std::string& ravexDir) {
    std::string path = ravexDir + "/version.txt";
    std::ifstream file(path);
    if (file.is_open()) {
        std::string ver;
        std::getline(file, ver);
        file.close();
        return ver;
    }
    return "none";
}


static void check_updates_thread(LauncherState *state) {
    auto release = ravex::launcher::plugins::GithubUtility::getLatestRelease();

    g_idle_add([](gpointer user_data) -> gboolean {
        UpdateTaskData *data = static_cast<UpdateTaskData*>(user_data);
        if (!data->remote_version.empty()) {
            std::string local_ver = get_local_version_internal(data->state->ravex_dir);
            bool client_ok = ravex::launcher::checks::isClientDownloaded(data->state->mods_dir, data->remote_version);

            if (local_ver != data->remote_version || !client_ok) {
                gtk_label_set_text(GTK_LABEL(data->state->status_label), "status: update available!");

                GtkWidget *dialog = gtk_message_dialog_new(GTK_WINDOW(data->state->window),
                    GTK_DIALOG_MODAL, GTK_MESSAGE_QUESTION, GTK_BUTTONS_YES_NO,
                    "A fresh update is available: %s\nDo you want to download and install it?", data->remote_version.c_str());

                int response = gtk_dialog_run(GTK_DIALOG(dialog));
                gtk_widget_destroy(dialog);

                if (response == GTK_RESPONSE_YES) {
                    std::thread(download_update_thread, data).detach();
                    return FALSE;
                }
            } else {
                gtk_label_set_text(GTK_LABEL(data->state->status_label), "status: already up-to-date!");
                GtkWidget *dialog = gtk_message_dialog_new(GTK_WINDOW(data->state->window),
                    GTK_DIALOG_DESTROY_WITH_PARENT, GTK_MESSAGE_INFO, GTK_BUTTONS_OK,
                    "Your RaveX client is fully updated!");
                gtk_dialog_run(GTK_DIALOG(dialog));
                gtk_widget_destroy(dialog);
            }
        } else {
            gtk_label_set_text(GTK_LABEL(data->state->status_label), "status: connection failed!");
            GtkWidget *dialog = gtk_message_dialog_new(GTK_WINDOW(data->state->window),
                GTK_DIALOG_DESTROY_WITH_PARENT, GTK_MESSAGE_ERROR, GTK_BUTTONS_CLOSE,
                "Could not connect to GitHub API. Please check your internet connection.");
            gtk_dialog_run(GTK_DIALOG(dialog));
            gtk_widget_destroy(dialog);
        }

        data->state->update_active = false;
        gtk_widget_set_sensitive(data->state->btn_update, TRUE);
        gtk_widget_set_sensitive(data->state->btn_launch, TRUE);
        delete data;
        return FALSE;
    }, new UpdateTaskData{state, release.tagName, release.downloadUrl});
}

void on_check_clicked(GtkWidget *widget, gpointer user_data) {
    LauncherState *state = static_cast<LauncherState*>(user_data);
    if (state->update_active || state->game_running) return;

    state->update_active = true;
    gtk_widget_set_sensitive(state->btn_update, FALSE);
    gtk_widget_set_sensitive(state->btn_launch, FALSE);
    gtk_label_set_text(GTK_LABEL(state->status_label), "status: checking for updates...");
    gtk_progress_bar_set_fraction(GTK_PROGRESS_BAR(state->progress_bar), 0.0);

    std::thread(check_updates_thread, state).detach();
}

static gboolean on_console_pipe_data(GIOChannel *channel, GIOCondition cond, gpointer user_data) {
    LauncherState *state = static_cast<LauncherState*>(user_data);
    if (cond & (G_IO_HUP | G_IO_ERR)) {
        g_io_channel_shutdown(channel, FALSE, NULL);
        g_io_channel_unref(channel);
        return FALSE;
    }
    gchar buf[4096];
    gsize count;
    GError *err = nullptr;
    GIOStatus status = g_io_channel_read_chars(channel, buf, sizeof(buf) - 1, &count, &err);
    if (status == G_IO_STATUS_NORMAL && count > 0) {
        buf[count] = '\0';
        window::append_console_log(state, buf);
    }
    return TRUE;
}

void on_launch_clicked(GtkWidget *widget, gpointer user_data) {
    LauncherState *state = static_cast<LauncherState*>(user_data);

    if (state->game_running) {
        pid_t pid = state->game_pid.load();
        if (pid > 0) {
            gtk_label_set_text(GTK_LABEL(state->status_label), "status: killing game...");
            gtk_widget_set_sensitive(state->btn_launch, FALSE);
            kill(-pid, SIGKILL);
        }
        return;
    }

    gtk_widget_set_sensitive(state->btn_update, FALSE);
    gtk_widget_set_sensitive(state->btn_launch, FALSE);
    gtk_label_set_text(GTK_LABEL(state->status_label), "status: launching game...");

    int pipe_read = -1, pipe_write = -1;
    {
        int fds[2];
        if (pipe(fds) == 0) {
            pipe_read = fds[0];
            pipe_write = fds[1];
        }
    }
    state->console_pipe = pipe_read;

    g_idle_add([](gpointer user_data) -> gboolean {
        LauncherState *s = static_cast<LauncherState*>(user_data);
        window::open_console(s);
        return FALSE;
    }, state);

    if (pipe_read >= 0) {
        GIOChannel *channel = g_io_channel_unix_new(pipe_read);
        g_io_channel_set_encoding(channel, NULL, NULL);
        g_io_channel_set_buffered(channel, FALSE);
        g_io_add_watch(channel, static_cast<GIOCondition>(G_IO_IN | G_IO_HUP | G_IO_ERR), on_console_pipe_data, state);
        g_io_channel_unref(channel);
    }

    std::thread([state, pipe_write]() {
        plugins::SysOptimize::tunePriority();
        plugins::SysOptimize::trimMemory();

        std::string version;

        {
            auto release = plugins::GithubUtility::getLatestRelease();
            if (release.success) {
                version = release.minecraftVersion;

                std::string localVer = get_local_version_internal(state->ravex_dir);
                if (localVer != release.tagName) {
                    clearOldJars(state->mods_dir);
                    std::string destJar = state->mods_dir + "/ravex-" + release.tagName + ".jar";
                    plugins::GithubUtility::downloadFile(release.downloadUrl, destJar);

                    std::vector<std::string> assets = {
                        "font/sf_medium.ttf",
                        "font/sf_bold.ttf",
                        "font/comfortaa.ttf",
                        "natives/libravex_jni.so"
                    };
                    std::string remoteAssetBase = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/src/main/resources/assets/ravex/";
                    for (const auto& asset : assets) {
                        std::string localPath = state->ravex_dir + "/" + asset;
                        plugins::GithubUtility::downloadFile(remoteAssetBase + asset, localPath);
                    }

                    std::string versionPath = state->ravex_dir + "/version.txt";
                    std::ofstream outVFile(versionPath);
                    if (outVFile.is_open()) {
                        outVFile << release.tagName;
                        outVFile.close();
                    }
                }
            }
        }

        if (version.empty()) {
            std::cerr << "[RaveX] WARNING: MC_VERSION not found in release body, defaulting to 1.21.11" << std::endl;
            std::cerr << "[RaveX] Add MC_VERSION=1.21.11 to GitHub release body to set this automatically" << std::endl;
            version = "1.21.11";
        }

        bool mojang_ok = network::download_minecraft_version(state, version);
        if (!mojang_ok) {
            g_idle_add([](gpointer user_data) -> gboolean {
                LauncherState *s = static_cast<LauncherState*>(user_data);
                window::append_console_log(s, "[launcher] ERROR: failed to download minecraft files\n");
                gtk_label_set_text(GTK_LABEL(s->status_label), "status: mojang download failed");
                gtk_widget_set_sensitive(s->btn_update, TRUE);
                gtk_widget_set_sensitive(s->btn_launch, TRUE);
                return FALSE;
            }, state);
            return;
        }

        bool launch_ok = false;
        int launch_pipe[2] = { -1, pipe_write };
        pid_t pid = launch_minecraft_direct(state, version, launch_ok, launch_pipe);

        if (launch_ok && pid > 0) {
            state->game_pid = pid;
            state->game_running = true;

            std::thread(monitor_game_process, state, pid).detach();

            g_idle_add([](gpointer user_data) -> gboolean {
                LauncherState *s = static_cast<LauncherState*>(user_data);
                window::append_console_log(s, "[launcher] game started successfully\n");
                gtk_label_set_text(GTK_LABEL(s->status_label), "status: game running");
                gtk_button_set_label(GTK_BUTTON(s->btn_launch), "Kill Game");
                gtk_widget_set_sensitive(s->btn_launch, TRUE);
                return FALSE;
            }, state);
        } else {
            g_idle_add([](gpointer user_data) -> gboolean {
                LauncherState *s = static_cast<LauncherState*>(user_data);
                window::append_console_log(s, "[launcher] ERROR: failed to launch game\n");
                gtk_label_set_text(GTK_LABEL(s->status_label), "status: launch failed");
                gtk_widget_set_sensitive(s->btn_update, TRUE);
                gtk_widget_set_sensitive(s->btn_launch, TRUE);
                return FALSE;
            }, state);
        }
    }).detach();
}

} 
} 
} 
} 

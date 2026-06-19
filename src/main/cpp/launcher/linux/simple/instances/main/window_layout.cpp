#include "include/window_layout.h"
#include "include/window_signals.h"
#include "include/widget_helpers.h"
#include "../../../checks/system_checks.h"
#include "../../button/include/simple_buttons.h"
#include "../console/include/console_window.h"
#include <fstream>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

static void on_console_clicked(GtkWidget*, gpointer data) {
    LauncherState *s = static_cast<LauncherState*>(data);
    open_console(s);
}

static std::string get_local_version(const std::string& ravexDir) {
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

void build_layout(LauncherState *state) {
    GtkWidget *vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 8);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 16);
    gtk_container_add(GTK_CONTAINER(state->window), vbox);

    // заголовок
    GtkWidget *lbl_header = gtk_label_new("KickX Launcher");
    gtk_style_context_add_class(gtk_widget_get_style_context(lbl_header), "header-label");
    gtk_box_pack_start(GTK_BOX(vbox), lbl_header, FALSE, FALSE, 0);

    GtkWidget *lbl_subtitle = gtk_label_new("minecraft launcher");
    gtk_style_context_add_class(gtk_widget_get_style_context(lbl_subtitle), "subtitle-label");
    gtk_box_pack_start(GTK_BOX(vbox), lbl_subtitle, FALSE, FALSE, 0);

    GtkWidget *sep = gtk_separator_new(GTK_ORIENTATION_HORIZONTAL);
    gtk_box_pack_start(GTK_BOX(vbox), sep, FALSE, FALSE, 5);

    // системные данные
    std::string kernel = ravex::launcher::checks::getKernelVersion();
    std::string sys_text = "system: linux " + kernel;
    GtkWidget *lbl_system = gtk_label_new(sys_text.c_str());
    gtk_widget_set_halign(lbl_system, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(vbox), lbl_system, FALSE, FALSE, 2);

    // версия
    std::string local_ver = get_local_version(state->ravex_dir);
    std::string ver_text = "version: " + local_ver;
    state->version_label = gtk_label_new(ver_text.c_str());
    gtk_widget_set_halign(state->version_label, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(vbox), state->version_label, FALSE, FALSE, 2);

    // блок аккаунтов
    GtkWidget *hbox_acc = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_box_pack_start(GTK_BOX(vbox), hbox_acc, FALSE, FALSE, 5);

    GtkWidget *lbl_acc = gtk_label_new("account:");
    gtk_box_pack_start(GTK_BOX(hbox_acc), lbl_acc, FALSE, FALSE, 0);

    state->combo_accounts = gtk_combo_box_text_new();
    gtk_box_pack_start(GTK_BOX(hbox_acc), state->combo_accounts, TRUE, TRUE, 0);

    for (const auto& acc : state->accounts) {
        std::string display_name = acc.username + (acc.is_microsoft ? " (msa)" : "");
        gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(state->combo_accounts), display_name.c_str());
    }
    if (state->active_account_index >= 0 && state->active_account_index < static_cast<int>(state->accounts.size())) {
        gtk_combo_box_set_active(GTK_COMBO_BOX(state->combo_accounts), state->active_account_index);
    }

    GtkWidget *btn_add_offline = create_styled_button("+ Offline");
    gtk_box_pack_start(GTK_BOX(hbox_acc), btn_add_offline, FALSE, FALSE, 0);

    GtkWidget *btn_add_ms = create_styled_button("+ Microsoft");
    gtk_box_pack_start(GTK_BOX(hbox_acc), btn_add_ms, FALSE, FALSE, 0);

    // блок инстанций
    GtkWidget *frame_inst = gtk_frame_new("instance");
    gtk_box_pack_start(GTK_BOX(vbox), frame_inst, FALSE, FALSE, 5);

    GtkWidget *hbox_inst = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_container_set_border_width(GTK_CONTAINER(hbox_inst), 6);
    gtk_container_add(GTK_CONTAINER(frame_inst), hbox_inst);

    GtkWidget *lbl_inst_label = gtk_label_new("default");
    gtk_style_context_add_class(gtk_widget_get_style_context(lbl_inst_label), "inst-label");
    gtk_box_pack_start(GTK_BOX(hbox_inst), lbl_inst_label, FALSE, FALSE, 0);

    GtkWidget *lbl_inst_status = gtk_label_new("[idle]");
    gtk_style_context_add_class(gtk_widget_get_style_context(lbl_inst_status), "inst-status");
    gtk_box_pack_start(GTK_BOX(hbox_inst), lbl_inst_status, FALSE, FALSE, 0);

    GtkWidget *btn_console = gtk_button_new_with_label("Console");
    gtk_widget_set_name(btn_console, "btn-console");
    gtk_box_pack_end(GTK_BOX(hbox_inst), btn_console, FALSE, FALSE, 0);

    g_signal_connect(btn_console, "clicked", G_CALLBACK(on_console_clicked), state);

    // статус
    state->status_label = gtk_label_new("status: ready");
    gtk_widget_set_halign(state->status_label, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(vbox), state->status_label, FALSE, FALSE, 2);

    // прогресс бар
    state->progress_bar = gtk_progress_bar_new();
    gtk_progress_bar_set_show_text(GTK_PROGRESS_BAR(state->progress_bar), FALSE);
    gtk_box_pack_start(GTK_BOX(vbox), state->progress_bar, FALSE, FALSE, 8);

    GtkWidget *sep2 = gtk_separator_new(GTK_ORIENTATION_HORIZONTAL);
    gtk_box_pack_start(GTK_BOX(vbox), sep2, FALSE, FALSE, 5);

    // кнопки управления
    GtkWidget *hbox_btns = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 12);
    gtk_widget_set_halign(hbox_btns, GTK_ALIGN_CENTER);
    gtk_box_pack_start(GTK_BOX(vbox), hbox_btns, FALSE, FALSE, 8);

    GtkWidget *btn_bg = create_styled_button("Change BG");
    gtk_box_pack_start(GTK_BOX(hbox_btns), btn_bg, FALSE, FALSE, 0);

    state->btn_update = gtk_button_new_with_label("Check & Update");
    gtk_box_pack_start(GTK_BOX(hbox_btns), state->btn_update, FALSE, FALSE, 0);

    state->btn_launch = gtk_button_new_with_label("Launch Game");
    gtk_style_context_add_class(gtk_widget_get_style_context(state->btn_launch), "launch-btn");
    gtk_box_pack_start(GTK_BOX(hbox_btns), state->btn_launch, FALSE, FALSE, 0);

    // привязка сигналов
    bind_signals(state, btn_add_offline, btn_add_ms, btn_bg);
}

} // namespace window
} // namespace simple
} // namespace launcher
} // namespace ravex

#include "include/window_signals.h"
#include "../../window/background/custom/include/bg_loader.h"
#include "../../acc/include/account_manager.h"
#include "../../network/include/microsoft_auth.h"
#include "../../button/include/simple_buttons.h"
#include "../../button/include/update_handler.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

static void on_account_changed(GtkComboBox *combo, gpointer user_data) {
    LauncherState *state = static_cast<LauncherState*>(user_data);
    int active = gtk_combo_box_get_active(combo);
    if (active >= 0 && active < static_cast<int>(state->accounts.size())) {
        state->active_account_index = active;
        acc::save_accounts(state);
    }
}

static void on_add_offline_clicked(GtkWidget *widget, gpointer user_data) {
    LauncherState *state = static_cast<LauncherState*>(user_data);
    
    GtkWidget *dialog = gtk_dialog_new_with_buttons("Add Offline Account",
        GTK_WINDOW(state->window),
        GTK_DIALOG_MODAL,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Add", GTK_RESPONSE_OK,
        NULL);

    GtkWidget *content_area = gtk_dialog_get_content_area(GTK_DIALOG(dialog));
    GtkWidget *vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 10);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 15);
    gtk_container_add(GTK_CONTAINER(content_area), vbox);

    GtkWidget *lbl = gtk_label_new("enter nickname:");
    gtk_box_pack_start(GTK_BOX(vbox), lbl, FALSE, FALSE, 0);

    GtkWidget *entry = gtk_entry_new();
    gtk_box_pack_start(GTK_BOX(vbox), entry, FALSE, FALSE, 0);

    gtk_widget_show_all(dialog);

    int res = gtk_dialog_run(GTK_DIALOG(dialog));
    if (res == GTK_RESPONSE_OK) {
        const char *text = gtk_entry_get_text(GTK_ENTRY(entry));
        std::string name(text);
        if (!name.empty()) {
            acc::add_offline_account(state, name);
            gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(state->combo_accounts), name.c_str());
            gtk_combo_box_set_active(GTK_COMBO_BOX(state->combo_accounts), state->accounts.size() - 1);
        }
    }
    gtk_widget_destroy(dialog);
}

static void on_add_microsoft_clicked(GtkWidget *widget, gpointer user_data) {
    LauncherState *state = static_cast<LauncherState*>(user_data);
    if (network::login_microsoft_account(state)) {
        std::string name = state->accounts.back().username + " (Microsoft)";
        gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(state->combo_accounts), name.c_str());
        gtk_combo_box_set_active(GTK_COMBO_BOX(state->combo_accounts), state->accounts.size() - 1);
    }
}

static void on_change_bg_clicked(GtkWidget *widget, gpointer user_data) {
    LauncherState *state = static_cast<LauncherState*>(user_data);
    change_background(state);
}

void bind_signals(LauncherState *state, GtkWidget *btn_offline, GtkWidget *btn_ms, GtkWidget *btn_bg) {
    g_signal_connect(state->combo_accounts, "changed", G_CALLBACK(on_account_changed), state);
    g_signal_connect(btn_offline, "clicked", G_CALLBACK(on_add_offline_clicked), state);
    g_signal_connect(btn_ms, "clicked", G_CALLBACK(on_add_microsoft_clicked), state);
    g_signal_connect(btn_bg, "clicked", G_CALLBACK(on_change_bg_clicked), state);
    g_signal_connect(state->btn_update, "clicked", G_CALLBACK(button::on_check_clicked), state);
    g_signal_connect(state->btn_launch, "clicked", G_CALLBACK(button::on_launch_clicked), state);
}

} // namespace window
} // namespace simple
} // namespace launcher
} // namespace ravex

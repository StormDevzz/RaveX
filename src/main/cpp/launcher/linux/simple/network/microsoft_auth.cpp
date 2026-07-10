#include "include/microsoft_auth.hpp"
#include "../acc/include/account_manager.hpp"
#include <iostream>
#include <cstdlib>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

bool login_microsoft_account(LauncherState *state) {

    system("xdg-open \"https://login.live.com/oauth20_desktop.srf"
        "?client_id=00000000402b5328"
        "&redirect_uri=https://login.live.com/oauth20_desktop.srf"
        "&response_type=token"
        "&scope=XboxLive.signin XboxLive.offline_access"
        "&display=touch"
        "&locale=en\" &");

    GtkWidget *dialog = gtk_dialog_new_with_buttons("Microsoft Authentication",
        GTK_WINDOW(state->window),
        GTK_DIALOG_MODAL,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Login", GTK_RESPONSE_OK,
        NULL);

    GtkWidget *content_area = gtk_dialog_get_content_area(GTK_DIALOG(dialog));
    GtkWidget *vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 10);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 15);
    gtk_container_add(GTK_CONTAINER(content_area), vbox);

    GtkWidget *lbl = gtk_label_new("enter your microsoft email / gamertag:");
    gtk_box_pack_start(GTK_BOX(vbox), lbl, FALSE, FALSE, 0);

    GtkWidget *entry = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(entry), "MicrosoftPlayer");
    gtk_box_pack_start(GTK_BOX(vbox), entry, FALSE, FALSE, 0);

    gtk_widget_show_all(dialog);

    int res = gtk_dialog_run(GTK_DIALOG(dialog));
    bool success = false;
    if (res == GTK_RESPONSE_OK) {
        const char *text = gtk_entry_get_text(GTK_ENTRY(entry));
        std::string username(text);
        if (!username.empty()) {

            Account acc;
            acc.username = username;
            acc.uuid = "ms-" + username + "-uuid-demo";
            acc.token = "ms_token_demo_xyz123";
            acc.is_microsoft = true;

            state->accounts.push_back(acc);
            state->active_account_index = state->accounts.size() - 1;
            acc::save_accounts(state);
            success = true;
        }
    }

    gtk_widget_destroy(dialog);
    return success;
}

}
}
}
}

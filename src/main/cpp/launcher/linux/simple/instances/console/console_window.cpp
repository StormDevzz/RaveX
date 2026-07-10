#include "include/console_window.hpp"
#include <gtk/gtk.h>
#include <signal.h>
#include <unistd.h>
#include <cstring>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {

static void on_kill_game(GtkWidget*, gpointer data) {
    LauncherState *state = static_cast<LauncherState*>(data);
    if (state->game_running) {
        pid_t pid = state->game_pid.load();
        if (pid > 0) {
            kill(-pid, SIGKILL);
            append_console_log(state, "[console] game killed\n");
            state->game_running = false;
            state->game_pid = -1;
        }
    }
}

static void on_copy_all(GtkWidget*, gpointer data) {
    LauncherState *state = static_cast<LauncherState*>(data);
    if (!state->console_text) return;

    GtkTextBuffer *buffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(state->console_text));
    GtkTextIter start, end;
    gtk_text_buffer_get_start_iter(buffer, &start);
    gtk_text_buffer_get_end_iter(buffer, &end);

    char *text = gtk_text_buffer_get_text(buffer, &start, &end, FALSE);
    if (text) {
        gtk_clipboard_set_text(gtk_clipboard_get(GDK_SELECTION_CLIPBOARD), text, -1);
        g_free(text);
    }
}

void open_console(LauncherState *state) {
    if (state->console_window) {
        gtk_window_present(GTK_WINDOW(state->console_window));
        return;
    }

    state->console_window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(state->console_window), "Game Console");
    gtk_window_set_default_size(GTK_WINDOW(state->console_window), 800, 500);
    gtk_window_set_transient_for(GTK_WINDOW(state->console_window), GTK_WINDOW(state->window));
    gtk_window_set_destroy_with_parent(GTK_WINDOW(state->console_window), TRUE);

    GtkWidget *vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 5);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 10);
    gtk_container_add(GTK_CONTAINER(state->console_window), vbox);

    GtkWidget *scrolled = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolled),
        GTK_POLICY_AUTOMATIC, GTK_POLICY_AUTOMATIC);
    gtk_box_pack_start(GTK_BOX(vbox), scrolled, TRUE, TRUE, 0);

    state->console_text = gtk_text_view_new();
    gtk_text_view_set_editable(GTK_TEXT_VIEW(state->console_text), FALSE);
    gtk_text_view_set_cursor_visible(GTK_TEXT_VIEW(state->console_text), TRUE);
    gtk_text_view_set_wrap_mode(GTK_TEXT_VIEW(state->console_text), GTK_WRAP_WORD_CHAR);

    GtkCssProvider *css = gtk_css_provider_new();
    gtk_css_provider_load_from_data(css, "textview { font-family: monospace; font-size: 10pt; }", -1, NULL);
    GtkStyleContext *ctx = gtk_widget_get_style_context(state->console_text);
    gtk_style_context_add_provider(ctx, GTK_STYLE_PROVIDER(css), GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
    g_object_unref(css);

    gtk_container_add(GTK_CONTAINER(scrolled), state->console_text);

    GtkWidget *hbox = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 10);
    gtk_box_pack_start(GTK_BOX(vbox), hbox, FALSE, FALSE, 0);

    GtkWidget *btn_kill = gtk_button_new_with_label("Kill Game");
    gtk_widget_set_name(btn_kill, "btn-kill");
    g_signal_connect(btn_kill, "clicked", G_CALLBACK(on_kill_game), state);
    gtk_box_pack_start(GTK_BOX(hbox), btn_kill, FALSE, FALSE, 0);

    GtkWidget *btn_copy = gtk_button_new_with_label("Copy All");
    gtk_widget_set_name(btn_copy, "btn-copy");
    g_signal_connect(btn_copy, "clicked", G_CALLBACK(on_copy_all), state);
    gtk_box_pack_start(GTK_BOX(hbox), btn_copy, FALSE, FALSE, 0);

    GtkWidget *btn_close = gtk_button_new_with_label("Close Console");
    g_signal_connect_swapped(btn_close, "clicked", G_CALLBACK(gtk_widget_destroy), state->console_window);
    gtk_box_pack_end(GTK_BOX(hbox), btn_close, FALSE, FALSE, 0);

    g_signal_connect(state->console_window, "destroy", G_CALLBACK(gtk_widget_destroyed), &state->console_window);

    gtk_widget_show_all(state->console_window);
}

void close_console(LauncherState *state) {
    if (state->console_window) {
        gtk_widget_destroy(state->console_window);
    }
}

bool is_console_open(LauncherState *state) {
    return state->console_window != nullptr;
}

void append_console_log(LauncherState *state, const char *text) {
    if (!state->console_text) return;

    GtkTextBuffer *buffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(state->console_text));
    GtkTextIter iter;
    gtk_text_buffer_get_end_iter(buffer, &iter);
    gtk_text_buffer_insert(buffer, &iter, text, -1);

    GtkTextMark *mark = gtk_text_buffer_get_insert(buffer);
    gtk_text_view_scroll_to_mark(GTK_TEXT_VIEW(state->console_text), mark, 0.0, FALSE, 0.0, 0.0);
}

}
}
}
}

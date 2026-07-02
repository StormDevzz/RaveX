#include "include/instance_editor.hpp"
#include "../../state/include/launcher_state.hpp"
#include <gtk/gtk.h>
#include <cstdlib>
#include <sys/stat.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace instance {

struct EditorWidgets {
    GtkWidget* entry_name;
    GtkWidget* entry_ver;
    GtkWidget* spin_ram;
    GtkWidget* entry_icon;
    GtkWidget* icon_preview;
    GtkWidget* preview_box;
};

static void on_browse_icon(GtkWidget* btn, gpointer user_data) {
    auto* ew = static_cast<EditorWidgets*>(user_data);
    GtkWindow* parent = GTK_WINDOW(gtk_widget_get_toplevel(btn));

    GtkWidget* dialog = gtk_file_chooser_dialog_new("Select Instance Icon",
        parent, GTK_FILE_CHOOSER_ACTION_OPEN,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Open", GTK_RESPONSE_ACCEPT, NULL);

    GtkFileFilter* filter = gtk_file_filter_new();
    gtk_file_filter_set_name(filter, "Images (PNG, JPG, SVG)");
    gtk_file_filter_add_mime_type(filter, "image/png");
    gtk_file_filter_add_mime_type(filter, "image/jpeg");
    gtk_file_filter_add_mime_type(filter, "image/svg+xml");
    gtk_file_chooser_add_filter(GTK_FILE_CHOOSER(dialog), filter);

    if (gtk_dialog_run(GTK_DIALOG(dialog)) == GTK_RESPONSE_ACCEPT) {
        char* path = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(dialog));
        if (path) {
            gtk_entry_set_text(GTK_ENTRY(ew->entry_icon), path);

            if (ew->icon_preview) {
                gtk_container_remove(GTK_CONTAINER(ew->preview_box), ew->icon_preview);
            }
            ew->icon_preview = gtk_image_new_from_file(path);
            if (ew->icon_preview) {
                gtk_widget_set_size_request(ew->icon_preview, 48, 48);
                gtk_box_pack_start(GTK_BOX(ew->preview_box), ew->icon_preview, FALSE, FALSE, 0);
                gtk_widget_show(ew->icon_preview);
            }
            g_free(path);
        }
    }
    gtk_widget_destroy(dialog);
}

bool show_instance_editor(GtkWindow* parent, InstanceInfo& info, bool createMode) {
    GtkWidget* dialog = gtk_dialog_new_with_buttons(
        createMode ? "New Instance" : "Edit Instance",
        parent,
        GTK_DIALOG_MODAL,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Save", GTK_RESPONSE_ACCEPT, NULL);

    EditorWidgets* ew = new EditorWidgets();
    GtkWidget* content = gtk_dialog_get_content_area(GTK_DIALOG(dialog));
    GtkWidget* grid = gtk_grid_new();
    gtk_grid_set_row_spacing(GTK_GRID(grid), 8);
    gtk_grid_set_column_spacing(GTK_GRID(grid), 12);
    gtk_container_set_border_width(GTK_CONTAINER(grid), 16);
    gtk_container_add(GTK_CONTAINER(content), grid);

    int r = 0;

    gtk_grid_attach(GTK_GRID(grid), gtk_label_new("Name:"), 0, r, 1, 1);
    ew->entry_name = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(ew->entry_name), info.name.c_str());
    if (createMode) gtk_entry_set_placeholder_text(GTK_ENTRY(ew->entry_name), "my instance");
    gtk_grid_attach(GTK_GRID(grid), ew->entry_name, 1, r, 2, 1);
    r++;

    gtk_grid_attach(GTK_GRID(grid), gtk_label_new("MC Version:"), 0, r, 1, 1);
    ew->entry_ver = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(ew->entry_ver), info.mc_version.c_str());
    gtk_grid_attach(GTK_GRID(grid), ew->entry_ver, 1, r, 2, 1);
    r++;

    gtk_grid_attach(GTK_GRID(grid), gtk_label_new("RAM (MB):"), 0, r, 1, 1);
    ew->spin_ram = gtk_spin_button_new_with_range(1024, 32768, 512);
    gtk_spin_button_set_value(GTK_SPIN_BUTTON(ew->spin_ram), info.ram_mb);
    gtk_grid_attach(GTK_GRID(grid), ew->spin_ram, 1, r, 2, 1);
    r++;

    gtk_grid_attach(GTK_GRID(grid), gtk_label_new("Icon:"), 0, r, 1, 1);
    ew->entry_icon = gtk_entry_new();
    gtk_entry_set_text(GTK_ENTRY(ew->entry_icon), info.icon_path.c_str());
    gtk_grid_attach(GTK_GRID(grid), ew->entry_icon, 1, r, 1, 1);

    GtkWidget* btn_browse = gtk_button_new_with_label("Browse...");
    gtk_grid_attach(GTK_GRID(grid), btn_browse, 2, r, 1, 1);

    ew->preview_box = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 4);
    ew->icon_preview = nullptr;
    if (!info.icon_path.empty()) {
        ew->icon_preview = gtk_image_new_from_file(info.icon_path.c_str());
        if (ew->icon_preview) {
            gtk_widget_set_size_request(ew->icon_preview, 48, 48);
            gtk_box_pack_start(GTK_BOX(ew->preview_box), ew->icon_preview, FALSE, FALSE, 0);
        }
    }
    gtk_grid_attach(GTK_GRID(grid), ew->preview_box, 3, r, 1, 1);
    r++;

    g_signal_connect(btn_browse, "clicked", G_CALLBACK(on_browse_icon), ew);

    gtk_widget_show_all(dialog);

    int resp = gtk_dialog_run(GTK_DIALOG(dialog));
    bool saved = false;
    if (resp == GTK_RESPONSE_ACCEPT) {
        info.name = gtk_entry_get_text(GTK_ENTRY(ew->entry_name));
        info.mc_version = gtk_entry_get_text(GTK_ENTRY(ew->entry_ver));
        info.ram_mb = gtk_spin_button_get_value_as_int(GTK_SPIN_BUTTON(ew->spin_ram));
        info.icon_path = gtk_entry_get_text(GTK_ENTRY(ew->entry_icon));
        saved = true;
    }

    gtk_widget_destroy(dialog);
    delete ew;
    return saved;
}

} 
} 
} 
} 

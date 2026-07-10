#include "include/window_layout.hpp"
#include "include/window_signals.hpp"
#include "include/widget_helpers.hpp"
#include "../../../checks/include/system_checks.hpp"
#include "../../instances/editor/include/instance_editor.hpp"
#include "../../instances/manager/include/instance_manager.hpp"
#include "../../button/include/simple_buttons.hpp"
#include "../console/include/console_window.hpp"
#include <fstream>
#include <cstdlib>

namespace ravex {
namespace launcher {
namespace simple {
namespace window {


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

static void on_console_clicked(GtkWidget*, gpointer data) {
    LauncherState* s = static_cast<LauncherState*>(data);
    open_console(s);
}


static const char* INSTANCE_TARGET = "ravex-instance-idx";


static void rebuild_instances(LauncherState* state);


static GtkWidget* create_instance_card(LauncherState* state, int idx) {
    auto& inst = state->instances[idx];

    GtkWidget* card = gtk_event_box_new();
    gtk_widget_set_size_request(card, 160, 140);
    gtk_style_context_add_class(gtk_widget_get_style_context(card), "inst-card");

    GtkWidget* vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 4);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 8);
    gtk_container_add(GTK_CONTAINER(card), vbox);


    GtkWidget* icon_img = nullptr;
    if (!inst.icon_path.empty()) {
        icon_img = gtk_image_new_from_file(inst.icon_path.c_str());
    }
    if (!icon_img) {
        icon_img = gtk_image_new_from_icon_name("application-x-executable", GTK_ICON_SIZE_DIALOG);
    }
    gtk_widget_set_size_request(icon_img, 48, 48);
    gtk_box_pack_start(GTK_BOX(vbox), icon_img, FALSE, FALSE, 0);


    GtkWidget* lbl_name = gtk_label_new(inst.name.c_str());
    gtk_label_set_ellipsize(GTK_LABEL(lbl_name), PANGO_ELLIPSIZE_END);
    gtk_box_pack_start(GTK_BOX(vbox), lbl_name, FALSE, FALSE, 0);


    std::string ver_text = "MC " + inst.mc_version;
    GtkWidget* lbl_ver = gtk_label_new(ver_text.c_str());
    gtk_style_context_add_class(gtk_widget_get_style_context(lbl_ver), "inst-version");
    gtk_box_pack_start(GTK_BOX(vbox), lbl_ver, FALSE, FALSE, 0);


    GtkWidget* hbox_btns = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 4);
    gtk_box_set_homogeneous(GTK_BOX(hbox_btns), TRUE);


    g_object_set_data(G_OBJECT(card), "inst-idx", GINT_TO_POINTER(idx));


    gtk_drag_source_set(card, GDK_BUTTON1_MASK, nullptr, 0, GDK_ACTION_MOVE);
    gtk_drag_source_add_text_targets(card);
    g_signal_connect(card, "drag-data-get", G_CALLBACK(+[](GtkWidget* w, GdkDragContext*, GtkSelectionData* sel, guint, guint, gpointer) {
        int idx = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(w), "inst-idx"));
        std::string s = std::to_string(idx);
        gtk_selection_data_set_text(sel, s.c_str(), -1);
    }), nullptr);
    g_signal_connect(card, "drag-begin", G_CALLBACK(+[](GtkWidget* w, GdkDragContext*, gpointer) {
        gtk_widget_set_opacity(w, 0.5);
    }), nullptr);
    g_signal_connect(card, "drag-end", G_CALLBACK(+[](GtkWidget* w, GdkDragContext*, gpointer) {
        gtk_widget_set_opacity(w, 1.0);
    }), nullptr);


    GtkWidget* btn_edit = gtk_button_new_with_label("Edit");
    gtk_widget_set_name(btn_edit, "btn-card-edit");
    g_object_set_data(G_OBJECT(btn_edit), "inst-idx", GINT_TO_POINTER(idx));
    g_signal_connect(btn_edit, "clicked", G_CALLBACK(+[](GtkWidget* btn, gpointer user_data) {
        LauncherState* st = static_cast<LauncherState*>(user_data);
        int i = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(btn), "inst-idx"));
        auto& inst = st->instances[i];
        std::string oldName = inst.name;
        if (instance::show_instance_editor(GTK_WINDOW(st->window), inst, false)) {
            instance::save_instance(st->kickx_dir, inst);
            if (inst.name != oldName) {
                std::string oldDir = st->kickx_dir + "/instances/" + oldName;
                std::string newDir = st->kickx_dir + "/instances/" + inst.name;
                rename(oldDir.c_str(), newDir.c_str());
                inst.dir = newDir;
            }
            rebuild_instances(st);
            gtk_combo_box_text_remove_all(GTK_COMBO_BOX_TEXT(st->combo_instances));
            for (auto& i : st->instances)
                gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(st->combo_instances), i.name.c_str());
            gtk_combo_box_set_active(GTK_COMBO_BOX(st->combo_instances), st->active_instance_index);
        }
    }), state);
    gtk_box_pack_start(GTK_BOX(hbox_btns), btn_edit, TRUE, TRUE, 0);


    GtkWidget* btn_del = gtk_button_new_with_label("X");
    gtk_widget_set_name(btn_del, "btn-card-del");
    g_object_set_data(G_OBJECT(btn_del), "inst-idx", GINT_TO_POINTER(idx));
    g_signal_connect(btn_del, "clicked", G_CALLBACK(+[](GtkWidget* btn, gpointer user_data) {
        LauncherState* st = static_cast<LauncherState*>(user_data);
        int i = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(btn), "inst-idx"));
        std::string name = st->instances[i].name;
        if (st->instances.size() <= 1) return;
        instance::delete_instance(st->kickx_dir, name);
        st->instances.erase(st->instances.begin() + i);
        if (st->active_instance_index >= (int)st->instances.size())
            st->active_instance_index = (int)st->instances.size() - 1;
        rebuild_instances(st);
    }), state);
    gtk_box_pack_start(GTK_BOX(hbox_btns), btn_del, TRUE, TRUE, 0);

    gtk_box_pack_start(GTK_BOX(vbox), hbox_btns, FALSE, FALSE, 0);

    gtk_widget_show_all(card);
    return card;
}


static void rebuild_instances(LauncherState* state) {
    if (!state->instance_list) return;

    GList* children = gtk_container_get_children(GTK_CONTAINER(state->instance_list));
    for (GList* iter = children; iter != nullptr; iter = iter->next) {
        gtk_container_remove(GTK_CONTAINER(state->instance_list), GTK_WIDGET(iter->data));
    }
    g_list_free(children);
    for (size_t i = 0; i < state->instances.size(); i++) {
        GtkWidget* card = create_instance_card(state, i);
        gtk_container_add(GTK_CONTAINER(state->instance_list), card);
    }
    gtk_widget_show_all(state->instance_list);
}


static GtkWidget* build_instance_page(LauncherState* state) {
    GtkWidget* vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 8);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 12);


    GtkWidget* scroll = gtk_scrolled_window_new(nullptr, nullptr);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scroll),
        GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
    gtk_widget_set_vexpand(scroll, TRUE);

    GtkWidget* flow = gtk_flow_box_new();
    gtk_flow_box_set_selection_mode(GTK_FLOW_BOX(flow), GTK_SELECTION_NONE);
    gtk_flow_box_set_min_children_per_line(GTK_FLOW_BOX(flow), 2);
    gtk_flow_box_set_max_children_per_line(GTK_FLOW_BOX(flow), 6);
    gtk_flow_box_set_activate_on_single_click(GTK_FLOW_BOX(flow), FALSE);
    gtk_widget_set_halign(flow, GTK_ALIGN_FILL);
    gtk_container_add(GTK_CONTAINER(scroll), flow);
    gtk_box_pack_start(GTK_BOX(vbox), scroll, TRUE, TRUE, 0);

    state->instance_list = flow;


    gtk_drag_dest_set(flow, GTK_DEST_DEFAULT_ALL, nullptr, 0, GDK_ACTION_MOVE);
    gtk_drag_dest_add_text_targets(flow);

    g_signal_connect(flow, "drag-data-received", G_CALLBACK(+[](GtkWidget* flowbox,
        GdkDragContext*, gint x, gint y, GtkSelectionData* sel, guint, guint t, gpointer user_data) {
        LauncherState* st = static_cast<LauncherState*>(user_data);
        const gchar* text = (const gchar*)gtk_selection_data_get_text(sel);
        if (!text) return;
        int srcIdx = std::atoi(text);
        if (srcIdx < 0 || srcIdx >= (int)st->instances.size()) return;


        int dstIdx = -1;
        GList* flowChildren = gtk_container_get_children(GTK_CONTAINER(flowbox));
        for (GList* iter = flowChildren; iter != nullptr; iter = iter->next) {
            GtkWidget* fc = GTK_WIDGET(iter->data);
            GtkAllocation alloc;
            gtk_widget_get_allocation(fc, &alloc);
            if (x >= alloc.x && x < alloc.x + alloc.width &&
                y >= alloc.y && y < alloc.y + alloc.height) {
                GtkWidget* card = gtk_bin_get_child(GTK_BIN(fc));
                dstIdx = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(card), "inst-idx"));
                break;
            }
        }
        g_list_free(flowChildren);

        if (dstIdx < 0) {

            dstIdx = (int)st->instances.size() - 1;
        }

        if (srcIdx != dstIdx && dstIdx >= 0 && dstIdx < (int)st->instances.size()) {
            auto inst = st->instances[srcIdx];
            st->instances.erase(st->instances.begin() + srcIdx);
            st->instances.insert(st->instances.begin() + dstIdx, inst);

            if (st->active_instance_index == srcIdx) st->active_instance_index = dstIdx;
            else if (srcIdx < st->active_instance_index && dstIdx >= st->active_instance_index) st->active_instance_index--;
            else if (srcIdx > st->active_instance_index && dstIdx <= st->active_instance_index) st->active_instance_index++;

            rebuild_instances(st);
            gtk_combo_box_text_remove_all(GTK_COMBO_BOX_TEXT(st->combo_instances));
            for (auto& i : st->instances)
                gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(st->combo_instances), i.name.c_str());
            gtk_combo_box_set_active(GTK_COMBO_BOX(st->combo_instances), st->active_instance_index);
        }
    }), state);


    for (size_t i = 0; i < state->instances.size(); i++) {
        GtkWidget* card = create_instance_card(state, i);
        gtk_container_add(GTK_CONTAINER(flow), card);
    }


    GtkWidget* hbox_bottom = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_widget_set_halign(hbox_bottom, GTK_ALIGN_CENTER);

    GtkWidget* btn_add = gtk_button_new_with_label("+ Add Instance");
    g_signal_connect_swapped(btn_add, "clicked", G_CALLBACK(+[](LauncherState* st) {
        InstanceInfo newInst;
        newInst.name = "new-instance";
        newInst.mc_version = "1.21.11";
        newInst.ram_mb = 4096;
        if (instance::show_instance_editor(GTK_WINDOW(st->window), newInst, true)) {
            instance::save_instance(st->kickx_dir, newInst);
            st->instances.push_back(newInst);
            int newIdx = (int)st->instances.size() - 1;
            st->active_instance_index = newIdx;
            rebuild_instances(st);
            gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(st->combo_instances), newInst.name.c_str());
            gtk_combo_box_set_active(GTK_COMBO_BOX(st->combo_instances), newIdx);
        }
    }), state);
    gtk_box_pack_start(GTK_BOX(hbox_bottom), btn_add, FALSE, FALSE, 0);

    gtk_box_pack_start(GTK_BOX(vbox), hbox_bottom, FALSE, FALSE, 4);

    return vbox;
}


static GtkWidget* build_launcher_page(LauncherState* state) {
    GtkWidget* vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 8);
    gtk_container_set_border_width(GTK_CONTAINER(vbox), 16);


    GtkWidget* lbl_header = gtk_label_new("KickX Launcher");
    gtk_style_context_add_class(gtk_widget_get_style_context(lbl_header), "header-label");
    gtk_box_pack_start(GTK_BOX(vbox), lbl_header, FALSE, FALSE, 0);

    GtkWidget* lbl_subtitle = gtk_label_new("minecraft launcher");
    gtk_style_context_add_class(gtk_widget_get_style_context(lbl_subtitle), "subtitle-label");
    gtk_box_pack_start(GTK_BOX(vbox), lbl_subtitle, FALSE, FALSE, 0);

    gtk_box_pack_start(GTK_BOX(vbox), gtk_separator_new(GTK_ORIENTATION_HORIZONTAL), FALSE, FALSE, 5);


    std::string kernel = checks::getKernelVersion();
    GtkWidget* lbl_system = gtk_label_new(("system: linux " + kernel).c_str());
    gtk_widget_set_halign(lbl_system, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(vbox), lbl_system, FALSE, FALSE, 2);


    std::string ver_text = "version: " + get_local_version(state->ravex_dir);
    state->version_label = gtk_label_new(ver_text.c_str());
    gtk_widget_set_halign(state->version_label, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(vbox), state->version_label, FALSE, FALSE, 2);


    GtkWidget* hbox_inst = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_box_pack_start(GTK_BOX(vbox), hbox_inst, FALSE, FALSE, 5);

    gtk_box_pack_start(GTK_BOX(hbox_inst), gtk_label_new("instance:"), FALSE, FALSE, 0);

    state->combo_instances = gtk_combo_box_text_new();
    for (auto& inst : state->instances)
        gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(state->combo_instances), inst.name.c_str());
    gtk_combo_box_set_active(GTK_COMBO_BOX(state->combo_instances), state->active_instance_index);
    gtk_box_pack_start(GTK_BOX(hbox_inst), state->combo_instances, TRUE, TRUE, 0);

    g_signal_connect(state->combo_instances, "changed", G_CALLBACK(+[](GtkComboBox* box, gpointer user_data) {
        LauncherState* st = static_cast<LauncherState*>(user_data);
        int idx = gtk_combo_box_get_active(box);
        if (idx >= 0 && idx < (int)st->instances.size()) {
            st->active_instance_index = idx;
            st->mods_dir = st->instances[idx].dir + "/mods";
        }
    }), state);


    GtkWidget* hbox_acc = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_box_pack_start(GTK_BOX(vbox), hbox_acc, FALSE, FALSE, 5);

    gtk_box_pack_start(GTK_BOX(hbox_acc), gtk_label_new("account:"), FALSE, FALSE, 0);

    state->combo_accounts = gtk_combo_box_text_new();
    gtk_box_pack_start(GTK_BOX(hbox_acc), state->combo_accounts, TRUE, TRUE, 0);

    for (const auto& acc : state->accounts) {
        std::string display = acc.username + (acc.is_microsoft ? " (msa)" : "");
        gtk_combo_box_text_append_text(GTK_COMBO_BOX_TEXT(state->combo_accounts), display.c_str());
    }
    if (state->active_account_index >= 0 && state->active_account_index < (int)state->accounts.size())
        gtk_combo_box_set_active(GTK_COMBO_BOX(state->combo_accounts), state->active_account_index);

    GtkWidget* btn_add_offline = create_styled_button("+ Offline");
    gtk_box_pack_start(GTK_BOX(hbox_acc), btn_add_offline, FALSE, FALSE, 0);

    GtkWidget* btn_add_ms = create_styled_button("+ Microsoft");
    gtk_box_pack_start(GTK_BOX(hbox_acc), btn_add_ms, FALSE, FALSE, 0);


    {
        GtkWidget* frame = gtk_frame_new("instance");
        gtk_box_pack_start(GTK_BOX(vbox), frame, FALSE, FALSE, 5);

        GtkWidget* hbox = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
        gtk_container_set_border_width(GTK_CONTAINER(hbox), 6);
        gtk_container_add(GTK_CONTAINER(frame), hbox);

        GtkWidget* lbl_inst = gtk_label_new("[select instance]");
        gtk_style_context_add_class(gtk_widget_get_style_context(lbl_inst), "inst-label");
        gtk_box_pack_start(GTK_BOX(hbox), lbl_inst, FALSE, FALSE, 0);

        GtkWidget* btn_console = gtk_button_new_with_label("Console");
        gtk_widget_set_name(btn_console, "btn-console");
        gtk_box_pack_end(GTK_BOX(hbox), btn_console, FALSE, FALSE, 0);
        g_signal_connect(btn_console, "clicked", G_CALLBACK(on_console_clicked), state);


        g_signal_connect(state->combo_instances, "changed", G_CALLBACK(+[](GtkComboBox* box, gpointer user_data) {
            LauncherState* st = static_cast<LauncherState*>(user_data);
            GtkWidget* container = static_cast<GtkWidget*>(g_object_get_data(G_OBJECT(box), "frame-container"));
            if (!container) return;
            int idx = gtk_combo_box_get_active(box);
            GtkWidget* label = static_cast<GtkWidget*>(g_object_get_data(G_OBJECT(container), "inst-label"));
            if (!label) return;
            if (idx >= 0 && idx < (int)st->instances.size()) {
                auto& inst = st->instances[idx];
                std::string text = inst.name + " [" + inst.mc_version + "]";
                gtk_label_set_text(GTK_LABEL(label), text.c_str());
            }
        }), state);
        g_object_set_data(G_OBJECT(hbox), "inst-label", lbl_inst);
        g_object_set_data(G_OBJECT(state->combo_instances), "frame-container", hbox);
    }


    state->status_label = gtk_label_new("status: ready");
    gtk_widget_set_halign(state->status_label, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(vbox), state->status_label, FALSE, FALSE, 2);


    state->progress_bar = gtk_progress_bar_new();
    gtk_progress_bar_set_show_text(GTK_PROGRESS_BAR(state->progress_bar), FALSE);
    gtk_box_pack_start(GTK_BOX(vbox), state->progress_bar, FALSE, FALSE, 8);

    gtk_box_pack_start(GTK_BOX(vbox), gtk_separator_new(GTK_ORIENTATION_HORIZONTAL), FALSE, FALSE, 5);


    GtkWidget* hbox_btns = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 12);
    gtk_widget_set_halign(hbox_btns, GTK_ALIGN_CENTER);
    gtk_box_pack_start(GTK_BOX(vbox), hbox_btns, FALSE, FALSE, 8);

    GtkWidget* btn_bg = create_styled_button("Change BG");
    gtk_box_pack_start(GTK_BOX(hbox_btns), btn_bg, FALSE, FALSE, 0);

    state->btn_update = gtk_button_new_with_label("Check & Update");
    gtk_box_pack_start(GTK_BOX(hbox_btns), state->btn_update, FALSE, FALSE, 0);

    state->btn_launch = gtk_button_new_with_label("Launch Game");
    gtk_style_context_add_class(gtk_widget_get_style_context(state->btn_launch), "launch-btn");
    gtk_box_pack_start(GTK_BOX(hbox_btns), state->btn_launch, FALSE, FALSE, 0);

    bind_signals(state, btn_add_offline, btn_add_ms, btn_bg);

    return vbox;
}


void build_layout(LauncherState* state) {

    state->notebook = gtk_notebook_new();
    gtk_container_add(GTK_CONTAINER(state->window), state->notebook);


    GtkWidget* page1 = build_launcher_page(state);
    gtk_notebook_append_page(GTK_NOTEBOOK(state->notebook), page1, gtk_label_new("Launcher"));


    GtkWidget* page2 = build_instance_page(state);
    gtk_notebook_append_page(GTK_NOTEBOOK(state->notebook), page2, gtk_label_new("Instances"));
}

}
}
}
}

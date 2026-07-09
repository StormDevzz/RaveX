#include <jni.h>
#include "desktopgui.hpp"
#include <string>
#include <vector>
#include <gtk/gtk.h>
#include <gdk/gdk.h>
#include <map>
#include <mutex>
#include <thread>
#include <sstream>
#include <cmath>

static JavaVM* cached_jvm = nullptr;
static jclass desktopgui_class = nullptr;

static JNIEnv* get_env() {
    JNIEnv* env = nullptr;
    if (cached_jvm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED) {
        cached_jvm->AttachCurrentThread((void**)&env, nullptr);
    }
    return env;
}

void notify_java_toggle(const std::string& name) {
    JNIEnv* env = get_env();
    if (!env || !desktopgui_class) return;
    jmethodID method = env->GetStaticMethodID(desktopgui_class, "toggleModuleFromNative", "(Ljava/lang/String;)V");
    if (method) {
        jstring jname = env->NewStringUTF(name.c_str());
        env->CallStaticVoidMethod(desktopgui_class, method, jname);
        env->DeleteLocalRef(jname);
    }
}

void notify_java_close() {
    JNIEnv* env = get_env();
    if (!env || !desktopgui_class) return;
    jmethodID method = env->GetStaticMethodID(desktopgui_class, "onNativeClose", "()V");
    if (method) {
        env->CallStaticVoidMethod(desktopgui_class, method);
    }
}

std::string notify_java_get_params(const std::string& name) {
    JNIEnv* env = get_env();
    if (!env || !desktopgui_class) return "";
    jmethodID method = env->GetStaticMethodID(desktopgui_class, "getModuleParams", "(Ljava/lang/String;)Ljava/lang/String;");
    if (method) {
        jstring jname = env->NewStringUTF(name.c_str());
        jstring result = (jstring)env->CallStaticObjectMethod(desktopgui_class, method, jname);
        env->DeleteLocalRef(jname);
        if (result) {
            const char* chars = env->GetStringUTFChars(result, nullptr);
            std::string out(chars);
            env->ReleaseStringUTFChars(result, chars);
            env->DeleteLocalRef(result);
            return out;
        }
    }
    return "";
}

void notify_java_set_param(const std::string& mod_name, const std::string& param_name, const std::string& value) {
    JNIEnv* env = get_env();
    if (!env || !desktopgui_class) return;
    jmethodID method = env->GetStaticMethodID(desktopgui_class, "setModuleParam", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (method) {
        jstring jmod = env->NewStringUTF(mod_name.c_str());
        jstring jpar = env->NewStringUTF(param_name.c_str());
        jstring jval = env->NewStringUTF(value.c_str());
        env->CallStaticVoidMethod(desktopgui_class, method, jmod, jpar, jval);
        env->DeleteLocalRef(jmod);
        env->DeleteLocalRef(jpar);
        env->DeleteLocalRef(jval);
    }
}

namespace {

GtkWidget* main_window = nullptr;
std::map<std::string, GtkWidget*> switch_widgets;
std::mutex gui_mutex;
void free_string(gpointer data) {
    delete static_cast<std::string*>(data);
}

void on_param_changed(GtkWidget* widget, gpointer user_data) {
    auto data = static_cast<std::pair<std::string, std::string>*>(user_data);
    if (!data) return;
    std::string& mod_name = data->first;
    std::string& param_name = data->second;

    if (GTK_IS_SWITCH(widget)) {
        bool val = gtk_switch_get_active(GTK_SWITCH(widget));
        notify_java_set_param(mod_name, param_name, val ? "true" : "false");
    } else if (GTK_IS_SCALE(widget)) {
        double val = gtk_range_get_value(GTK_RANGE(widget));
        std::ostringstream oss;
        oss << val;
        notify_java_set_param(mod_name, param_name, oss.str());
    } else if (GTK_IS_COMBO_BOX_TEXT(widget)) {
        const char* val = gtk_combo_box_text_get_active_text(GTK_COMBO_BOX_TEXT(widget));
        if (val) { notify_java_set_param(mod_name, param_name, val); }
    } else if (GTK_IS_ENTRY(widget)) {
        const char* val = gtk_entry_get_text(GTK_ENTRY(widget));
        notify_java_set_param(mod_name, param_name, val);
    } else if (GTK_IS_BUTTON(widget)) {
        notify_java_set_param(mod_name, param_name, "trigger");
    }
}

GtkWidget* build_param_row(GtkWidget* parent, const std::string& mod_name, const std::string& param_spec) {
    std::string spec = param_spec;
    std::vector<std::string> parts;
    size_t pos = 0;
    while ((pos = spec.find(':')) != std::string::npos) {
        parts.push_back(spec.substr(0, pos));
        spec.erase(0, pos + 1);
    }
    parts.push_back(spec);
    if (parts.empty()) return nullptr;
    std::string type = parts[0];

    GtkWidget* hbox = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_style_context_add_class(gtk_widget_get_style_context(hbox), "param-row");
    gtk_box_pack_start(GTK_BOX(parent), hbox, FALSE, FALSE, 4);

    auto pair_data = new std::pair<std::string, std::string>(mod_name, parts[1]);

    GtkWidget* label = gtk_label_new(parts[1].c_str());
    gtk_widget_set_halign(label, GTK_ALIGN_START);
    gtk_widget_set_size_request(label, 120, -1);
    gtk_label_set_xalign(GTK_LABEL(label), 0);
    gtk_box_pack_start(GTK_BOX(hbox), label, FALSE, FALSE, 0);

    if (type == "bool" && parts.size() >= 3) {
        GtkWidget* sw = gtk_switch_new();
        gtk_switch_set_active(GTK_SWITCH(sw), parts[2] == "true");
        g_signal_connect_data(sw, "state-set", G_CALLBACK(on_param_changed), pair_data, [](gpointer p, GClosure*) { delete static_cast<std::pair<std::string,std::string>*>(p); }, GConnectFlags(0));
        gtk_box_pack_end(GTK_BOX(hbox), sw, FALSE, FALSE, 0);
    } else if (type == "num" && parts.size() >= 6) {
        double val = std::stod(parts[2]), min = std::stod(parts[3]), max = std::stod(parts[4]), step = std::stod(parts[5]);
        GtkWidget* scale = gtk_scale_new_with_range(GTK_ORIENTATION_HORIZONTAL, min, max, step);
        gtk_range_set_value(GTK_RANGE(scale), val);
        gtk_widget_set_size_request(scale, 150, -1);
        g_signal_connect_data(scale, "value-changed", G_CALLBACK(on_param_changed), pair_data, [](gpointer p, GClosure*) { delete static_cast<std::pair<std::string,std::string>*>(p); }, GConnectFlags(0));
        gtk_box_pack_end(GTK_BOX(hbox), scale, TRUE, TRUE, 0);
    } else if (type == "mode" && parts.size() >= 4) {
        GtkWidget* combo = gtk_combo_box_text_new();
        std::string opts = parts[3];
        size_t cpos = 0;
        while ((cpos = opts.find(',')) != std::string::npos) {
            gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(combo), NULL, opts.substr(0, cpos).c_str());
            opts.erase(0, cpos + 1);
        }
        if (!opts.empty()) gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(combo), NULL, opts.c_str());
        GtkTreeModel* model = gtk_combo_box_get_model(GTK_COMBO_BOX(combo));
        GtkTreeIter iter;
        if (gtk_tree_model_get_iter_first(model, &iter)) {
            do {
                gchar* val = NULL;
                gtk_tree_model_get(model, &iter, 0, &val, -1);
                if (val && parts[2] == val) { gtk_combo_box_set_active_iter(GTK_COMBO_BOX(combo), &iter); g_free(val); break; }
                g_free(val);
            } while (gtk_tree_model_iter_next(model, &iter));
        }
        g_signal_connect_data(combo, "changed", G_CALLBACK(on_param_changed), pair_data, [](gpointer p, GClosure*) { delete static_cast<std::pair<std::string,std::string>*>(p); }, GConnectFlags(0));
        gtk_box_pack_end(GTK_BOX(hbox), combo, FALSE, FALSE, 0);
    } else if (type == "str" && parts.size() >= 3) {
        GtkWidget* entry = gtk_entry_new();
        gtk_style_context_add_class(gtk_widget_get_style_context(entry), "param-entry");
        gtk_entry_set_text(GTK_ENTRY(entry), parts[2].c_str());
        g_signal_connect_data(entry, "changed", G_CALLBACK(on_param_changed), pair_data, [](gpointer p, GClosure*) { delete static_cast<std::pair<std::string,std::string>*>(p); }, GConnectFlags(0));
        gtk_box_pack_end(GTK_BOX(hbox), entry, TRUE, TRUE, 0);
    } else if (type == "action") {
        GtkWidget* btn = gtk_button_new_with_label("\u25B6");
        gtk_style_context_add_class(gtk_widget_get_style_context(btn), "action-btn");
        g_signal_connect_data(btn, "clicked", G_CALLBACK(on_param_changed), pair_data, [](gpointer p, GClosure*) { delete static_cast<std::pair<std::string,std::string>*>(p); }, GConnectFlags(0));
        gtk_box_pack_end(GTK_BOX(hbox), btn, FALSE, FALSE, 0);
    } else {
        delete pair_data;
    }
    return hbox;
}

void on_gear_clicked(GtkWidget* button, gpointer user_data) {
    auto mod_name = static_cast<std::string*>(user_data);
    if (!mod_name) return;
    std::string params = notify_java_get_params(*mod_name);
    if (params.empty()) return;

    GtkWidget* dialog = gtk_dialog_new_with_buttons(
        mod_name->c_str(), GTK_WINDOW(main_window),
        GTK_DIALOG_DESTROY_WITH_PARENT, NULL, GTK_RESPONSE_CLOSE, NULL);
    gtk_window_set_default_size(GTK_WINDOW(dialog), 380, 420);
    gtk_window_set_position(GTK_WINDOW(dialog), GTK_WIN_POS_CENTER_ON_PARENT);

    GtkWidget* content = gtk_dialog_get_content_area(GTK_DIALOG(dialog));
    gtk_style_context_add_class(gtk_widget_get_style_context(content), "dialog-bg");
    gtk_container_set_border_width(GTK_CONTAINER(content), 0);

    GtkWidget* header = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
    gtk_container_set_border_width(GTK_CONTAINER(header), 14);
    gtk_widget_set_margin_bottom(header, 4);
    gtk_box_pack_start(GTK_BOX(content), header, FALSE, FALSE, 0);

    GtkWidget* hlabel = gtk_label_new(mod_name->c_str());
    gtk_style_context_add_class(gtk_widget_get_style_context(hlabel), "dialog-title");
    gtk_box_pack_start(GTK_BOX(header), hlabel, FALSE, FALSE, 0);

    GtkWidget* scroll = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scroll), GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
    gtk_box_pack_start(GTK_BOX(content), scroll, TRUE, TRUE, 0);

    GtkWidget* box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 2);
    gtk_container_set_border_width(GTK_CONTAINER(box), 12);
    gtk_container_add(GTK_CONTAINER(scroll), box);

    std::string remaining = params;
    size_t pp = 0;
    while ((pp = remaining.find('|')) != std::string::npos) {
        build_param_row(box, *mod_name, remaining.substr(0, pp));
        remaining.erase(0, pp + 1);
    }
    if (!remaining.empty()) build_param_row(box, *mod_name, remaining);

    gtk_widget_show_all(dialog);
    g_signal_connect(dialog, "response", G_CALLBACK(gtk_widget_destroy), NULL);
}

gboolean on_switch_toggled(GtkSwitch* widget, gboolean state, gpointer user_data) {
    auto name_ptr = static_cast<std::string*>(g_object_get_data(G_OBJECT(widget), "mod_name"));
    if (name_ptr) {
        notify_java_toggle(*name_ptr);
    }
    return FALSE;
}

void on_window_destroy(GtkWidget* widget, gpointer user_data) {
    { std::lock_guard<std::mutex> lock(gui_mutex); main_window = NULL; switch_widgets.clear(); }
    gtk_main_quit();
    notify_java_close();
}

void apply_css() {
    GtkCssProvider* provider = gtk_css_provider_new();
    gtk_css_provider_load_from_data(provider,
        "window { background: #0a0a0a; }"
        "window decoration { background: transparent; border: 1px solid #00ff00; }"
        "label { font-family: 'Courier New', 'Liberation Mono', 'monospace'; color: #00ff00; font-size: 12px; }"
        ".win-title { font-size: 16px; font-weight: bold; color: #00ff00; letter-spacing: 2px; }"
        ".row-box { background: #0d0d0d; padding: 6px 10px; margin-bottom: 2px; border: 1px solid #00ff00; }"
        ".row-box:hover { background: #111111; border-color: #33ff33; }"
        ".row-box label { color: #00ff00; font-weight: normal; }"
        ".gear-btn { background: #0a0a0a; border: 1px solid #00ff00; color: #00ff00; font-size: 14px; padding: 0 6px; min-width: 0; }"
        ".gear-btn:hover { color: #00ff00; background: #003300; }"
        "switch { outline: none; margin: 0; }"
        "switch slider { background: #003300; border: 1px solid #00ff00; min-width: 32px; min-height: 16px; }"
        "switch:checked slider { background: #00ff00; }"
        ".dialog-bg { background: #0a0a0a; }"
        ".dialog-title { font-size: 14px; font-weight: bold; color: #00ff00; }"
        ".param-row { padding: 4px 0; }"
        ".param-row label { color: #00ff00; font-size: 11px; }"
        "scale trough { background: #003300; border: 1px solid #00ff00; min-height: 4px; }"
        "scale highlight { background: #00ff00; }"
        "scale slider { background: #00ff00; min-width: 12px; min-height: 12px; margin: -4px; }"
        "combobox { background: #0d0d0d; border: 1px solid #00ff00; padding: 2px 4px; color: #00ff00; }"
        "combobox:hover { border-color: #33ff33; }"
        "combobox window { background: #0a0a0a; }"
        ".param-entry { background: #0d0d0d; border: 1px solid #00ff00; color: #00ff00; padding: 3px 6px; }"
        ".param-entry:focus { border-color: #33ff33; }"
        ".action-btn { background: #003300; border: 1px solid #00ff00; color: #00ff00; padding: 3px 10px; min-width: 0; }"
        ".action-btn:hover { background: #005500; }"
        "scrollbar { background: #0a0a0a; }"
        "scrollbar trough { background: #0d0d0d; border: none; }"
        "scrollbar slider { background: #003300; border: 1px solid #00ff00; min-width: 8px; }",
        -1, NULL);

    GdkScreen* screen = gdk_screen_get_default();
    if (screen) {
        gtk_style_context_add_provider_for_screen(screen,
            GTK_STYLE_PROVIDER(provider), GTK_STYLE_PROVIDER_PRIORITY_USER);
    }
    g_object_unref(provider);
}

void gtk_thread_func(std::vector<ravex::ModuleGuiData> modules) {
    if (!gtk_init_check(NULL, NULL)) return;

    main_window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(main_window), "RaveX");
    gtk_window_set_default_size(GTK_WINDOW(main_window), 420, 640);
    gtk_window_set_position(GTK_WINDOW(main_window), GTK_WIN_POS_CENTER);

    GdkScreen* screen = gdk_screen_get_default();
    if (screen) {
        GdkVisual* visual = gdk_screen_get_rgba_visual(screen);
        if (visual) {
            gtk_widget_set_visual(main_window, visual);
        }
    }
    gtk_widget_set_app_paintable(main_window, TRUE);

    g_signal_connect(main_window, "destroy", G_CALLBACK(on_window_destroy), NULL);
    apply_css();

    GtkWidget* main_box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
    gtk_container_set_border_width(GTK_CONTAINER(main_box), 0);
    gtk_container_add(GTK_CONTAINER(main_window), main_box);

    GtkWidget* header = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
    gtk_widget_set_margin_start(header, 16);
    gtk_widget_set_margin_end(header, 16);
    gtk_widget_set_margin_top(header, 16);
    gtk_widget_set_margin_bottom(header, 8);
    gtk_box_pack_start(GTK_BOX(main_box), header, FALSE, FALSE, 0);

    GtkWidget* title_label = gtk_label_new("RaveX v1.4.7");
    gtk_style_context_add_class(gtk_widget_get_style_context(title_label), "win-title");
    gtk_box_pack_start(GTK_BOX(header), title_label, FALSE, FALSE, 0);

    char subtitle[64];
    snprintf(subtitle, sizeof(subtitle), "[%zu]", modules.size());
    GtkWidget* sub_label = gtk_label_new(subtitle);
    gtk_widget_set_halign(sub_label, GTK_ALIGN_END);
    gtk_box_pack_end(GTK_BOX(header), sub_label, FALSE, FALSE, 0);

    GtkWidget* scroll = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scroll), GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
    gtk_box_pack_start(GTK_BOX(main_box), scroll, TRUE, TRUE, 0);

    GtkWidget* list_box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
    gtk_container_set_border_width(GTK_CONTAINER(list_box), 10);
    gtk_widget_set_margin_start(list_box, 6);
    gtk_widget_set_margin_end(list_box, 6);
    gtk_container_add(GTK_CONTAINER(scroll), list_box);

    {
        std::lock_guard<std::mutex> lock(gui_mutex);
        switch_widgets.clear();
        for (const auto& mod : modules) {
            GtkWidget* row_box = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 8);
            gtk_style_context_add_class(gtk_widget_get_style_context(row_box), "row-box");

            GtkWidget* label = gtk_label_new(mod.name.c_str());
            gtk_widget_set_halign(label, GTK_ALIGN_START);
            gtk_box_pack_start(GTK_BOX(row_box), label, TRUE, TRUE, 0);

            auto gear_name = new std::string(mod.name);
            GtkWidget* gear = gtk_button_new_with_label("\u2699");
            gtk_style_context_add_class(gtk_widget_get_style_context(gear), "gear-btn");
            g_signal_connect_data(gear, "clicked", G_CALLBACK(on_gear_clicked), gear_name,
                [](gpointer p, GClosure*) { delete static_cast<std::string*>(p); }, GConnectFlags(0));
            gtk_box_pack_start(GTK_BOX(row_box), gear, FALSE, FALSE, 0);

            GtkWidget* sw = gtk_switch_new();
            gtk_switch_set_active(GTK_SWITCH(sw), mod.enabled);
            gtk_widget_set_halign(sw, GTK_ALIGN_END);
            gtk_box_pack_start(GTK_BOX(row_box), sw, FALSE, FALSE, 0);

            auto name_ptr = new std::string(mod.name);
            g_object_set_data_full(G_OBJECT(sw), "mod_name", name_ptr, free_string);
            g_signal_connect(sw, "state-set", G_CALLBACK(on_switch_toggled), NULL);

            gtk_box_pack_start(GTK_BOX(list_box), row_box, FALSE, FALSE, 0);
            switch_widgets[mod.name] = sw;
        }
    }

    gtk_widget_show_all(main_window);

    g_signal_connect(main_window, "draw", G_CALLBACK(+[](GtkWidget* w, cairo_t* cr, gpointer) -> gboolean {
        int width = gtk_widget_get_allocated_width(w);
        int height = gtk_widget_get_allocated_height(w);
        cairo_set_source_rgba(cr, 0.04, 0.04, 0.04, 1);
        cairo_rectangle(cr, 0, 0, width, height);
        cairo_fill(cr);
        return FALSE;
    }), NULL);

    gtk_main();
}

struct UpdateData {
    std::string name;
    bool enabled;
};

gboolean update_switch_idle(gpointer user_data) {
    auto data = static_cast<UpdateData*>(user_data);
    if (!data) return FALSE;
    {
        std::lock_guard<std::mutex> lock(gui_mutex);
        auto it = switch_widgets.find(data->name);
        if (it != switch_widgets.end() && it->second != NULL) {
            g_signal_handlers_block_by_func(it->second, (gpointer)on_switch_toggled, NULL);
            gtk_switch_set_active(GTK_SWITCH(it->second), data->enabled);
            g_signal_handlers_unblock_by_func(it->second, (gpointer)on_switch_toggled, NULL);
        }
    }
    delete data;
    return FALSE;
}

}

void ravex::start_gui(const std::vector<ravex::ModuleGuiData>& modules) {
    std::thread(gtk_thread_func, modules).detach();
}

void ravex::update_gui_state(const std::string& name, bool enabled) {
    auto data = new UpdateData{name, enabled};
    g_idle_add(update_switch_idle, data);
}

void ravex::stop_gui() {
    std::lock_guard<std::mutex> lock(gui_mutex);
    if (main_window) {
        gtk_widget_destroy(main_window);
        main_window = NULL;
    }
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    cached_jvm = vm;
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_openDesktopGui(JNIEnv* env, jclass cls, jobjectArray names, jbooleanArray states) {
    if (desktopgui_class) env->DeleteGlobalRef(desktopgui_class);
    desktopgui_class = (jclass)env->NewGlobalRef(cls);

    std::vector<ravex::ModuleGuiData> modules;
    jsize len = env->GetArrayLength(names);
    jboolean* states_buf = env->GetBooleanArrayElements(states, NULL);
    for (jsize i = 0; i < len; i++) {
        jstring jname = (jstring)env->GetObjectArrayElement(names, i);
        const char* name_chars = env->GetStringUTFChars(jname, NULL);
        modules.push_back({std::string(name_chars), states_buf[i] == JNI_TRUE});
        env->ReleaseStringUTFChars(jname, name_chars);
        env->DeleteLocalRef(jname);
    }
    env->ReleaseBooleanArrayElements(states, states_buf, JNI_ABORT);
    ravex::start_gui(modules);
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_updateModuleState(JNIEnv* env, jclass cls, jstring name, jboolean enabled) {
    const char* name_chars = env->GetStringUTFChars(name, NULL);
    ravex::update_gui_state(std::string(name_chars), enabled == JNI_TRUE);
    env->ReleaseStringUTFChars(name, name_chars);
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_closeDesktopGui(JNIEnv* env, jclass cls) {
    ravex::stop_gui();
}

}
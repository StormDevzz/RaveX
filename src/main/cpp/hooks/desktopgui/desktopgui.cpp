#include "desktopgui.h"

// Declaration of the callbacks implemented in the JNI file
extern void notify_java_toggle(const std::string& name);
extern void notify_java_close();

#ifdef _WIN32
#include <windows.h>
#include <map>
#include <mutex>
#include <thread>

namespace ravex {

static HWND hwnd_main = NULL;
static std::map<std::string, HWND> checkbox_widgets;
static std::mutex win_mutex;

LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam);

static void delete_string(void* data) {
    if (data) delete (std::string*)data;
}

void win_thread_func(std::vector<ModuleGuiData> modules) {
    HINSTANCE hinst = GetModuleHandle(NULL);
    WNDCLASSEX wc = {0};
    wc.cbSize = sizeof(WNDCLASSEX);
    wc.lpfnWndProc = wnd_proc;
    wc.hInstance = hinst;
    wc.lpszClassName = "RaveXDesktopGuiClass";
    wc.hbrBackground = CreateSolidBrush(RGB(14, 10, 26));
    
    RegisterClassEx(&wc);
    
    hwnd_main = CreateWindowEx(
        0, "RaveXDesktopGuiClass", "RaveX Desktop Dashboard",
        WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX,
        CW_USEDEFAULT, CW_USEDEFAULT, 350, 500,
        NULL, NULL, hinst, NULL
    );
    
    if (!hwnd_main) return;
    
    CreateWindow(
        "STATIC", "RaveX Premium Desktop Dashboard",
        WS_VISIBLE | WS_CHILD | SS_CENTER,
        10, 15, 310, 25,
        hwnd_main, NULL, hinst, NULL
    );
    
    int y = 50;
    std::lock_guard<std::mutex> lock(win_mutex);
    checkbox_widgets.clear();
    
    int id = 1000;
    for (const auto& mod : modules) {
        HWND hwnd_cb = CreateWindow(
            "BUTTON", mod.name.c_str(),
            WS_VISIBLE | WS_CHILD | BS_AUTOCHECKBOX,
            20, y, 290, 25,
            hwnd_main, (HMENU)(intptr_t)id, hinst, NULL
        );
        SendMessage(hwnd_cb, BM_SETCHECK, mod.enabled ? BST_CHECKED : BST_UNCHECKED, 0);
        
        checkbox_widgets[mod.name] = hwnd_cb;
        SetProp(hwnd_cb, "mod_name", (HANDLE)new std::string(mod.name));
        
        y += 30;
        id++;
    }
    
    ShowWindow(hwnd_main, SW_SHOW);
    UpdateWindow(hwnd_main);
    
    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    
    {
        std::lock_guard<std::mutex> lock(win_mutex);
        for (auto const& [name, hwnd] : checkbox_widgets) {
            std::string* ptr = (std::string*)GetProp(hwnd, "mod_name");
            if (ptr) {
                RemoveProp(hwnd, "mod_name");
                delete ptr;
            }
        }
        checkbox_widgets.clear();
        hwnd_main = NULL;
    }
    
    notify_java_close();
}

LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wparam, LPARAM lparam) {
    switch (msg) {
        case WM_COMMAND: {
            int id = LOWORD(wparam);
            HWND hwnd_cb = (HWND)lparam;
            if (hwnd_cb && id >= 1000) {
                std::string* name_ptr = (std::string*)GetProp(hwnd_cb, "mod_name");
                if (name_ptr) {
                    notify_java_toggle(*name_ptr);
                }
            }
            break;
        }
        case WM_DESTROY:
            PostQuitMessage(0);
            break;
        default:
            return DefWindowProc(hwnd, msg, wparam, lparam);
    }
    return 0;
}

void start_gui(const std::vector<ModuleGuiData>& modules) {
    std::thread(win_thread_func, modules).detach();
}

void update_gui_state(const std::string& name, bool enabled) {
    std::lock_guard<std::mutex> lock(win_mutex);
    auto it = checkbox_widgets.find(name);
    if (it != checkbox_widgets.end() && it->second != NULL) {
        SendMessage(it->second, BM_SETCHECK, enabled ? BST_CHECKED : BST_UNCHECKED, 0);
    }
}

void stop_gui() {
    std::lock_guard<std::mutex> lock(win_mutex);
    if (hwnd_main) {
        DestroyWindow(hwnd_main);
    }
}

} // namespace ravex

#else // UNIX / Linux GTK3

#include <gtk/gtk.h>
#include <map>
#include <mutex>
#include <thread>

namespace ravex {

static GtkWidget* main_window = nullptr;
static std::map<std::string, GtkWidget*> switch_widgets;
static std::mutex gui_mutex;

static void delete_string(gpointer data) {
    if (data) delete (std::string*)data;
}

static gboolean on_switch_toggled(GtkSwitch* widget, gboolean state, gpointer user_data) {
    std::string* name_ptr = (std::string*)g_object_get_data(G_OBJECT(widget), "mod_name");
    if (name_ptr) {
        notify_java_toggle(*name_ptr);
    }
    return FALSE; // Allow state change
}

static void on_window_destroy(GtkWidget* widget, gpointer user_data) {
    {
        std::lock_guard<std::mutex> lock(gui_mutex);
        main_window = nullptr;
        switch_widgets.clear();
    }
    gtk_main_quit();
    notify_java_close();
}

void gtk_thread_func(std::vector<ModuleGuiData> modules) {
    gtk_init(nullptr, nullptr);
    
    main_window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(main_window), "RaveX Premium Desktop GUI");
    gtk_window_set_default_size(GTK_WINDOW(main_window), 350, 500);
    g_signal_connect(main_window, "destroy", G_CALLBACK(on_window_destroy), nullptr);

    // Apply beautiful dark styling matching client colors
    GtkCssProvider* provider = gtk_css_provider_new();
    gtk_css_provider_load_from_data(provider,
        "window { background-color: #0e0a1a; }"
        "label { font-family: 'Inter', 'SansSerif'; color: #eeeeee; }"
        "switch { outline: none; }"
        "switch:checked slider { background-color: #da70d6; }"
        "switch slider { background-color: #4a425c; }"
        "scrolledwindow { background: transparent; }"
        "viewport { background: transparent; }"
        "box.row-box { background-color: #161026; padding: 12px; margin-bottom: 6px; border-radius: 6px; border: 1px solid #281a4a; }"
        "box.row-box:hover { background-color: #1e1534; border: 1px solid #da70d6; transition: background-color 0.2s, border-color 0.2s; }"
        "label.title-lbl { font-size: 16px; font-weight: bold; color: #da70d6; margin-bottom: 12px; }"
        , -1, nullptr);
    gtk_style_context_add_provider_for_screen(gdk_screen_get_default(),
        GTK_STYLE_PROVIDER(provider), GTK_STYLE_PROVIDER_PRIORITY_USER);

    GtkWidget* main_box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 10);
    gtk_container_set_border_width(GTK_CONTAINER(main_box), 15);
    gtk_container_add(GTK_CONTAINER(main_window), main_box);

    GtkWidget* title_label = gtk_label_new("RaveX Desktop Dashboard");
    gtk_style_context_add_class(gtk_widget_get_style_context(title_label), "title-lbl");
    gtk_box_pack_start(GTK_BOX(main_box), title_label, FALSE, FALSE, 0);

    GtkWidget* scroll = gtk_scrolled_window_new(nullptr, nullptr);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scroll), GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
    gtk_box_pack_start(GTK_BOX(main_box), scroll, TRUE, TRUE, 0);

    GtkWidget* list_box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
    gtk_container_add(GTK_CONTAINER(scroll), list_box);

    {
        std::lock_guard<std::mutex> lock(gui_mutex);
        switch_widgets.clear();

        for (const auto& mod : modules) {
            GtkWidget* row_box = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 10);
            gtk_style_context_add_class(gtk_widget_get_style_context(row_box), "row-box");

            GtkWidget* label = gtk_label_new(mod.name.c_str());
            gtk_widget_set_halign(label, GTK_ALIGN_START);
            gtk_box_pack_start(GTK_BOX(row_box), label, TRUE, TRUE, 0);

            GtkWidget* sw = gtk_switch_new();
            gtk_switch_set_active(GTK_SWITCH(sw), mod.enabled);
            gtk_widget_set_halign(sw, GTK_ALIGN_END);
            gtk_box_pack_start(GTK_BOX(row_box), sw, FALSE, FALSE, 0);

            std::string* name_ptr = new std::string(mod.name);
            g_object_set_data_full(G_OBJECT(sw), "mod_name", name_ptr, delete_string);

            g_signal_connect(sw, "state-set", G_CALLBACK(on_switch_toggled), nullptr);

            gtk_box_pack_start(GTK_BOX(list_box), row_box, FALSE, FALSE, 0);

            switch_widgets[mod.name] = sw;
        }
    }

    gtk_widget_show_all(main_window);
    gtk_main();
}

void start_gui(const std::vector<ModuleGuiData>& modules) {
    std::thread(gtk_thread_func, modules).detach();
}

struct UpdateData {
    std::string name;
    bool enabled;
};

static gboolean update_switch_state_idle(gpointer user_data) {
    UpdateData* data = (UpdateData*)user_data;
    if (data) {
        std::lock_guard<std::mutex> lock(gui_mutex);
        auto it = switch_widgets.find(data->name);
        if (it != switch_widgets.end() && it->second != nullptr) {
            g_signal_handlers_block_by_func(it->second, (gpointer)on_switch_toggled, nullptr);
            gtk_switch_set_active(GTK_SWITCH(it->second), data->enabled);
            g_signal_handlers_unblock_by_func(it->second, (gpointer)on_switch_toggled, nullptr);
        }
        delete data;
    }
    return FALSE;
}

void update_gui_state(const std::string& name, bool enabled) {
    UpdateData* data = new UpdateData{name, enabled};
    g_idle_add(update_switch_state_idle, data);
}

void stop_gui() {
    std::lock_guard<std::mutex> lock(gui_mutex);
    if (main_window) {
        gtk_widget_destroy(main_window);
        main_window = nullptr;
    }
}

} // namespace ravex

#endif // _WIN32

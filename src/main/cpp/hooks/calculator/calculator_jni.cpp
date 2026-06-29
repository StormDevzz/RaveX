#include <jni.h>
#include "calculator.h"
#include <gtk/gtk.h>
#include <string>
#include <vector>
#include <deque>
#include <mutex>
#include <thread>
#include <sstream>

static JavaVM* g_jvm = nullptr;
static jclass  g_calc_class = nullptr;

static GtkWidget* g_window      = nullptr;
static GtkWidget* g_display     = nullptr;  
static GtkWidget* g_result      = nullptr;  
static GtkWidget* g_history_box = nullptr;  
static std::string g_current_expr;
static std::deque<std::string> g_history;   
static std::mutex g_gui_mutex;

static JNIEnv* get_env() {
    JNIEnv* env = nullptr;
    if (g_jvm && g_jvm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED) {
        g_jvm->AttachCurrentThread((void**)&env, nullptr);
    }
    return env;
}

static void notify_java_close() {
    JNIEnv* env = get_env();
    if (!env || !g_calc_class) return;
    jmethodID m = env->GetStaticMethodID(g_calc_class, "onNativeClose", "()V");
    if (m) env->CallStaticVoidMethod(g_calc_class, m);
}



static void push_history(const std::string& expr, const std::string& result) {
    if (expr.empty() || result.empty()) return;
    std::string entry = expr + " = " + result;
    g_history.push_front(entry);
    if (g_history.size() > 50) g_history.pop_back();
}

static void refresh_history() {
    if (!g_history_box) return;
    
    GList* children = gtk_container_get_children(GTK_CONTAINER(g_history_box));
    for (GList* l = children; l; l = l->next)
        gtk_widget_destroy(GTK_WIDGET(l->data));
    g_list_free(children);

    for (const auto& entry : g_history) {
        GtkWidget* lbl = gtk_label_new(entry.c_str());
        gtk_widget_set_halign(lbl, GTK_ALIGN_END);
        gtk_label_set_selectable(GTK_LABEL(lbl), TRUE);
        gtk_style_context_add_class(gtk_widget_get_style_context(lbl), "hist-lbl");
        gtk_box_pack_start(GTK_BOX(g_history_box), lbl, FALSE, FALSE, 0);
    }
    gtk_widget_show_all(g_history_box);
}

static void update_display() {
    if (!g_display || !g_result) return;
    gtk_label_set_text(GTK_LABEL(g_display), g_current_expr.empty() ? "" : g_current_expr.c_str());

    if (!g_current_expr.empty()) {
        std::string preview = ravex::MathParser::evaluate(g_current_expr);
        if (preview.substr(0, 6) != "Error:") {
            gtk_label_set_markup(GTK_LABEL(g_result), ("<span alpha='60%'>" + preview + "</span>").c_str());
        } else {
            gtk_label_set_text(GTK_LABEL(g_result), "");
        }
    } else {
        gtk_label_set_text(GTK_LABEL(g_result), "");
    }
}

static void do_calculate() {
    if (g_current_expr.empty()) return;
    std::string res = ravex::MathParser::evaluate(g_current_expr);
    bool isError = res.substr(0, 6) == "Error:";

    gtk_label_set_markup(GTK_LABEL(g_result),
        isError ? ("<span color='#ff6666'>" + res + "</span>").c_str()
                : ("<span color='#da70d6'>" + res + "</span>").c_str());

    if (!isError) {
        push_history(g_current_expr, res);
        g_current_expr = res;
        gtk_label_set_text(GTK_LABEL(g_display), g_current_expr.c_str());
        gtk_label_set_text(GTK_LABEL(g_result), "");
        refresh_history();
    }
}



static void on_btn_clicked(GtkWidget* btn, gpointer data) {
    const char* label = static_cast<const char*>(data);
    std::string lstr(label);

    if (lstr == "=") {
        do_calculate();
    } else if (lstr == "C") {
        g_current_expr.clear();
        gtk_label_set_text(GTK_LABEL(g_result), "");
        update_display();
    } else if (lstr == "⌫") {
        if (!g_current_expr.empty()) {
            
            size_t pos = g_current_expr.size() - 1;
            while (pos > 0 && (g_current_expr[pos] & 0xC0) == 0x80) pos--;
            g_current_expr = g_current_expr.substr(0, pos);
        }
        update_display();
    } else if (lstr == "±") {
        if (!g_current_expr.empty()) {
            if (g_current_expr[0] == '-') g_current_expr = g_current_expr.substr(1);
            else g_current_expr = "-" + g_current_expr;
        }
        update_display();
    } else {
        g_current_expr += lstr;
        update_display();
    }
}


static gboolean on_key_press(GtkWidget* widget, GdkEventKey* event, gpointer) {
    guint key = event->keyval;
    if (key == GDK_KEY_Return || key == GDK_KEY_KP_Enter) {
        do_calculate(); return TRUE;
    }
    if (key == GDK_KEY_Escape) {
        g_current_expr.clear();
        gtk_label_set_text(GTK_LABEL(g_result), "");
        update_display(); return TRUE;
    }
    if (key == GDK_KEY_BackSpace) {
        if (!g_current_expr.empty()) g_current_expr.pop_back();
        update_display(); return TRUE;
    }
    
    if (event->string && event->string[0]) {
        char c = event->string[0];
        if ((c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-' ||
            c == '*' || c == '/' || c == '%' || c == '^' || c == '(' || c == ')') {
            g_current_expr += c;
            update_display(); return TRUE;
        }
    }
    return FALSE;
}

static void on_window_destroy(GtkWidget*, gpointer) {
    std::lock_guard<std::mutex> lock(g_gui_mutex);
    g_window = nullptr;
    gtk_main_quit();
    notify_java_close();
}



static void apply_calc_css() {
    GtkCssProvider* p = gtk_css_provider_new();
    gtk_css_provider_load_from_data(p,
        "window { background-color: #0e0a1a; }"
        "label.display-lbl { font-size: 20px; font-family: 'monospace'; color: #eeeeee; padding: 4px 8px; }"
        "label.result-lbl { font-size: 14px; font-family: 'monospace'; color: #da70d6; padding: 0 8px 4px 8px; }"
        "label.hist-lbl { font-size: 11px; color: #7a6a9c; padding: 0 4px; }"
        "label.title-lbl { font-size: 15px; font-weight: bold; color: #da70d6; padding: 8px 10px; }"
        "button.num-btn { background-image: none; background-color: #1a1040; border: 1px solid #2a1850; border-radius: 6px; color: #eeeeee; font-size: 15px; padding: 10px; min-width: 48px; min-height: 38px; }"
        "button.num-btn:hover { background-color: #2a2060; border-color: #da70d6; }"
        "button.num-btn:active { background-color: #3a2870; }"
        "button.op-btn { background-image: none; background-color: #1e0e38; border: 1px solid #3a1a5a; border-radius: 6px; color: #da70d6; font-size: 15px; padding: 10px; min-width: 48px; min-height: 38px; }"
        "button.op-btn:hover { background-color: #2e1a50; border-color: #da70d6; }"
        "button.eq-btn { background-image: none; background-color: #7a1aaa; border: 1px solid #da70d6; border-radius: 6px; color: #ffffff; font-size: 16px; font-weight: bold; padding: 10px; min-width: 48px; min-height: 38px; }"
        "button.eq-btn:hover { background-color: #9a2acc; }"
        "button.fn-btn { background-image: none; background-color: #0e0e26; border: 1px solid #1a1840; border-radius: 6px; color: #9090d0; font-size: 12px; padding: 6px 4px; min-width: 58px; min-height: 30px; }"
        "button.fn-btn:hover { background-color: #1a1840; color: #da70d6; }"
        "button.clear-btn { background-image: none; background-color: #3a0a1a; border: 1px solid #6a1a2a; border-radius: 6px; color: #ff8888; font-size: 15px; padding: 10px; min-width: 48px; min-height: 38px; }"
        "button.clear-btn:hover { background-color: #5a1a2a; }"
        "scrolledwindow { background: transparent; }"
        "viewport { background: #080614; border-radius: 4px; }",
        -1, nullptr);
    gtk_style_context_add_provider_for_screen(gdk_screen_get_default(),
        GTK_STYLE_PROVIDER(p), GTK_STYLE_PROVIDER_PRIORITY_USER);
    g_object_unref(p);
}



static const char* btn_labels_persist[64]; 
static int btn_label_count = 0;

static GtkWidget* make_btn(const char* label_str, const char* css_class, const char* data_str) {
    GtkWidget* btn = gtk_button_new_with_label(label_str);
    gtk_style_context_add_class(gtk_widget_get_style_context(btn), css_class);
    
    btn_labels_persist[btn_label_count] = data_str;
    g_signal_connect(btn, "clicked", G_CALLBACK(on_btn_clicked), (gpointer)data_str);
    btn_label_count++;
    return btn;
}



static void gtk_calc_thread() {
    if (!gtk_init_check(nullptr, nullptr)) return;
    apply_calc_css();

    g_window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(g_window), "RaveX Calculator");
    gtk_window_set_default_size(GTK_WINDOW(g_window), 400, 580);
    gtk_window_set_resizable(GTK_WINDOW(g_window), FALSE);
    g_signal_connect(g_window, "destroy", G_CALLBACK(on_window_destroy), nullptr);
    g_signal_connect(g_window, "key-press-event", G_CALLBACK(on_key_press), nullptr);

    GtkWidget* main_vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
    gtk_container_add(GTK_CONTAINER(g_window), main_vbox);

    
    GtkWidget* title = gtk_label_new("⚡ RaveX Calculator");
    gtk_style_context_add_class(gtk_widget_get_style_context(title), "title-lbl");
    gtk_widget_set_halign(title, GTK_ALIGN_START);
    gtk_box_pack_start(GTK_BOX(main_vbox), title, FALSE, FALSE, 0);

    
    GtkWidget* disp_frame = gtk_box_new(GTK_ORIENTATION_VERTICAL, 2);
    gtk_container_set_border_width(GTK_CONTAINER(disp_frame), 8);
    gtk_box_pack_start(GTK_BOX(main_vbox), disp_frame, FALSE, FALSE, 0);

    g_display = gtk_label_new("");
    gtk_style_context_add_class(gtk_widget_get_style_context(g_display), "display-lbl");
    gtk_widget_set_halign(g_display, GTK_ALIGN_END);
    gtk_label_set_ellipsize(GTK_LABEL(g_display), PANGO_ELLIPSIZE_START);
    gtk_label_set_selectable(GTK_LABEL(g_display), TRUE);
    gtk_box_pack_start(GTK_BOX(disp_frame), g_display, FALSE, FALSE, 0);

    g_result = gtk_label_new("");
    gtk_style_context_add_class(gtk_widget_get_style_context(g_result), "result-lbl");
    gtk_widget_set_halign(g_result, GTK_ALIGN_END);
    gtk_box_pack_start(GTK_BOX(disp_frame), g_result, FALSE, FALSE, 0);

    
    gtk_box_pack_start(GTK_BOX(main_vbox), gtk_separator_new(GTK_ORIENTATION_HORIZONTAL), FALSE, FALSE, 0);

    
    GtkWidget* fn_grid = gtk_grid_new();
    gtk_grid_set_column_spacing(GTK_GRID(fn_grid), 4);
    gtk_grid_set_row_spacing(GTK_GRID(fn_grid), 4);
    gtk_container_set_border_width(GTK_CONTAINER(fn_grid), 6);
    gtk_box_pack_start(GTK_BOX(main_vbox), fn_grid, FALSE, FALSE, 0);

    
    const char* fns[2][5] = {
        {"sin(", "cos(", "tan(", "sqrt(", "log("},
        {"asin(", "acos(", "atan(", "^",   "fact("}
    };
    for (int r = 0; r < 2; r++) {
        for (int c = 0; c < 5; c++) {
            GtkWidget* btn = make_btn(fns[r][c], "fn-btn", fns[r][c]);
            gtk_grid_attach(GTK_GRID(fn_grid), btn, c, r, 1, 1);
        }
    }

    
    GtkWidget* grid = gtk_grid_new();
    gtk_grid_set_column_spacing(GTK_GRID(grid), 4);
    gtk_grid_set_row_spacing(GTK_GRID(grid), 4);
    gtk_container_set_border_width(GTK_CONTAINER(grid), 8);
    gtk_box_pack_start(GTK_BOX(main_vbox), grid, FALSE, FALSE, 0);

    
    gtk_grid_attach(GTK_GRID(grid), make_btn("C",  "clear-btn", "C"),  0, 0, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("±",  "op-btn",    "±"),  1, 0, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("%",  "op-btn",    "%"),  2, 0, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("⌫",  "op-btn",    "⌫"),  3, 0, 1, 1);

    
    gtk_grid_attach(GTK_GRID(grid), make_btn("7",  "num-btn",  "7"),  0, 1, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("8",  "num-btn",  "8"),  1, 1, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("9",  "num-btn",  "9"),  2, 1, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("÷",  "op-btn",   "/"),  3, 1, 1, 1);

    
    gtk_grid_attach(GTK_GRID(grid), make_btn("4",  "num-btn",  "4"),  0, 2, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("5",  "num-btn",  "5"),  1, 2, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("6",  "num-btn",  "6"),  2, 2, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("×",  "op-btn",   "*"),  3, 2, 1, 1);

    
    gtk_grid_attach(GTK_GRID(grid), make_btn("1",  "num-btn",  "1"),  0, 3, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("2",  "num-btn",  "2"),  1, 3, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("3",  "num-btn",  "3"),  2, 3, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("−",  "op-btn",   "-"),  3, 3, 1, 1);

    
    gtk_grid_attach(GTK_GRID(grid), make_btn("0",  "num-btn",  "0"),  0, 4, 2, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn(".",  "num-btn",  "."),  2, 4, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("+",  "op-btn",   "+"),  3, 4, 1, 1);

    
    gtk_grid_attach(GTK_GRID(grid), make_btn("(",  "op-btn",  "("),  0, 5, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn(")",  "op-btn",  ")"),  1, 5, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("π",  "op-btn",  "pi"),  2, 5, 1, 1);
    gtk_grid_attach(GTK_GRID(grid), make_btn("=",  "eq-btn",  "="),  3, 5, 1, 1);

    
    gtk_box_pack_start(GTK_BOX(main_vbox), gtk_separator_new(GTK_ORIENTATION_HORIZONTAL), FALSE, FALSE, 4);

    GtkWidget* hist_label = gtk_label_new("History");
    gtk_widget_set_halign(hist_label, GTK_ALIGN_START);
    gtk_style_context_add_class(gtk_widget_get_style_context(hist_label), "hist-lbl");
    gtk_container_set_border_width(GTK_CONTAINER(hist_label), 4);
    gtk_box_pack_start(GTK_BOX(main_vbox), hist_label, FALSE, FALSE, 0);

    GtkWidget* hist_scroll = gtk_scrolled_window_new(nullptr, nullptr);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(hist_scroll), GTK_POLICY_NEVER, GTK_POLICY_AUTOMATIC);
    gtk_widget_set_size_request(hist_scroll, -1, 90);
    gtk_box_pack_start(GTK_BOX(main_vbox), hist_scroll, TRUE, TRUE, 0);

    g_history_box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 2);
    gtk_container_set_border_width(GTK_CONTAINER(g_history_box), 4);
    gtk_container_add(GTK_CONTAINER(hist_scroll), g_history_box);

    gtk_widget_show_all(g_window);
    gtk_main();
}



extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_Calculator_openCalculator(JNIEnv* env, jclass cls) {
    if (g_calc_class) env->DeleteGlobalRef(g_calc_class);
    g_calc_class = (jclass)env->NewGlobalRef(cls);
    g_current_expr.clear();
    std::thread(gtk_calc_thread).detach();
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_Calculator_closeCalculator(JNIEnv* env, jclass cls) {
    std::lock_guard<std::mutex> lock(g_gui_mutex);
    if (g_window) {
        gtk_widget_destroy(g_window);
        g_window = nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_ravex_modules_client_Calculator_nativeEvaluate(JNIEnv* env, jclass cls, jstring expr) {
    const char* e = env->GetStringUTFChars(expr, nullptr);
    std::string result = ravex::MathParser::evaluate(std::string(e));
    env->ReleaseStringUTFChars(expr, e);
    return env->NewStringUTF(result.c_str());
}

} 

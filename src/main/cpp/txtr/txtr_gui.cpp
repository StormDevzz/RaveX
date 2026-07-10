#include "txtr.hpp"
#include "include/txtr_platform.hpp"
#include <gtk/gtk.h>
#include <cstring>
#include <string>
#include <vector>
#include <functional>
#include <dirent.h>
#include <sys/stat.h>

static txtr::TextureData currentTex;
static std::string currentPath;


static GtkWidget *infoLabel, *statusLabel, *formatCombo, *qualitySpin;
static GtkWidget *batchDirEntry, *batchFmtCombo, *batchRecurseCheck;

static const char* fmtToStr(txtr::ImageFormat f) {
    switch (f) {
        case txtr::ImageFormat::PNG:  return "PNG";
        case txtr::ImageFormat::JPEG: return "JPEG";
        case txtr::ImageFormat::BMP:  return "BMP";
        case txtr::ImageFormat::TGA:  return "TGA";
        case txtr::ImageFormat::DDS:  return "DDS";
        default: return "?";
    }
}

static txtr::ImageFormat strToFmt(const char* s) {
    if (strcmp(s, "PNG") == 0) return txtr::ImageFormat::PNG;
    if (strcmp(s, "JPEG") == 0) return txtr::ImageFormat::JPEG;
    if (strcmp(s, "BMP") == 0) return txtr::ImageFormat::BMP;
    if (strcmp(s, "TGA") == 0) return txtr::ImageFormat::TGA;
    if (strcmp(s, "DDS") == 0) return txtr::ImageFormat::DDS;
    return txtr::ImageFormat::PNG;
}



static void onOpen(GtkButton*, GtkWindow* parent) {
    GtkWidget* dialog = gtk_file_chooser_dialog_new("Open Image",
        parent, GTK_FILE_CHOOSER_ACTION_OPEN,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Open", GTK_RESPONSE_ACCEPT, nullptr);

    if (gtk_dialog_run(GTK_DIALOG(dialog)) == GTK_RESPONSE_ACCEPT) {
        char* path = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(dialog));
        currentPath = path;
        g_free(path);

        try {
            currentTex = txtr::load(currentPath);
            auto info = txtr::getInfo(currentPath);

            char buf[256];
            std::snprintf(buf, sizeof(buf),
                "<b>%s</b>\n%dx%d  |  %d channels  |  %salpha  |  %zu bytes",
                currentPath.c_str(),
                info.width, info.height, info.channels,
                info.hasAlpha ? "" : "no ",
                info.fileSize);
            gtk_label_set_markup(GTK_LABEL(infoLabel), buf);
            gtk_label_set_text(GTK_LABEL(statusLabel), "Loaded");
        } catch (const std::exception& e) {
            gtk_label_set_markup(GTK_LABEL(infoLabel),
                ("<span color='red'>" + std::string(e.what()) + "</span>").c_str());
            gtk_label_set_text(GTK_LABEL(statusLabel), "Failed");
        }
    }
    gtk_widget_destroy(dialog);
}



static void onConvert(GtkButton*, GtkWindow* parent) {
    if (!currentTex.valid()) {
        gtk_label_set_text(GTK_LABEL(statusLabel), "No image loaded");
        return;
    }

    GtkWidget* dialog = gtk_file_chooser_dialog_new("Save As",
        parent, GTK_FILE_CHOOSER_ACTION_SAVE,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Save", GTK_RESPONSE_ACCEPT, nullptr);

    auto active = gtk_combo_box_text_get_active_text(GTK_COMBO_BOX_TEXT(formatCombo));
    txtr::ImageFormat outFmt = strToFmt(active);
    g_free(active);

    auto fmtStr = fmtToStr(outFmt);
    std::string defaultName = currentPath;
    auto dot = defaultName.rfind('.');
    if (dot != std::string::npos) defaultName = defaultName.substr(0, dot);
    defaultName += "." + std::string(fmtStr);
    gtk_file_chooser_set_current_name(GTK_FILE_CHOOSER(dialog), defaultName.c_str());

    if (gtk_dialog_run(GTK_DIALOG(dialog)) == GTK_RESPONSE_ACCEPT) {
        char* path = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(dialog));
        std::string outPath = path;
        g_free(path);

        try {
            auto tex = currentTex;
            if (outFmt == txtr::ImageFormat::JPEG && tex.channels == 4) {
                txtr::ConvertOptions copts;
                copts.outputFormat = txtr::ImageFormat::JPEG;
                tex = txtr::convert(tex, copts);
            }

            txtr::save(outPath, tex, outFmt);

            char buf[128];
            std::snprintf(buf, sizeof(buf), "Saved as %s", fmtStr);
            gtk_label_set_text(GTK_LABEL(statusLabel), buf);
        } catch (const std::exception& e) {
            gtk_label_set_text(GTK_LABEL(statusLabel), e.what());
        }
    }
    gtk_widget_destroy(dialog);
}



static void onBatch(GtkButton*, GtkWindow*) {
    const char* dir = gtk_entry_get_text(GTK_ENTRY(batchDirEntry));
    if (!dir || !*dir) {
        gtk_label_set_text(GTK_LABEL(statusLabel), "No directory specified");
        return;
    }

    auto active = gtk_combo_box_text_get_active_text(GTK_COMBO_BOX_TEXT(batchFmtCombo));
    auto outFmt = strToFmt(active);
    g_free(active);
    bool recurse = gtk_toggle_button_get_active(GTK_TOGGLE_BUTTON(batchRecurseCheck));

    std::function<std::vector<std::string>(const std::string&)> files;
    files = [&](const std::string& d) -> std::vector<std::string> {
        std::vector<std::string> result;
        DIR* dp = opendir(d.c_str());
        if (!dp) return result;
        struct dirent* entry;
        while ((entry = readdir(dp))) {
            std::string name = entry->d_name;
            if (name == "." || name == "..") continue;
            std::string full = d + "/" + name;
            struct stat st;
            if (stat(full.c_str(), &st) != 0) continue;
            if (S_ISDIR(st.st_mode)) {
                if (recurse) {
                    auto sub = files(full);
                    result.insert(result.end(), sub.begin(), sub.end());
                }
            } else if (S_ISREG(st.st_mode)) {
                auto ext = txtr::platform::getExtension(name);
                if (ext == "png" || ext == "jpg" || ext == "jpeg" ||
                    ext == "bmp" || ext == "tga" || ext == "dds")
                    result.push_back(full);
            }
        }
        closedir(dp);
        return result;
    };

    auto inputs = files(dir);
    if (inputs.empty()) {
        gtk_label_set_text(GTK_LABEL(statusLabel), "No images found");
        return;
    }

    std::string outDir = std::string(dir) + "/converted";
    mkdir(outDir.c_str(), 0755);

    int ok = 0, fail = 0;
    for (auto& f : inputs) {
        auto base = f.substr(f.rfind('/') + 1);
        auto dot = base.rfind('.');
        auto outName = (dot != std::string::npos) ? base.substr(0, dot) : base;
        outName += "." + std::string(fmtToStr(outFmt));
        auto outPath = outDir + "/" + outName;

        try {
            auto tex = txtr::load(f);
            txtr::save(outPath, tex, outFmt);
            ok++;
        } catch (...) { fail++; }
    }

    char buf[128];
    std::snprintf(buf, sizeof(buf), "Batch: %d ok, %d failed -> %s", ok, fail, outDir.c_str());
    gtk_label_set_text(GTK_LABEL(statusLabel), buf);
}



static void onBatchBrowse(GtkButton*, GtkWindow* parent) {
    GtkWidget* dialog = gtk_file_chooser_dialog_new("Select Directory",
        parent, GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER,
        "_Cancel", GTK_RESPONSE_CANCEL,
        "_Select", GTK_RESPONSE_ACCEPT, nullptr);

    if (gtk_dialog_run(GTK_DIALOG(dialog)) == GTK_RESPONSE_ACCEPT) {
        char* path = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(dialog));
        gtk_entry_set_text(GTK_ENTRY(batchDirEntry), path);
        g_free(path);
    }
    gtk_widget_destroy(dialog);
}



static void onAbout(GtkWidget*, GtkWindow* parent) {
    GtkWidget* dlg = gtk_about_dialog_new();
    gtk_about_dialog_set_program_name(GTK_ABOUT_DIALOG(dlg), "txtr");
    gtk_about_dialog_set_version(GTK_ABOUT_DIALOG(dlg), "1.0");
    gtk_about_dialog_set_comments(GTK_ABOUT_DIALOG(dlg), "Texture conversion tool\nSupports PNG, JPEG, BMP, TGA, DDS");
    gtk_window_set_transient_for(GTK_WINDOW(dlg), parent);
    gtk_dialog_run(GTK_DIALOG(dlg));
    gtk_widget_destroy(dlg);
}



int main(int argc, char** argv) {
    gtk_init(&argc, &argv);

    auto* app = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(app), "txtr");
    gtk_window_set_default_size(GTK_WINDOW(app), 520, 400);
    g_signal_connect(app, "destroy", G_CALLBACK(gtk_main_quit), nullptr);

    auto* nb = gtk_notebook_new();


    auto* tab1 = gtk_box_new(GTK_ORIENTATION_VERTICAL, 6);
    gtk_container_set_border_width(GTK_CONTAINER(tab1), 12);

    auto* openBtn = gtk_button_new_with_label("Open Image");
    g_signal_connect(openBtn, "clicked", G_CALLBACK(onOpen), app);
    gtk_box_pack_start(GTK_BOX(tab1), openBtn, FALSE, FALSE, 0);

    infoLabel = gtk_label_new("No image loaded");
    gtk_label_set_xalign(GTK_LABEL(infoLabel), 0);
    gtk_box_pack_start(GTK_BOX(tab1), infoLabel, FALSE, FALSE, 0);

    auto* sep = gtk_separator_new(GTK_ORIENTATION_HORIZONTAL);
    gtk_box_pack_start(GTK_BOX(tab1), sep, FALSE, FALSE, 6);

    auto* fmtRow = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
    gtk_box_pack_start(GTK_BOX(fmtRow), gtk_label_new("Format:"), FALSE, FALSE, 0);
    formatCombo = gtk_combo_box_text_new();
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(formatCombo), nullptr, "PNG");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(formatCombo), nullptr, "JPEG");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(formatCombo), nullptr, "BMP");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(formatCombo), nullptr, "TGA");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(formatCombo), nullptr, "DDS");
    gtk_combo_box_set_active(GTK_COMBO_BOX(formatCombo), 0);
    gtk_box_pack_start(GTK_BOX(fmtRow), formatCombo, FALSE, FALSE, 0);

    gtk_box_pack_start(GTK_BOX(fmtRow), gtk_label_new("  Quality:"), FALSE, FALSE, 0);
    qualitySpin = gtk_spin_button_new_with_range(1, 100, 1);
    gtk_spin_button_set_value(GTK_SPIN_BUTTON(qualitySpin), 90);
    gtk_box_pack_start(GTK_BOX(fmtRow), qualitySpin, FALSE, FALSE, 0);

    gtk_box_pack_start(GTK_BOX(tab1), fmtRow, FALSE, FALSE, 0);

    auto* convertBtn = gtk_button_new_with_label("Convert & Save");
    g_signal_connect(convertBtn, "clicked", G_CALLBACK(onConvert), app);
    gtk_box_pack_start(GTK_BOX(tab1), convertBtn, FALSE, FALSE, 0);

    statusLabel = gtk_label_new("Ready");
    gtk_label_set_xalign(GTK_LABEL(statusLabel), 0);
    gtk_box_pack_start(GTK_BOX(tab1), statusLabel, FALSE, FALSE, 0);

    gtk_notebook_append_page(GTK_NOTEBOOK(nb), tab1, gtk_label_new("Single"));


    auto* tab2 = gtk_box_new(GTK_ORIENTATION_VERTICAL, 6);
    gtk_container_set_border_width(GTK_CONTAINER(tab2), 12);

    auto* dirRow = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
    batchDirEntry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(batchDirEntry), "/path/to/textures");
    gtk_box_pack_start(GTK_BOX(dirRow), batchDirEntry, TRUE, TRUE, 0);
    auto* browseBtn = gtk_button_new_with_label("Browse");
    g_signal_connect(browseBtn, "clicked", G_CALLBACK(onBatchBrowse), app);
    gtk_box_pack_start(GTK_BOX(dirRow), browseBtn, FALSE, FALSE, 0);
    gtk_box_pack_start(GTK_BOX(tab2), dirRow, FALSE, FALSE, 0);

    auto* batchFmtRow = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
    gtk_box_pack_start(GTK_BOX(batchFmtRow), gtk_label_new("Output format:"), FALSE, FALSE, 0);
    batchFmtCombo = gtk_combo_box_text_new();
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(batchFmtCombo), nullptr, "PNG");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(batchFmtCombo), nullptr, "JPEG");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(batchFmtCombo), nullptr, "BMP");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(batchFmtCombo), nullptr, "TGA");
    gtk_combo_box_text_append(GTK_COMBO_BOX_TEXT(batchFmtCombo), nullptr, "DDS");
    gtk_combo_box_set_active(GTK_COMBO_BOX(batchFmtCombo), 4);
    gtk_box_pack_start(GTK_BOX(batchFmtRow), batchFmtCombo, FALSE, FALSE, 0);

    batchRecurseCheck = gtk_check_button_new_with_label("Recursive");
    gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(batchRecurseCheck), TRUE);
    gtk_box_pack_start(GTK_BOX(batchFmtRow), batchRecurseCheck, FALSE, FALSE, 0);

    gtk_box_pack_start(GTK_BOX(tab2), batchFmtRow, FALSE, FALSE, 0);

    auto* batchBtn = gtk_button_new_with_label("Convert All");
    g_signal_connect(batchBtn, "clicked", G_CALLBACK(onBatch), app);
    gtk_box_pack_start(GTK_BOX(tab2), batchBtn, FALSE, FALSE, 0);

    gtk_notebook_append_page(GTK_NOTEBOOK(nb), tab2, gtk_label_new("Batch"));

    auto* vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
    auto* menubar = gtk_menu_bar_new();
    auto* helpMenu = gtk_menu_new();
    auto* aboutItem = gtk_menu_item_new_with_label("About");
    g_signal_connect(aboutItem, "activate", G_CALLBACK(onAbout), app);
    gtk_menu_shell_append(GTK_MENU_SHELL(helpMenu), aboutItem);
    auto* helpItem = gtk_menu_item_new_with_label("Help");
    gtk_menu_item_set_submenu(GTK_MENU_ITEM(helpItem), helpMenu);
    gtk_menu_shell_append(GTK_MENU_SHELL(menubar), helpItem);

    gtk_box_pack_start(GTK_BOX(vbox), menubar, FALSE, FALSE, 0);
    gtk_box_pack_start(GTK_BOX(vbox), nb, TRUE, TRUE, 0);
    gtk_container_add(GTK_CONTAINER(app), vbox);

    gtk_widget_show_all(app);
    gtk_main();
    return 0;
}

#include <QApplication>
#include <cstdio>
#include <cstring>
#include "../include/skin_window.hpp"

static void printHelp() {
    printf("\n=== Skintool — Minecraft Skin Viewer ===\n");
    printf("Usage: skintool [skin.png]\n");
    printf("Controls:\n");
    printf("  Left-drag  : Orbit camera\n");
    printf("  Scroll     : Zoom\n");
    printf("  W          : Toggle wireframe\n");
    printf("  R          : Toggle auto-rotate\n");
    printf("  Space      : Reset camera\n");
    printf("  O          : Open skin PNG\n");
    printf("  Mouse drag : Orbit\n");
    printf("========================================\n\n");
}

int main(int argc, char** argv) {
    QApplication app(argc, argv);

    const char* skinPath = "skin.png";
    if (argc > 1) skinPath = argv[1];

    printHelp();
    printf("Loading skin: %s\n", skinPath);

    SkinWindow win(skinPath);
    win.show();
    return app.exec();
}

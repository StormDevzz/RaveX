#include <QApplication>
#include <cstdio>
#include <cstring>
#include "../include/main_window.hpp"

static void printUsage(const char* prog) {
    printf("Usage: %s <pcap_file>\n", prog);
    printf("Opens a Qt window visualizing Minecraft protocol packets.\n");
}

int main(int argc, char** argv) {
    if (argc < 2) { printUsage(argv[0]); return 1; }

    const char* path = nullptr;
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--help")) { printUsage(argv[0]); return 0; }
        if (argv[i][0] != '-') { path = argv[i]; continue; }
        fprintf(stderr, "Unknown option: %s\n", argv[i]);
        return 1;
    }

    if (!path) { fprintf(stderr, "No pcap file specified.\n"); return 1; }

    QApplication app(argc, argv);
    MainWindow win(QString::fromUtf8(path));
    win.show();
    return app.exec();
}

#include "fs_util.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dirent.h>

char* find_icon_file(const char* de) {
    if (!de || !*de) return NULL;
    char lower[256]; int i;
    for (i = 0; de[i] && i < 255; i++) lower[i] = (de[i] >= 'A' && de[i] <= 'Z') ? de[i] + 32 : de[i];
    lower[i] = '\0';
    const char* paths[] = {
        "/usr/share/icons/hicolor/scalable/apps/",
        "/usr/share/icons/hicolor/128x128/apps/",
        "/usr/share/icons/hicolor/64x64/apps/",
        "/usr/share/icons/hicolor/48x48/apps/",
        "/usr/share/pixmaps/", NULL
    };
    char* home = getenv("HOME");
    char up[512];
    if (home) {
        snprintf(up, sizeof(up), "%s/.local/share/icons/hicolor/128x128/apps/", home);
        DIR* dp = opendir(up); if (dp) { struct dirent* e;
            while ((e = readdir(dp)) != NULL) {
                if (e->d_type != DT_REG && e->d_type != DT_LNK) continue;
                const char* fn = e->d_name;
                if (strncmp(fn, lower, strlen(lower)) == 0 || strncmp(fn, de, strlen(de)) == 0) {
                    const char* ext = strrchr(fn, '.');
                    if (ext && (strcmp(ext, ".png") == 0 || strcmp(ext, ".svg") == 0)) {
                        char* fp = malloc(strlen(up) + strlen(fn) + 1);
                        if (fp) { sprintf(fp, "%s%s", up, fn); closedir(dp); return fp; }
                    }
                }
            } closedir(dp);
        }
    }
    for (int pi = 0; paths[pi]; pi++) {
        DIR* dp = opendir(paths[pi]); if (!dp) continue;
        struct dirent* e;
        while ((e = readdir(dp)) != NULL) {
            if (e->d_type != DT_REG && e->d_type != DT_LNK) continue;
            const char* fn = e->d_name;
            const char* cand[] = {lower, de, NULL};
            for (int ci = 0; cand[ci]; ci++) {
                size_t cl = strlen(cand[ci]);
                if (strncmp(fn, cand[ci], cl) == 0 && (fn[cl] == '.' || fn[cl] == '-' || fn[cl] == '\0')) {
                    const char* ext = strrchr(fn, '.');
                    if (ext && (strcmp(ext, ".png") == 0 || strcmp(ext, ".svg") == 0)) {
                        char* fp = malloc(strlen(paths[pi]) + strlen(fn) + 1);
                        if (fp) { sprintf(fp, "%s%s", paths[pi], fn); closedir(dp); return fp; }
                    }
                }
            }
        } closedir(dp);
    }
    return NULL;
}

uint8_t* read_file(const char* path, size_t* out_len) {
    *out_len = 0;
    if (!path) return NULL;
    int fd = open(path, O_RDONLY);
    if (fd < 0) return NULL;
    struct stat st;
    if (fstat(fd, &st) < 0) { close(fd); return NULL; }
    size_t sz = st.st_size;
    uint8_t* data = malloc(sz); if (!data) { close(fd); return NULL; }
    size_t pos = 0;
    while (pos < sz) { int n = read(fd, data + pos, sz - pos); if (n <= 0) break; pos += n; }
    close(fd);
    *out_len = pos;
    return data;
}

#include <iostream>
#include <string>
#include <cstdlib>
#include <sys/stat.h>
#include "simple/simple_launcher.h"

// создаем папку если ее не существует
void ensureDirectory(const std::string& path) {
    struct stat buffer;
    if (stat(path.c_str(), &buffer) != 0) {
        std::string cmd = "mkdir -p \"" + path + "\"";
        system(cmd.c_str());
    }
}

int main(int argc, char *argv[]) {
    const char* homeEnv = getenv("HOME");
    if (!homeEnv) {
        std::cerr << "[!] error: HOME folder not found. aborting!" << std::endl;
        return 1;
    }
    std::string home = homeEnv;
    std::string modsDir = home + "/.minecraft/mods";
    std::string ravexDir = home + "/.ravex";

    ensureDirectory(modsDir);
    ensureDirectory(ravexDir);
    ensureDirectory(ravexDir + "/font");
    ensureDirectory(ravexDir + "/natives");

    // запускаем графическую оболочку лаунчера
    ravex::launcher::simple::SimpleLauncher::run(modsDir, ravexDir);
    return 0;
}

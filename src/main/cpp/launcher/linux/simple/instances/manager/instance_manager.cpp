#include "include/instance_manager.hpp"
#include <fstream>
#include <sstream>
#include <sys/stat.h>
#include <cstdlib>
#include <dirent.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace instance {

static std::string cfgPath(const std::string& instDir) {
    return instDir + "/instance.cfg";
}

static void ensureDir(const std::string& path) {
    struct stat st;
    if (stat(path.c_str(), &st) != 0) {
        std::string cmd = "mkdir -p \"" + path + "\"";
        system(cmd.c_str());
    }
}

InstanceInfo load_instance(const std::string& instDir) {
    InstanceInfo info;
    info.dir = instDir;

    size_t slash = instDir.rfind('/');
    if (slash != std::string::npos)
        info.name = instDir.substr(slash + 1);

    std::ifstream f(cfgPath(instDir));
    if (!f.is_open()) return info;

    std::string line;
    while (std::getline(f, line)) {
        if (line.empty() || line[0] == '#') continue;
        size_t eq = line.find('=');
        if (eq == std::string::npos) continue;
        std::string key = line.substr(0, eq);
        std::string val = line.substr(eq + 1);
        if (key == "name") info.name = val;
        else if (key == "mc_version") info.mc_version = val;
        else if (key == "ram_mb") info.ram_mb = std::stoi(val);
        else if (key == "icon") info.icon_path = val;
    }
    return info;
}

std::vector<InstanceInfo> load_instances(const std::string& kickxDir) {
    std::vector<InstanceInfo> instances;
    std::string instancesDir = kickxDir + "/instances";

    DIR* dir = opendir(instancesDir.c_str());
    if (!dir) {

        InstanceInfo def = create_default_instance(kickxDir);
        instances.push_back(def);
        return instances;
    }

    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        std::string name(entry->d_name);
        if (name == "." || name == "..") continue;
        if (entry->d_type != DT_DIR) continue;

        std::string instDir = instancesDir + "/" + name;
        InstanceInfo info = load_instance(instDir);
        if (!info.name.empty()) {
            instances.push_back(info);
        }
    }
    closedir(dir);

    if (instances.empty()) {
        InstanceInfo def = create_default_instance(kickxDir);
        instances.push_back(def);
    }

    return instances;
}

void save_instance(const std::string& kickxDir, const InstanceInfo& inst) {
    std::string instDir = kickxDir + "/instances/" + inst.name;
    ensureDir(instDir);

    std::ofstream f(cfgPath(instDir));
    if (!f.is_open()) return;
    f << "name=" << inst.name << "\n";
    f << "mc_version=" << inst.mc_version << "\n";
    f << "ram_mb=" << inst.ram_mb << "\n";
    f << "icon=" << inst.icon_path << "\n";
    f.close();
}

void delete_instance(const std::string& kickxDir, const std::string& name) {
    std::string instDir = kickxDir + "/instances/" + name;
    std::string cmd = "rm -rf \"" + instDir + "\"";
    system(cmd.c_str());
}

InstanceInfo create_default_instance(const std::string& kickxDir) {
    InstanceInfo def;
    def.name = "default";
    def.dir = kickxDir + "/instances/default";
    def.mc_version = "1.21.11";
    def.ram_mb = 4096;
    ensureDir(def.dir);
    ensureDir(def.dir + "/mods");
    save_instance(kickxDir, def);
    return def;
}

}
}
}
}

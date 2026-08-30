#include "game/include/fabric.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include <shlobj.h>

namespace ravex::game {

namespace {

std::wstring gameDir() {
    PWSTR known = nullptr;
    std::wstring profile;
    if (SHGetKnownFolderPath(FOLDERID_Profile, KF_FLAG_DEFAULT, nullptr, &known) == S_OK) {
        profile = known;
    }
    if (known) CoTaskMemFree(known);
    if (profile.empty()) {
        DWORD size = GetEnvironmentVariableW(L"USERPROFILE", nullptr, 0);
        if (size > 0) {
            profile.resize(size - 1);
            GetEnvironmentVariableW(L"USERPROFILE", profile.data(), size);
        }
    }
    return joinPath(profile, L".minecraft");
}

std::wstring fabricVersionDir(const std::string& mcVersion) {
    return joinPath(joinPath(gameDir(), L"versions"), fromUtf8(mcVersion) + L"-fabric");
}

std::wstring fabricJar(const std::string& mcVersion) {
    return joinPath(fabricVersionDir(mcVersion), fromUtf8(mcVersion) + L"-fabric.jar");
}

std::wstring fabricJson(const std::string& mcVersion) {
    return joinPath(fabricVersionDir(mcVersion), fromUtf8(mcVersion) + L"-fabric.json");
}

std::wstring forgeVersionDir(const std::string& mcVersion) {
    return joinPath(joinPath(gameDir(), L"versions"), fromUtf8(mcVersion) + L"-forge");
}

std::wstring forgeJson(const std::string& mcVersion) {
    return joinPath(forgeVersionDir(mcVersion), fromUtf8(mcVersion) + L"-forge.json");
}

std::wstring quiltVersionDir(const std::string& mcVersion) {
    return joinPath(joinPath(gameDir(), L"versions"), fromUtf8(mcVersion) + L"-quilt");
}

std::wstring quiltJson(const std::string& mcVersion) {
    return joinPath(quiltVersionDir(mcVersion), fromUtf8(mcVersion) + L"-quilt.json");
}

}

bool isFabricInstalled(const std::string& mcVersion) {
    return fileExists(fabricJson(mcVersion));
}

bool ensureFabric(const std::string& mcVersion, const std::string& loaderVersion, std::string* error,
                  const std::function<void(const net::Progress&)>& progress, const bool* cancelled) {
    if (mcVersion.empty()) {
        *error = "No version specified";
        return false;
    }
    if (isFabricInstalled(mcVersion)) return true;

    std::string resolvedLoader = loaderVersion;
    if (resolvedLoader.empty()) {
        std::string loaderUrl = "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion + "/latest/stable";
        std::string loaderResp = net::httpGet(loaderUrl, error);
        if (loaderResp.empty()) return false;
        ravex::json::Value loaderArr = ravex::json::Value::parse(loaderResp);
        if (loaderArr.isNull() || loaderArr.size() == 0) {
            *error = "No Fabric loader found for " + mcVersion;
            return false;
        }
        const ravex::json::Value& loader = loaderArr.at(0);
        if (loader.has("loader") && loader.at("loader").has("version")) {
            resolvedLoader = loader.at("loader").at("version").asString();
        }
    }
    if (resolvedLoader.empty()) {
        *error = "Invalid Fabric loader version";
        return false;
    }

    std::string mappingsUrl = "https://meta.fabricmc.net/v2/versions/yarn/" + mcVersion + "/latest";
    std::string mappingsResp = net::httpGet(mappingsUrl, error);
    std::string mappingsVersion;
    if (!mappingsResp.empty()) {
        ravex::json::Value mappingsArr = ravex::json::Value::parse(mappingsResp);
        if (!mappingsArr.isNull() && mappingsArr.size() > 0) {
            const ravex::json::Value& m = mappingsArr.at(0);
            if (m.has("version")) mappingsVersion = m.at("version").asString();
        }
    }

    std::wstring vDir = fabricVersionDir(mcVersion);
    createDirs(vDir);

    std::string profileJson = "{\n"
        "  \"id\": \"" + mcVersion + "-fabric\",\n"
        "  \"inheritsFrom\": \"" + mcVersion + "\",\n"
        "  \"mainClass\": \"net.fabricmc.loader.impl.launch.knot.KnotClient\",\n"
        "  \"arguments\": {\n"
        "    \"jvm\": [\n"
        "      \"-Dfabric.threadDump.log4j=false\"\n"
        "    ]\n"
        "  },\n"
        "  \"libraries\": [\n";
    profileJson += "    { \"name\": \"net.fabricmc:sponge-mixin:0.8.2+build.24\" },\n";
    profileJson += "    { \"name\": \"net.fabricmc:fabric-loader:" + resolvedLoader + "\" },\n";
    if (!mappingsVersion.empty()) {
        profileJson += "    { \"name\": \"net.fabricmc:yarn:" + mappingsVersion + ":v2\" },\n";
    }
    profileJson += "    { \"name\": \"net.fabricmc:fabric-language-kotlin:1.10.0+kotlin.1.8.21\" }\n";
    profileJson += "  ]\n}";

    writeFileAtomic(fabricJson(mcVersion), profileJson);
    auto ensureLib = [&](const std::string& mavenCoord){
        std::string p = mavenCoord;
        std::string url;
        std::string path;
        size_t c1 = p.find(':');
        size_t c2 = p.find(':', c1+1);
        if (c1==std::string::npos || c2==std::string::npos) return true;
        std::string group = p.substr(0,c1);
        std::string artifact = p.substr(c1+1, c2-c1-1);
        std::string ver = p.substr(c2+1);
        std::string groupPath = group;
        for(char &c: groupPath) if(c=='.') c='/';
        url = "https://maven.fabricmc.net/" + groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar";
        std::wstring dest = joinPath(joinPath(gameDir(), L"libraries"), fromUtf8(groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar"));
        // handle windows path separators: groupPath already has /, joinPath will handle
        if (fileExists(dest)) return true;
        createDirs(dest.substr(0, dest.find_last_of(L"\\/")));
        std::string err2;
        bool ok = net::downloadFile(url, dest, progress, cancelled, &err2);
        if (!ok && error) *error = err2;
        return ok;
    };
    if (!ensureLib("net.fabricmc:fabric-loader:" + resolvedLoader)) return false;
    if (!ensureLib("net.fabricmc:sponge-mixin:0.8.2+build.24")) return false;
    if (!ensureLib("net.fabricmc:fabric-language-kotlin:1.10.0+kotlin.1.8.21")) return false;
    if (!mappingsVersion.empty()) {
        std::string url = "https://maven.fabricmc.net/net/fabricmc/yarn/" + mappingsVersion + "/yarn-" + mappingsVersion + "-v2.jar";
        std::wstring dest = joinPath(joinPath(gameDir(), L"libraries"), fromUtf8("net/fabricmc/yarn/" + mappingsVersion + "/yarn-" + mappingsVersion + "-v2.jar"));
        if (!fileExists(dest)) {
            createDirs(dest.substr(0, dest.find_last_of(L"\\/")));
            std::string err2;
            if (!net::downloadFile(url, dest, progress, cancelled, &err2)) return false;
        }
    }
    return true;
}

std::vector<std::string> fetchFabricLoaderVersions(const std::string& mcVersion) {
    std::vector<std::string> out;
    std::string url = mcVersion.empty() ? "https://meta.fabricmc.net/v2/versions/loader" : "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion;
    std::string resp = net::httpGet(url, nullptr);
    if (resp.empty()) return out;
    ravex::json::Value arr = ravex::json::Value::parse(resp);
    if (arr.isNull()) return out;
    for (size_t i = 0; i < arr.size(); ++i) {
        const ravex::json::Value& v = arr.at(i);
        if (v.has("version")) out.push_back(v.at("version").asString());
        else if (v.has("loader") && v.at("loader").has("version")) out.push_back(v.at("loader").at("version").asString());
    }
    if (out.empty() && arr.has("version")) out.push_back(arr.at("version").asString());
    return out;
}

std::vector<std::string> fetchForgeLoaderVersions(const std::string& mcVersion) {
    std::vector<std::string> out;
    std::string resp = net::httpGet("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml", nullptr);
    if (resp.empty()) resp = net::httpGet("https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml", nullptr);
    if (resp.empty()) return out;
    size_t pos = 0;
    while ((pos = resp.find("<version>", pos)) != std::string::npos) {
        size_t start = pos + 9;
        size_t end = resp.find("</version>", start);
        if (end == std::string::npos) break;
        std::string ver = resp.substr(start, end - start);
        if (!mcVersion.empty()) {
            std::string prefix = mcVersion + "-";
            if (ver.rfind(prefix, 0) == 0) {
                std::string shortVer = ver.substr(prefix.size());
                if (!shortVer.empty()) out.push_back(shortVer);
            }
        } else {
            out.push_back(ver);
        }
        pos = end + 10;
        if (out.size() >= 40) break;
    }
    if (out.size() > 30) out.resize(30);
    return out;
}

std::vector<std::string> fetchQuiltLoaderVersions(const std::string& mcVersion) {
    std::vector<std::string> out;
    std::string resp = net::httpGet("https://meta.quiltmc.org/v3/versions/loader", nullptr);
    if (resp.empty()) return out;
    ravex::json::Value arr = ravex::json::Value::parse(resp);
    if (arr.isNull()) return out;
    for (size_t i = 0; i < arr.size(); ++i) {
        const ravex::json::Value& v = arr.at(i);
        if (v.has("version")) out.push_back(v.at("version").asString());
        else if (v.type() == ravex::json::Value::Type::String) out.push_back(v.asString());
        if (out.size() >= 30) break;
    }
    return out;
}

bool isForgeInstalled(const std::string& mcVersion) {
    return fileExists(forgeJson(mcVersion));
}

bool isQuiltInstalled(const std::string& mcVersion) {
    return fileExists(quiltJson(mcVersion));
}

void removeLoaderVersion(const std::string& mcVersion, const std::string& loader) {
    std::wstring dir;
    if (loader == "fabric") dir = fabricVersionDir(mcVersion);
    else if (loader == "forge") dir = forgeVersionDir(mcVersion);
    else if (loader == "quilt") dir = quiltVersionDir(mcVersion);
    else return;
    if (!fileExists(dir)) return;
    WIN32_FIND_DATAW fd;
    std::wstring pat = joinPath(dir, L"*");
    HANDLE h = FindFirstFileW(pat.c_str(), &fd);
    if (h != INVALID_HANDLE_VALUE) {
        do {
            if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
                DeleteFileW(joinPath(dir, fd.cFileName).c_str());
            }
        } while (FindNextFileW(h, &fd));
        FindClose(h);
    }
    RemoveDirectoryW(dir.c_str());
}

bool ensureForge(const std::string& mcVersion, const std::string& loaderVersion, std::string* error,
                  const std::function<void(const net::Progress&)>& progress, const bool* cancelled) {
    if (mcVersion.empty()) { *error = "No version specified"; return false; }
    if (isForgeInstalled(mcVersion)) return true;

    std::string resolvedLoader = loaderVersion;
    if (resolvedLoader.empty()) {
        std::string url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml";
        std::string resp = net::httpGet(url, error);
        if (resp.empty()) { *error = "Failed to fetch Forge versions"; return false; }
        std::string prefix = mcVersion + "-";
        size_t pos = 0;
        while ((pos = resp.find("<version>", pos)) != std::string::npos) {
            size_t start = pos + 9;
            size_t end = resp.find("</version>", start);
            if (end == std::string::npos) break;
            std::string ver = resp.substr(start, end - start);
            if (ver.rfind(prefix, 0) == 0) { resolvedLoader = ver.substr(prefix.size()); break; }
            pos = end + 10;
        }
    }
    if (resolvedLoader.empty()) { *error = "No Forge version found for " + mcVersion; return false; }

    std::wstring vDir = forgeVersionDir(mcVersion);
    createDirs(vDir);

    std::string fullForgeVer = mcVersion + "-" + resolvedLoader;
    std::string profileJson = "{\n"
        "  \"id\": \"" + mcVersion + "-forge\",\n"
        "  \"inheritsFrom\": \"" + mcVersion + "\",\n"
        "  \"mainClass\": \"net.minecraftforge.fml.relauncher.ServerLaunchWrapper\",\n"
        "  \"libraries\": [\n"
        "    { \"name\": \"net.minecraftforge:forge:" + fullForgeVer + "\", \"url\": \"https://files.minecraftforge.net/maven/\" },\n"
        "    { \"name\": \"net.minecraft:launchwrapper:1.12\", \"url\": \"https://libraries.minecraft.net/\" },\n"
        "    { \"name\": \"org.ow2.asm:asm-all:5.2\", \"url\": \"https://libraries.minecraft.net/\" }\n"
        "  ]\n}";

    writeFileAtomic(forgeJson(mcVersion), profileJson);

    auto ensureForgeLib = [&](const std::string& group, const std::string& artifact, const std::string& ver, const std::string& mavenBase, const std::string& mavenUrl) {
        std::string groupPath = group;
        for (char& c : groupPath) if (c == '.') c = '/';
        std::string url = mavenUrl + groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar";
        std::wstring dest = joinPath(joinPath(gameDir(), L"libraries"), fromUtf8(groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar"));
        if (fileExists(dest)) return true;
        createDirs(dest.substr(0, dest.find_last_of(L"\\/")));
        std::string err2;
        bool ok = net::downloadFile(url, dest, progress, cancelled, &err2);
        if (!ok && error) *error = err2;
        return ok;
    };

    if (!ensureForgeLib("net.minecraftforge", "forge", fullForgeVer, "forge", "https://files.minecraftforge.net/maven/")) return false;
    if (!ensureForgeLib("net.minecraft", "launchwrapper", "1.12", "launchwrapper", "https://libraries.minecraft.net/")) return false;
    if (!ensureForgeLib("org.ow2.asm", "asm-all", "5.2", "asm", "https://libraries.minecraft.net/")) return false;

    return true;
}

bool ensureQuilt(const std::string& mcVersion, const std::string& loaderVersion, std::string* error,
                  const std::function<void(const net::Progress&)>& progress, const bool* cancelled) {
    if (mcVersion.empty()) { *error = "No version specified"; return false; }
    if (isQuiltInstalled(mcVersion)) return true;

    std::string resolvedLoader = loaderVersion;
    if (resolvedLoader.empty()) {
        std::string loaderUrl = "https://meta.quiltmc.org/v3/versions/loader/" + mcVersion + "/latest";
        std::string loaderResp = net::httpGet(loaderUrl, error);
        if (!loaderResp.empty()) {
            ravex::json::Value loaderArr = ravex::json::Value::parse(loaderResp);
            if (!loaderArr.isNull() && loaderArr.size() > 0) {
                const ravex::json::Value& l = loaderArr.at(0);
                if (l.has("loader") && l.at("loader").has("version")) resolvedLoader = l.at("loader").at("version").asString();
            }
        }
    }
    if (resolvedLoader.empty()) { *error = "No Quilt loader found for " + mcVersion; return false; }

    std::string mappingsUrl = "https://meta.quiltmc.org/v3/versions/yarn/" + mcVersion + "/latest";
    std::string mappingsResp = net::httpGet(mappingsUrl, error);
    std::string mappingsVersion;
    if (!mappingsResp.empty()) {
        ravex::json::Value mappingsArr = ravex::json::Value::parse(mappingsResp);
        if (!mappingsArr.isNull() && mappingsArr.size() > 0) {
            const ravex::json::Value& m = mappingsArr.at(0);
            if (m.has("version")) mappingsVersion = m.at("version").asString();
        }
    }

    std::wstring vDir = quiltVersionDir(mcVersion);
    createDirs(vDir);

    std::string profileJson = "{\n"
        "  \"id\": \"" + mcVersion + "-quilt\",\n"
        "  \"inheritsFrom\": \"" + mcVersion + "\",\n"
        "  \"mainClass\": \"org.quiltmc.loader.impl.launch.knot.KnotClient\",\n"
        "  \"arguments\": {\n"
        "    \"jvm\": [\"-Dquilt.threadDump.log4j=false\"]\n"
        "  },\n"
        "  \"libraries\": [\n"
        "    { \"name\": \"org.quiltmc:quilt-loader:" + resolvedLoader + "\" },\n"
        "    { \"name\": \"org.ow2.asm:asm:9.7.1\" },\n"
        "    { \"name\": \"org.ow2.asm:asm-commons:9.7.1\" },\n"
        "    { \"name\": \"org.ow2.asm:asm-tree:9.7.1\" },\n"
        "    { \"name\": \"org.ow2.asm:asm-util:9.7.1\" },\n"
        "    { \"name\": \"org.ow2.asm:asm-analysis:9.7.1\" }\n"
        "  ]\n}";

    writeFileAtomic(quiltJson(mcVersion), profileJson);

    auto ensureQLib = [&](const std::string& mavenCoord) {
        std::string p = mavenCoord;
        size_t c1 = p.find(':');
        size_t c2 = p.find(':', c1 + 1);
        size_t c3 = p.find(':', c2 + 1);
        if (c1 == std::string::npos || c2 == std::string::npos) return true;
        std::string group = p.substr(0, c1);
        std::string artifact = p.substr(c1 + 1, c2 - c1 - 1);
        std::string ver = (c3 != std::string::npos) ? p.substr(c2 + 1, c3 - c2 - 1) : p.substr(c2 + 1);
        std::string groupPath = group;
        for (char& c : groupPath) if (c == '.') c = '/';
        std::string url = "https://maven.quiltmc.org/repository/release/" + groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar";
        std::wstring dest = joinPath(joinPath(gameDir(), L"libraries"), fromUtf8(groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar"));
        if (fileExists(dest)) return true;
        createDirs(dest.substr(0, dest.find_last_of(L"\\/")));
        std::string err2;
        bool ok = net::downloadFile(url, dest, progress, cancelled, &err2);
        if (!ok) {
            url = "https://libraries.minecraft.net/" + groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar";
            ok = net::downloadFile(url, dest, progress, cancelled, &err2);
        }
        if (!ok && error) *error = err2;
        return ok;
    };

    if (!ensureQLib("org.quiltmc:quilt-loader:" + resolvedLoader)) return false;
    if (!ensureQLib("org.ow2.asm:asm:9.7.1")) return false;
    if (!ensureQLib("org.ow2.asm:asm-commons:9.7.1")) return false;
    if (!ensureQLib("org.ow2.asm:asm-tree:9.7.1")) return false;
    if (!ensureQLib("org.ow2.asm:asm-util:9.7.1")) return false;
    if (!ensureQLib("org.ow2.asm:asm-analysis:9.7.1")) return false;

    return true;
}

}

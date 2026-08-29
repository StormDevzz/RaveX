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

}

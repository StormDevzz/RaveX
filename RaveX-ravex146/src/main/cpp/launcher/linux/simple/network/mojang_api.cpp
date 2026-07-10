#include "include/mojang_api.hpp"
#include "include/http_client.hpp"
#include "include/mojang_downloader.hpp"
#include "include/mojang_parser.hpp"
#include "../event/include/event_queue.hpp"
#include "../integr/include/fabric_installer.hpp"
#include <iostream>
#include <fstream>
#include <vector>
#include <sys/stat.h>
#include <unistd.h>
#include <cstdlib>
#include <cstdio>
#include <thread>
#include <atomic>
#include <cstring>

namespace ravex {
namespace launcher {
namespace simple {
namespace network {

static void ensureDirectory(const std::string& path) {
    std::string cmd = "mkdir -p \"" + path + "\"";
    system(cmd.c_str());
}


static std::string readCachedVersion(const std::string& kickxDir) {
    std::string path = kickxDir + "/.current_version.txt";
    std::ifstream f(path);
    std::string ver;
    if (f.is_open()) {
        std::getline(f, ver);
        f.close();
    }
    return ver;
}


static void writeCachedVersion(const std::string& kickxDir, const std::string& version) {
    std::string path = kickxDir + "/.current_version.txt";
    std::ofstream f(path);
    if (f.is_open()) {
        f << version;
        f.close();
    }
}


static void parallelDownload(const std::vector<std::string>& urls,
                             const std::vector<std::string>& dests) {

    for (const auto& d : dests) {
        size_t slash = d.find_last_of('/');
        if (slash != std::string::npos)
            ensureDirectory(d.substr(0, slash));
    }


    for (size_t i = 0; i < urls.size(); i += 16) {
        std::string batch;
        size_t end = std::min(i + 16, urls.size());
        for (size_t j = i; j < end; j++) {
            struct stat st;
            if (stat(dests[j].c_str(), &st) == 0) continue;
            batch += "curl -sL -o \"" + dests[j] + "\" \"" + urls[j] + "\" 2>/dev/null &\n";
        }
        if (!batch.empty()) {
            batch += "wait\n";
            system(batch.c_str());
        }
    }
}


static bool downloadFabricLibraries(LauncherState *state,
                                    const std::string& mcVersion,
                                    double progressStart, double progressRange) {
    using namespace event;

    queue_progress(state, "Setting up Fabric...", progressStart);

    bool ok = integr::installFabric(state->kickx_dir, mcVersion);


    {
        std::string installerMeta = http_get("https://meta.fabricmc.net/v2/versions/installer"
        if (!installerMeta.empty() && installerMeta != "[]" && installerMeta != "[\n]") {

            size_t verKey = installerMeta.find("\"version\":");
            if (verKey != std::string::npos) {
                verKey += 10;
                while (verKey < installerMeta.length() &&
                       (installerMeta[verKey] == ' ' || installerMeta[verKey] == '\t' ||
                        installerMeta[verKey] == '\n' || installerMeta[verKey] == '\r'))
                    verKey++;
                if (verKey < installerMeta.length() && installerMeta[verKey] == '"') {
                    size_t ivEnd = installerMeta.find("\"", verKey + 1);
                    if (ivEnd != std::string::npos) {
                        std::string instVersion = installerMeta.substr(verKey + 1, ivEnd - verKey - 1);
                        std::string installerUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/"
                                                   + instVersion + "/fabric-installer-" + instVersion + ".jar";
                        std::string installerDest = state->kickx_dir + "/fabric-installer-" + instVersion + ".jar";
                        struct stat st;
                        if (stat(installerDest.c_str(), &st) != 0) {
                            http_download(installerUrl, installerDest);
                        }
                    }
                }
            }
        }
    }

    queue_progress(state, ok ? "Fabric ready!" : "Fabric not available", progressStart + progressRange);
    return ok;
}


bool download_minecraft_version(LauncherState *state, const std::string& version) {
    using namespace event;
    queue_progress(state, "Fetching Mojang manifest...", 0.0);

    std::string manifest = http_get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    if (manifest.empty()) {
        queue_hide(state);
        return false;
    }

    std::string search_str = "\"id\": \"" + version + "\"";
    size_t id_pos = manifest.find(search_str);
    if (id_pos == std::string::npos) {
        queue_hide(state);
        return false;
    }

    size_t url_pos = manifest.find("\"url\":", id_pos);
    if (url_pos == std::string::npos) return false;
    size_t start = manifest.find("\"", url_pos + 6);
    size_t end = manifest.find("\"", start + 1);
    if (start == std::string::npos || end == std::string::npos) return false;
    std::string version_url = manifest.substr(start + 1, end - start - 1);

    queue_progress(state, "Downloading version metadata...", 0.05);

    std::string version_json = http_get(version_url);
    if (version_json.empty()) {
        queue_hide(state);
        return false;
    }

    std::string version_json_path = state->kickx_dir + "/versions/" + version + "/" + version + ".json";
    ensureDirectory(state->kickx_dir + "/versions/" + version);
    std::ofstream out_json(version_json_path);
    if (out_json.is_open()) {
        out_json << version_json;
        out_json.close();
    }


    std::string cached_ver = readCachedVersion(state->kickx_dir);
    if (cached_ver != version) {
        queue_progress(state, "Version changed — cleaning old libraries...", 0.07);
        system(("rm -rf \"" + state->kickx_dir + "/libraries\" 2>/dev/null").c_str());
        ensureDirectory(state->kickx_dir + "/libraries");
        writeCachedVersion(state->kickx_dir, version);
    } else {
        queue_progress(state, "Skipping library cleanup (same version)...", 0.07);
        ensureDirectory(state->kickx_dir + "/libraries");
    }

    queue_progress(state, "Downloading client.jar...", 0.1);

    size_t client_pos = version_json.find("\"client\":");
    if (client_pos == std::string::npos) {
        queue_hide(state);
        return false;
    }
    size_t client_url_pos = version_json.find("\"url\":", client_pos);
    if (client_url_pos == std::string::npos) return false;
    start = version_json.find("\"", client_url_pos + 6);
    end = version_json.find("\"", start + 1);
    if (start == std::string::npos || end == std::string::npos) return false;
    std::string client_url = version_json.substr(start + 1, end - start - 1);

    std::string client_jar_path = state->kickx_dir + "/versions/" + version + "/" + version + ".jar";
    if (!download_client_jar(client_url, client_jar_path)) {
        queue_hide(state);
        return false;
    }


    size_t offset = 0;
    std::vector<std::string> lib_urls, lib_dests;
    while (true) {
        size_t pos = version_json.find("https://"
        if (pos == std::string::npos) break;
        size_t url_end = version_json.find("\"", pos);
        if (url_end == std::string::npos) break;
        std::string lib_url = version_json.substr(pos, url_end - pos);
        offset = url_end;

        std::string rel_path = lib_url.substr(32);
        std::string lib_dest = state->kickx_dir + "/libraries/" + rel_path;
        lib_urls.push_back(lib_url);
        lib_dests.push_back(lib_dest);
    }


    int total_libs = (int)lib_urls.size();
    queue_progress(state, ("Libraries [0/" + std::to_string(total_libs) + "]").c_str(), 0.12);
    parallelDownload(lib_urls, lib_dests);
    queue_progress(state, ("Libraries [" + std::to_string(total_libs) + "/" + std::to_string(total_libs) + "]").c_str(), 0.5);


    queue_progress(state, "Downloading asset index...", 0.52);

    size_t asset_index_pos = version_json.find("\"assetIndex\":");
    if (asset_index_pos != std::string::npos) {
        size_t asset_url_pos = version_json.find("\"url\":", asset_index_pos);
        size_t id_pos = version_json.find("\"id\":", asset_index_pos);
        if (asset_url_pos != std::string::npos && id_pos != std::string::npos) {
            start = version_json.find("\"", asset_url_pos + 6);
            end = version_json.find("\"", start + 1);
            std::string asset_url = version_json.substr(start + 1, end - start - 1);

            start = version_json.find("\"", id_pos + 5);
            end = version_json.find("\"", start + 1);
            std::string asset_id = version_json.substr(start + 1, end - start - 1);

            std::string asset_index_path = state->kickx_dir + "/assets/indexes/" + asset_id + ".json";
            http_download(asset_url, asset_index_path);


            queue_progress(state, "Scanning assets...", 0.55);

            std::ifstream in_asset_index(asset_index_path);
            if (in_asset_index.is_open()) {
                std::string asset_index_content((std::istreambuf_iterator<char>(in_asset_index)),
                                                  std::istreambuf_iterator<char>());
                in_asset_index.close();


                std::vector<std::string> asset_urls, asset_dests;
                size_t search_offset = 0;
                while (true) {
                    size_t hpos = asset_index_content.find("\"hash\": \"", search_offset);
                    if (hpos == std::string::npos) break;
                    size_t hash_start = hpos + 9;
                    size_t hash_end = asset_index_content.find("\"", hash_start);
                    if (hash_end == std::string::npos) break;
                    std::string hash = asset_index_content.substr(hash_start, hash_end - hash_start);
                    search_offset = hash_end;

                    std::string prefix = hash.substr(0, 2);
                    std::string obj_path = state->kickx_dir + "/assets/objects/" + prefix + "/" + hash;
                    struct stat st;
                    if (stat(obj_path.c_str(), &st) != 0) {
                        std::string url = "https://resources.download.minecraft.net/"
                        asset_urls.push_back(url);
                        asset_dests.push_back(obj_path);
                    }
                }

                size_t missing = asset_urls.size();
                if (missing > 0) {

                    std::string list_path = state->kickx_dir + "/.asset_list.txt";
                    {
                        std::ofstream list_out(list_path);
                        for (size_t i = 0; i < asset_urls.size(); i++) {
                            list_out << asset_urls[i] << " " << asset_dests[i] << "\n";
                        }
                    }



                    std::string cmd =
                        "xargs -P 32 -n 2 sh -c '"
                        "mkdir -p \"${2%

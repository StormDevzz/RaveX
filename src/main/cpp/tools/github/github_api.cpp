#include "github_api.h"
#include <sstream>
#include <array>
#include <memory>
#include <regex>
#include <cstdio>

namespace ravex {
namespace tools {
namespace github {

GithubAPI::GithubAPI(const std::string& token)
    : m_token(token), m_userAgent("RaveX-Tools/1.0")
{}

void GithubAPI::setToken(const std::string& token) { m_token = token; }
void GithubAPI::setUserAgent(const std::string& ua) { m_userAgent = ua; }

HeaderMap GithubAPI::defaultHeaders() {
    HeaderMap headers;
    headers["User-Agent"] = m_userAgent;
    headers["Accept"] = "application/vnd.github.v3+json";
    if (!m_token.empty()) {
        headers["Authorization"] = "token " + m_token;
    }
    return headers;
}

GithubRepo GithubAPI::getRepo(const std::string& owner, const std::string& repo) {
    std::string response = request("GET", "/repos/" + owner + "/" + repo);
    GithubRepo r;
    if (response.empty()) return r;

    std::smatch m;
    if (std::regex_search(response, m, std::regex("\"full_name\"\\s*:\\s*\"([^\"]+)\"")) && m.size() > 1)
        r.fullName = m[1].str();
    if (std::regex_search(response, m, std::regex("\"description\"\\s*:\\s*\"([^\"]+)\"")) && m.size() > 1)
        r.description = m[1].str();
    if (std::regex_search(response, m, std::regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"")) && m.size() > 1)
        r.htmlUrl = m[1].str();
    if (std::regex_search(response, m, std::regex("\"stargazers_count\"\\s*:\\s*(\\d+)")) && m.size() > 1)
        r.stars = std::stoi(m[1].str());
    if (std::regex_search(response, m, std::regex("\"forks_count\"\\s*:\\s*(\\d+)")) && m.size() > 1)
        r.forks = std::stoi(m[1].str());
    if (std::regex_search(response, m, std::regex("\"open_issues_count\"\\s*:\\s*(\\d+)")) && m.size() > 1)
        r.openIssues = std::stoi(m[1].str());
    if (std::regex_search(response, m, std::regex("\"archived\"\\s*:\\s*(true|false)")) && m.size() > 1)
        r.archived = m[1].str() == "true";

    r.owner = owner;
    r.name = repo;
    r.apiUrl = m_apiBase + "/repos/" + owner + "/" + repo;
    return r;
}

GithubRelease GithubAPI::getLatestRelease(const std::string& owner, const std::string& repo) {
    std::string response = request("GET", "/repos/" + owner + "/" + repo + "/releases/latest");
    GithubRelease rel;
    if (response.empty()) return rel;

    std::smatch m;
    if (std::regex_search(response, m, std::regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"")) && m.size() > 1)
        rel.tagName = m[1].str();
    if (std::regex_search(response, m, std::regex("\"name\"\\s*:\\s*\"([^\"]+)\"")) && m.size() > 1)
        rel.name = m[1].str();
    if (std::regex_search(response, m, std::regex("\"prerelease\"\\s*:\\s*(true|false)")) && m.size() > 1)
        rel.prerelease = m[1].str() == "true";
    if (std::regex_search(response, m, std::regex("\"published_at\"\\s*:\\s*\"([^\"]+)\"")) && m.size() > 1)
        rel.publishedAt = m[1].str();
    if (std::regex_search(response, m, std::regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"")) && m.size() > 1)
        rel.htmlUrl = m[1].str();
    return rel;
}

std::vector<GithubRelease> GithubAPI::listReleases(const std::string& owner, const std::string& repo, int perPage) {
    std::string path = "/repos/" + owner + "/" + repo + "/releases?per_page=" + std::to_string(perPage);
    std::string response = request("GET", path);
    std::vector<GithubRelease> releases;

    if (response.empty() || response == "[]") return releases;

    // Simple parse: split by "tag_name" occurrences
    std::regex tagRegex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    auto begin = std::sregex_iterator(response.begin(), response.end(), tagRegex);
    auto end = std::sregex_iterator();
    for (auto it = begin; it != end; ++it) {
        GithubRelease rel;
        rel.tagName = (*it)[1].str();
        releases.push_back(rel);
    }
    return releases;
}

std::string GithubAPI::request(const std::string& method, const std::string& path,
                               const std::string& body, HeaderMap extraHeaders)
{
    std::string url = m_apiBase + path;
    std::string headers;
    auto def = defaultHeaders();
    for (auto& [k, v] : def) headers += " -H \"" + k + ": " + v + "\"";
    for (auto& [k, v] : extraHeaders) headers += " -H \"" + k + ": " + v + "\"";

    std::string cmd;
    if (method == "GET") {
        cmd = "curl -s" + headers + " \"" + url + "\" 2>/dev/null";
    } else if (method == "POST") {
        cmd = "curl -s -X POST" + headers + " -d '" + body + "' \"" + url + "\" 2>/dev/null";
    }

    std::array<char, 8192> buffer{};
    std::string result;
    std::unique_ptr<FILE, decltype(&pclose)> pipe(popen(cmd.c_str(), "r"), pclose);
    if (!pipe) return "";
    while (std::fgets(buffer.data(), buffer.size(), pipe.get()) != nullptr)
        result += buffer.data();
    return result;
}

} // namespace github
} // namespace tools
} // namespace ravex

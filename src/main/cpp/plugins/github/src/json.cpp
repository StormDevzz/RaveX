#include "ravex/github/json.h"
#include <fstream>
#include <sstream>
#include <cstdlib>
#include <cmath>
#include <cstdio>

namespace ravex {
namespace github {

// ─── JsonValue implementation ────────────────────────────────────────────────

bool JsonValue::asBool() const {
    if (m_type != Bool) throw std::runtime_error("not a bool");
    return m_bool;
}

double JsonValue::asNumber() const {
    if (m_type != Number) throw std::runtime_error("not a number");
    return m_num;
}

std::string JsonValue::asString() const {
    if (m_type != String) throw std::runtime_error("not a string");
    return m_str;
}

const JsonArray& JsonValue::asArray() const {
    if (m_type != Array) throw std::runtime_error("not an array");
    return m_arr;
}

const JsonObject& JsonValue::asObject() const {
    if (m_type != Object) throw std::runtime_error("not an object");
    return m_obj;
}

const JsonValue& JsonValue::operator[](const std::string& key) const {
    if (m_type != Object) throw std::runtime_error("not an object");
    static JsonValue nullVal;
    auto it = m_obj.find(key);
    return it != m_obj.end() ? it->second : nullVal;
}

const JsonValue& JsonValue::operator[](size_t idx) const {
    if (m_type != Array) throw std::runtime_error("not an array");
    static JsonValue nullVal;
    return idx < m_arr.size() ? m_arr[idx] : nullVal;
}

const JsonValue& JsonValue::at(const std::string& key) const {
    if (m_type != Object) throw std::runtime_error("not an object");
    auto it = m_obj.find(key);
    if (it == m_obj.end()) throw std::runtime_error("key not found: " + key);
    return it->second;
}

const JsonValue& JsonValue::at(size_t idx) const {
    if (m_type != Array) throw std::runtime_error("not an array");
    if (idx >= m_arr.size()) throw std::runtime_error("index out of bounds");
    return m_arr[idx];
}

bool JsonValue::has(const std::string& key) const {
    return m_type == Object && m_obj.find(key) != m_obj.end();
}

size_t JsonValue::size() const {
    if (m_type == Array) return m_arr.size();
    if (m_type == Object) return m_obj.size();
    if (m_type == String) return m_str.size();
    return 0;
}

bool JsonValue::empty() const { return size() == 0; }

// ─── Parser ──────────────────────────────────────────────────────────────────

void JsonValue::Parser::skipWS() {
    while (p < end && (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\r')) p++;
}

char JsonValue::Parser::peek() { return p < end ? *p : '\0'; }
char JsonValue::Parser::next() { return p < end ? *p++ : '\0'; }
bool JsonValue::Parser::eof() { return p >= end; }

JsonValue JsonValue::Parser::parseValue() {
    skipWS();
    char c = peek();
    switch (c) {
        case '"': return parseString();
        case '{': return parseObject();
        case '[': return parseArray();
        case 't': case 'f': case 'n': return parseBoolOrNull();
        default:
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            throw std::runtime_error(std::string("unexpected char: ") + c);
    }
}

JsonValue JsonValue::Parser::parseString() {
    return JsonValue(parseRawString());
}

std::string JsonValue::Parser::parseRawString() {
    if (next() != '"') throw std::runtime_error("expected '\"'");
    std::string result;
    while (p < end) {
        char c = next();
        if (c == '"') return result;
        if (c == '\\') {
            char esc = next();
            switch (esc) {
                case '"': result += '"'; break;
                case '\\': result += '\\'; break;
                case '/': result += '/'; break;
                case 'b': result += '\b'; break;
                case 'f': result += '\f'; break;
                case 'n': result += '\n'; break;
                case 'r': result += '\r'; break;
                case 't': result += '\t'; break;
                case 'u': {
                    // 4-digit hex
                    char hex[5] = { next(), next(), next(), next(), '\0' };
                    unsigned long cp = strtoul(hex, nullptr, 16);
                    if (cp <= 0x7F) {
                        result += (char)cp;
                    } else if (cp <= 0x7FF) {
                        result += (char)(0xC0 | (cp >> 6));
                        result += (char)(0x80 | (cp & 0x3F));
                    } else if (cp <= 0xFFFF) {
                        result += (char)(0xE0 | (cp >> 12));
                        result += (char)(0x80 | ((cp >> 6) & 0x3F));
                        result += (char)(0x80 | (cp & 0x3F));
                    }
                    break;
                }
                default: result += esc;
            }
        } else {
            result += c;
        }
    }
    throw std::runtime_error("unterminated string");
}

JsonValue JsonValue::Parser::parseNumber() {
    const char* start = p;
    if (p < end && *p == '-') p++;
    while (p < end && *p >= '0' && *p <= '9') p++;
    if (p < end && *p == '.') { p++; while (p < end && *p >= '0' && *p <= '9') p++; }
    if (p < end && (*p == 'e' || *p == 'E')) {
        p++; if (p < end && (*p == '+' || *p == '-')) p++;
        while (p < end && *p >= '0' && *p <= '9') p++;
    }
    char* endPtr;
    double val = strtod(start, &endPtr);
    p = endPtr;
    return JsonValue(val);
}

JsonValue JsonValue::Parser::parseObject() {
    next(); // skip '{'
    JsonObject obj;
    skipWS();
    if (peek() == '}') { next(); return JsonValue(obj); }
    while (true) {
        skipWS();
        std::string key = parseRawString();
        skipWS();
        if (next() != ':') throw std::runtime_error("expected ':'");
        obj[key] = parseValue();
        skipWS();
        char c = next();
        if (c == '}') break;
        if (c != ',') throw std::runtime_error("expected ',' or '}'");
    }
    JsonValue v;
    v.m_type = Object;
    v.m_obj = std::move(obj);
    return v;
}

JsonValue JsonValue::Parser::parseArray() {
    next(); // skip '['
    JsonArray arr;
    skipWS();
    if (peek() == ']') { next(); JsonValue v; v.m_type = Array; v.m_arr = std::move(arr); return v; }
    while (true) {
        arr.push_back(parseValue());
        skipWS();
        char c = next();
        if (c == ']') break;
        if (c != ',') throw std::runtime_error("expected ',' or ']'");
    }
    JsonValue v;
    v.m_type = Array;
    v.m_arr = std::move(arr);
    return v;
}

JsonValue JsonValue::Parser::parseBoolOrNull() {
    const char* saved = p;
    if (end - p >= 4 && p[0] == 't' && p[1] == 'r' && p[2] == 'u' && p[3] == 'e') { p += 4; return JsonValue(true); }
    p = saved;
    if (end - p >= 5 && p[0] == 'f' && p[1] == 'a' && p[2] == 'l' && p[3] == 's' && p[4] == 'e') { p += 5; return JsonValue(false); }
    p = saved;
    if (end - p >= 4 && p[0] == 'n' && p[1] == 'u' && p[2] == 'l' && p[3] == 'l') { p += 4; return JsonValue(); }
    throw std::runtime_error("expected bool or null");
}

JsonValue JsonValue::parse(const std::string& json) {
    return parse(json.data(), json.size());
}

JsonValue JsonValue::parse(const char* data, size_t len) {
    Parser parser(data, len);
    auto val = parser.parseValue();
    parser.skipWS();
    if (!parser.eof()) {
        // Might have trailing whitespace or single object — ok
    }
    return val;
}

JsonValue JsonValue::parseFile(const std::string& path) {
    std::ifstream ifs(path, std::ios::binary);
    if (!ifs) throw std::runtime_error("cannot open file: " + path);
    std::string content((std::istreambuf_iterator<char>(ifs)),
                         std::istreambuf_iterator<char>());
    return parse(content);
}

// ─── Serializer ──────────────────────────────────────────────────────────────

static std::string escapeJson(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 2);
    out += '"';
    for (char c : s) {
        switch (c) {
            case '"':  out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\b': out += "\\b"; break;
            case '\f': out += "\\f"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    char buf[8]; std::snprintf(buf, sizeof(buf), "\\u%04x", c);
                    out += buf;
                } else {
                    out += c;
                }
        }
    }
    out += '"';
    return out;
}

std::string JsonValue::serialize(bool pretty, int indent) const {
    std::string ws; if (pretty) ws.assign(indent * 2, ' ');
    std::string newl = pretty ? "\n" : "";

    switch (m_type) {
        case Null:   return "null";
        case Bool:   return m_bool ? "true" : "false";
        case Number: {
            char buf[64];
            if (m_num == std::floor(m_num) && std::isfinite(m_num))
                std::snprintf(buf, sizeof(buf), "%.0f", m_num);
            else
                std::snprintf(buf, sizeof(buf), "%.17g", m_num);
            return buf;
        }
        case String: return escapeJson(m_str);
        case Array: {
            if (m_arr.empty()) return "[]";
            std::string s = "[" + newl;
            for (size_t i = 0; i < m_arr.size(); i++) {
                if (i > 0) s += "," + newl;
                if (pretty) s += ws + "  ";
                s += m_arr[i].serialize(pretty, indent + 1);
            }
            s += newl + ws + "]";
            return s;
        }
        case Object: {
            if (m_obj.empty()) return "{}";
            std::string s = "{" + newl;
            bool first = true;
            for (auto& [k, v] : m_obj) {
                if (!first) s += "," + newl;
                first = false;
                if (pretty) s += ws + "  ";
                s += escapeJson(k) + ": " + v.serialize(pretty, indent + 1);
            }
            s += newl + ws + "}";
            return s;
        }
    }
    return "null";
}

} // namespace github
} // namespace ravex

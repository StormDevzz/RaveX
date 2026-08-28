#include "net/include/json.hpp"
#include <stdexcept>

namespace ravex::json {

const Value Value::nullValue_{};

Value Value::null() {
    return Value{};
}

Value::Type Value::type() const { return type_; }
bool Value::isNull() const { return type_ == Type::Null; }
bool Value::asBool() const { return boolValue_; }
double Value::asNumber() const { return numValue_; }
const std::string& Value::asString() const { return strValue_; }
std::size_t Value::size() const {
    if (type_ == Type::Array) return items_.size();
    if (type_ == Type::Object) return members_.size();
    return 0;
}
bool Value::has(const std::string& key) const {
    if (type_ != Type::Object) return false;
    for (auto& kv : members_) if (kv.first == key) return true;
    return false;
}
const Value& Value::at(const std::string& key) const {
    if (type_ == Type::Object) {
        for (auto& kv : members_) if (kv.first == key) return kv.second;
    }
    return nullValue_;
}
const Value& Value::at(std::size_t index) const {
    if (type_ == Type::Array && index < items_.size()) return items_[index];
    return nullValue_;
}
std::vector<std::string> Value::keys() const {
    std::vector<std::string> out;
    if (type_ != Type::Object) return out;
    out.reserve(members_.size());
    for (auto& kv : members_) out.push_back(kv.first);
    return out;
}

namespace {

class Parser {
public:
    explicit Parser(const std::string& t) : text(t) {}
    Value run() {
        skip();
        Value v;
        if (!parseValue(v)) throw std::runtime_error("json parse error");
        skip();
        if (pos != text.size()) throw std::runtime_error("json trailing data");
        return v;
    }
private:
    const std::string& text;
    std::size_t pos = 0;
    void skip() {
        while (pos < text.size() && (text[pos] == ' ' || text[pos] == '\t' || text[pos] == '\n' || text[pos] == '\r')) ++pos;
    }
    bool parseValue(Value& out) {
        skip();
        if (pos >= text.size()) return false;
        char c = text[pos];
        if (c == '{') return parseObject(out);
        if (c == '[') return parseArray(out);
        if (c == '"') {
            std::string s;
            if (!parseString(s)) return false;
            out.type_ = Value::Type::String;
            out.strValue_ = std::move(s);
            return true;
        }
        if (c == 't') {
            if (text.compare(pos, 4, "true") != 0) return false;
            pos += 4;
            out.type_ = Value::Type::Bool;
            out.boolValue_ = true;
            return true;
        }
        if (c == 'f') {
            if (text.compare(pos, 5, "false") != 0) return false;
            pos += 5;
            out.type_ = Value::Type::Bool;
            out.boolValue_ = false;
            return true;
        }
        if (c == 'n') {
            if (text.compare(pos, 4, "null") != 0) return false;
            pos += 4;
            out.type_ = Value::Type::Null;
            return true;
        }
        if (c == '-' || (c >= '0' && c <= '9')) return parseNumber(out);
        return false;
    }
    bool parseString(std::string& out) {
        if (text[pos] != '"') return false;
        ++pos;
        out.clear();
        while (pos < text.size()) {
            char ch = text[pos];
            if (ch == '"') { ++pos; return true; }
            if (ch == '\\') {
                ++pos;
                if (pos >= text.size()) return false;
                char e = text[pos];
                switch (e) {
                    case '"': out.push_back('"'); break;
                    case '\\': out.push_back('\\'); break;
                    case '/': out.push_back('/'); break;
                    case 'b': out.push_back('\b'); break;
                    case 'f': out.push_back('\f'); break;
                    case 'n': out.push_back('\n'); break;
                    case 'r': out.push_back('\r'); break;
                    case 't': out.push_back('\t'); break;
                    case 'u': {
                        if (pos + 4 >= text.size()) return false;
                        unsigned int cp = 0;
                        for (int i = 1; i <= 4; ++i) {
                            char h = text[pos + i];
                            cp <<= 4;
                            if (h >= '0' && h <= '9') cp |= h - '0';
                            else if (h >= 'a' && h <= 'f') cp |= h - 'a' + 10;
                            else if (h >= 'A' && h <= 'F') cp |= h - 'A' + 10;
                            else return false;
                        }
                        pos += 4;
                        if (cp < 0x80) out.push_back(static_cast<char>(cp));
                        else if (cp < 0x800) {
                            out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
                            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
                        } else if (cp < 0xD800 || cp >= 0xE000) {
                            out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
                            out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
                            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
                        } else {
                            if (pos + 6 >= text.size()) return false;
                            if (text[pos + 1] != '\\' || text[pos + 2] != 'u') return false;
                            unsigned int low = 0;
                            for (int i = 3; i <= 6; ++i) {
                                char h = text[pos + i];
                                low <<= 4;
                                if (h >= '0' && h <= '9') low |= h - '0';
                                else if (h >= 'a' && h <= 'f') low |= h - 'a' + 10;
                                else if (h >= 'A' && h <= 'F') low |= h - 'A' + 10;
                                else return false;
                            }
                            unsigned int full = 0x10000 + ((cp - 0xD800) * 0x400) + (low - 0xDC00);
                            out.push_back(static_cast<char>(0xF0 | (full >> 18)));
                            out.push_back(static_cast<char>(0x80 | ((full >> 12) & 0x3F)));
                            out.push_back(static_cast<char>(0x80 | ((full >> 6) & 0x3F)));
                            out.push_back(static_cast<char>(0x80 | (full & 0x3F)));
                            pos += 6;
                        }
                        break;
                    }
                    default: return false;
                }
                ++pos;
            } else {
                out.push_back(ch);
                ++pos;
            }
        }
        return false;
    }
    bool parseNumber(Value& out) {
        std::size_t start = pos;
        if (pos < text.size() && text[pos] == '-') ++pos;
        while (pos < text.size() && text[pos] >= '0' && text[pos] <= '9') ++pos;
        if (pos < text.size() && text[pos] == '.') {
            ++pos;
            while (pos < text.size() && text[pos] >= '0' && text[pos] <= '9') ++pos;
        }
        if (pos < text.size() && (text[pos] == 'e' || text[pos] == 'E')) {
            ++pos;
            if (pos < text.size() && (text[pos] == '+' || text[pos] == '-')) ++pos;
            while (pos < text.size() && text[pos] >= '0' && text[pos] <= '9') ++pos;
        }
        if (pos == start) return false;
        out.type_ = Value::Type::Number;
        out.numValue_ = std::stod(text.substr(start, pos - start));
        return true;
    }
    bool parseObject(Value& out) {
        ++pos;
        out.type_ = Value::Type::Object;
        skip();
        if (pos < text.size() && text[pos] == '}') { ++pos; return true; }
        while (true) {
            skip();
            std::string key;
            if (!parseString(key)) return false;
            skip();
            if (pos >= text.size() || text[pos] != ':') return false;
            ++pos;
            Value v;
            if (!parseValue(v)) return false;
            out.members_.emplace_back(std::move(key), std::move(v));
            skip();
            if (pos >= text.size()) return false;
            if (text[pos] == ',') { ++pos; continue; }
            if (text[pos] == '}') { ++pos; return true; }
            return false;
        }
    }
    bool parseArray(Value& out) {
        ++pos;
        out.type_ = Value::Type::Array;
        skip();
        if (pos < text.size() && text[pos] == ']') { ++pos; return true; }
        while (true) {
            Value v;
            if (!parseValue(v)) return false;
            out.items_.push_back(std::move(v));
            skip();
            if (pos >= text.size()) return false;
            if (text[pos] == ',') { ++pos; continue; }
            if (text[pos] == ']') { ++pos; return true; }
            return false;
        }
    }
};

}

Value Value::parse(const std::string& text) {
    return Parser(text).run();
}

}

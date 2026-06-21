#pragma once

#include <string>
#include <vector>
#include <map>
#include <variant>
#include <stdexcept>

namespace ravex {
namespace github {

// ─── Minimal JSON parser (no external dependencies) ─────────────────────────
// Supports: object, array, string, number, bool, null.
// Used internally by the GitHub HTTP client. Not a general-purpose parser.

class JsonValue;

using JsonObject = std::map<std::string, JsonValue>;
using JsonArray  = std::vector<JsonValue>;

class JsonValue {
public:
    enum Type { Null, Bool, Number, String, Array, Object };

    JsonValue() : m_type(Null), m_num(0) {}
    JsonValue(Type t) : m_type(t), m_num(0) {}
    JsonValue(bool b) : m_type(Bool), m_bool(b), m_num(0) {}
    JsonValue(double n) : m_type(Number), m_num(n) {}
    JsonValue(int n) : m_type(Number), m_num(static_cast<double>(n)) {}
    JsonValue(int64_t n) : m_type(Number), m_num(static_cast<double>(n)) {}
    JsonValue(const char* s) : m_type(String), m_str(s), m_num(0) {}
    JsonValue(const std::string& s) : m_type(String), m_str(s), m_num(0) {}

    Type type() const { return m_type; }
    bool isNull()   const { return m_type == Null; }
    bool isBool()   const { return m_type == Bool; }
    bool isNumber() const { return m_type == Number; }
    bool isString() const { return m_type == String; }
    bool isArray()  const { return m_type == Array; }
    bool isObject() const { return m_type == Object; }

    bool asBool()   const;
    double asNumber() const;
    int64_t asInt() const { return static_cast<int64_t>(asNumber()); }
    std::string asString() const;
    const JsonArray&  asArray()  const;
    const JsonObject& asObject() const;

    const JsonValue& operator[](const std::string& key) const;
    const JsonValue& operator[](size_t idx) const;
    const JsonValue& at(const std::string& key) const;
    const JsonValue& at(size_t idx) const;

    bool has(const std::string& key) const;

    size_t size() const;
    bool empty() const;

    // Parse JSON string → JsonValue
    static JsonValue parse(const std::string& json);
    static JsonValue parse(const char* data, size_t len);
    static JsonValue parseFile(const std::string& path);

    // Serialize → string
    std::string serialize(bool pretty = false, int indent = 0) const;

private:
    Type m_type;
    bool m_bool = false;
    double m_num = 0.0;
    std::string m_str;
    JsonArray  m_arr;
    JsonObject m_obj;

    struct Parser {
        const char* p;
        const char* end;
        Parser(const char* b, size_t len) : p(b), end(b + len) {}
        void skipWS();
        char peek();
        char next();
        bool eof();
        JsonValue parseValue();
        JsonValue parseString();
        JsonValue parseNumber();
        JsonValue parseObject();
        JsonValue parseArray();
        JsonValue parseBoolOrNull();
        std::string parseRawString();
    };
};

} // namespace github
} // namespace ravex

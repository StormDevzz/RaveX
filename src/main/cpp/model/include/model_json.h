#pragma once

#include <string>
#include <vector>
#include <map>
#include <cstdlib>
#include <cstring>

namespace model {
namespace json {

struct Value {
    enum Type { Null, Bool, Number, String, Array, Object } type = Null;
    bool b = false;
    double n = 0;
    std::string s;
    std::vector<Value> arr;
    std::map<std::string, Value> obj;

    bool isNull() const { return type == Null; }
    bool isBool() const { return type == Bool; }
    bool isNumber() const { return type == Number; }
    bool isString() const { return type == String; }
    bool isArray() const { return type == Array; }
    bool isObject() const { return type == Object; }

    int asInt() const { return static_cast<int>(n); }
    float asFloat() const { return static_cast<float>(n); }

    const Value& get(const std::string& k) const {
        static Value nullVal;
        if (type != Object) return nullVal;
        auto it = obj.find(k);
        return it != obj.end() ? it->second : nullVal;
    }

    const Value& operator[](size_t i) const {
        static Value nullVal;
        return (type == Array && i < arr.size()) ? arr[i] : nullVal;
    }
};

Value parse(const std::string& input);
std::string serialize(const Value& v, int indent = 0);

namespace detail {
    Value parseValue(const char*& p);
    std::string parseString(const char*& p);
}

} // namespace json
} // namespace model

#pragma once
#include <string>
#include <vector>

namespace ravex::json {

class Value {
public:
    enum class Type { Null, Bool, Number, String, Array, Object };
    static Value parse(const std::string& text);
    static Value null();
    Type type() const;
    bool isNull() const;
    bool asBool() const;
    double asNumber() const;
    const std::string& asString() const;
    std::size_t size() const;
    bool has(const std::string& key) const;
    const Value& at(const std::string& key) const;
    const Value& at(std::size_t index) const;
    std::vector<std::string> keys() const;
    Type type_ = Type::Null;
    bool boolValue_ = false;
    double numValue_ = 0;
    std::string strValue_;
    std::vector<Value> items_;
    std::vector<std::pair<std::string, Value>> members_;
    static const Value nullValue_;
};

}

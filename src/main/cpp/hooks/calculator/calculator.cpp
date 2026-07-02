#include "calculator.hpp"
#include <string>
#include <vector>
#include <stack>
#include <map>
#include <cmath>
#include <sstream>
#include <stdexcept>
#include <algorithm>
#include <cctype>
#include <iomanip>

namespace ravex {





enum class TokType { NUMBER, OP, FUNC, LPAREN, RPAREN, COMMA };

struct Token {
    TokType type;
    double  num   = 0;
    char    op    = 0;
    std::string func;
};

static bool isLetter(char c) { return std::isalpha((unsigned char)c) || c == '_'; }

static std::vector<Token> tokenize(const std::string& expr) {
    std::vector<Token> tokens;
    size_t i = 0;
    bool lastWasNum = false; 

    while (i < expr.size()) {
        char c = expr[i];

        
        if (std::isspace((unsigned char)c)) { i++; continue; }

        
        if (std::isdigit((unsigned char)c) || c == '.') {
            size_t start = i;
            while (i < expr.size() && (std::isdigit((unsigned char)expr[i]) || expr[i] == '.')) i++;
            
            if (i < expr.size() && (expr[i] == 'e' || expr[i] == 'E')) {
                i++;
                if (i < expr.size() && (expr[i] == '+' || expr[i] == '-')) i++;
                while (i < expr.size() && std::isdigit((unsigned char)expr[i])) i++;
            }
            Token t; t.type = TokType::NUMBER;
            t.num = std::stod(expr.substr(start, i - start));
            tokens.push_back(t); lastWasNum = true; continue;
        }

        
        if (isLetter(c)) {
            size_t start = i;
            while (i < expr.size() && (isLetter(expr[i]) || std::isdigit((unsigned char)expr[i]))) i++;
            std::string word = expr.substr(start, i - start);
            
            std::transform(word.begin(), word.end(), word.begin(), ::tolower);

            
            double constVal = 0; bool isConst = false;
            if (word == "pi")  { constVal = M_PI; isConst = true; }
            else if (word == "e")   { constVal = M_E;  isConst = true; }
            else if (word == "tau") { constVal = 2.0 * M_PI; isConst = true; }
            else if (word == "phi") { constVal = 1.6180339887498948; isConst = true; }
            else if (word == "inf") { constVal = std::numeric_limits<double>::infinity(); isConst = true; }

            if (isConst) {
                Token t; t.type = TokType::NUMBER; t.num = constVal;
                tokens.push_back(t); lastWasNum = true;
            } else {
                Token t; t.type = TokType::FUNC; t.func = word;
                tokens.push_back(t); lastWasNum = false;
            }
            continue;
        }

        
        if (c == '(') {
            Token t; t.type = TokType::LPAREN;
            tokens.push_back(t); i++; lastWasNum = false; continue;
        }
        if (c == ')') {
            Token t; t.type = TokType::RPAREN;
            tokens.push_back(t); i++; lastWasNum = true; continue;
        }
        if (c == ',') {
            Token t; t.type = TokType::COMMA;
            tokens.push_back(t); i++; lastWasNum = false; continue;
        }

        
        if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '^') {
            Token t; t.type = TokType::OP;
            
            if (c == '-' && !lastWasNum) {
                t.op = 'u'; 
            } else {
                t.op = c;
            }
            tokens.push_back(t); i++; lastWasNum = false; continue;
        }

        
        i++;
    }
    return tokens;
}





static int precedence(char op) {
    switch (op) {
        case '+': case '-': return 1;
        case '*': case '/': case '%': return 2;
        case '^': return 3;
        case 'u': return 4; 
        default: return 0;
    }
}

static bool rightAssoc(char op) { return op == '^' || op == 'u'; }

static double applyOp(char op, double a, double b) {
    switch (op) {
        case '+': return a + b;
        case '-': return a - b;
        case '*': return a * b;
        case '/':
            if (b == 0) throw std::runtime_error("Division by zero");
            return a / b;
        case '%':
            if (b == 0) throw std::runtime_error("Modulo by zero");
            return std::fmod(a, b);
        case '^': return std::pow(a, b);
        default: throw std::runtime_error(std::string("Unknown op: ") + op);
    }
}

static double applyFunc(const std::string& f, const std::vector<double>& args) {
    auto need = [&](size_t n) {
        if (args.size() != n) throw std::runtime_error("Wrong arg count for " + f);
    };
    if (f == "sin")    { need(1); return std::sin(args[0]); }
    if (f == "cos")    { need(1); return std::cos(args[0]); }
    if (f == "tan")    { need(1); return std::tan(args[0]); }
    if (f == "asin")   { need(1); return std::asin(args[0]); }
    if (f == "acos")   { need(1); return std::acos(args[0]); }
    if (f == "atan")   { need(1); return std::atan(args[0]); }
    if (f == "atan2")  { need(2); return std::atan2(args[0], args[1]); }
    if (f == "sinh")   { need(1); return std::sinh(args[0]); }
    if (f == "cosh")   { need(1); return std::cosh(args[0]); }
    if (f == "tanh")   { need(1); return std::tanh(args[0]); }
    if (f == "sqrt")   { need(1); if(args[0]<0) throw std::runtime_error("sqrt of negative"); return std::sqrt(args[0]); }
    if (f == "cbrt")   { need(1); return std::cbrt(args[0]); }
    if (f == "abs")    { need(1); return std::abs(args[0]); }
    if (f == "ceil")   { need(1); return std::ceil(args[0]); }
    if (f == "floor")  { need(1); return std::floor(args[0]); }
    if (f == "round")  { need(1); return std::round(args[0]); }
    if (f == "log" || f == "ln")  { need(1); if(args[0]<=0) throw std::runtime_error("log of non-positive"); return std::log(args[0]); }
    if (f == "log2")   { need(1); if(args[0]<=0) throw std::runtime_error("log2 of non-positive"); return std::log2(args[0]); }
    if (f == "log10")  { need(1); if(args[0]<=0) throw std::runtime_error("log10 of non-positive"); return std::log10(args[0]); }
    if (f == "exp")    { need(1); return std::exp(args[0]); }
    if (f == "pow")    { need(2); return std::pow(args[0], args[1]); }
    if (f == "min")    { need(2); return std::min(args[0], args[1]); }
    if (f == "max")    { need(2); return std::max(args[0], args[1]); }
    if (f == "rad")    { need(1); return args[0] * M_PI / 180.0; }
    if (f == "deg")    { need(1); return args[0] * 180.0 / M_PI; }
    if (f == "sign")   { need(1); return (args[0] > 0) - (args[0] < 0); }
    if (f == "fact" || f == "factorial") {
        need(1);
        int n = (int)args[0];
        if (n < 0) throw std::runtime_error("Factorial of negative");
        if (n > 20) throw std::runtime_error("Factorial too large");
        double result = 1;
        for (int k = 2; k <= n; k++) result *= k;
        return result;
    }
    throw std::runtime_error("Unknown function: " + f);
}

static double shuntingYard(const std::vector<Token>& tokens) {
    std::stack<Token> opStack;
    std::stack<double> output;
    std::stack<int> argCount;

    auto popOp = [&]() {
        Token op = opStack.top(); opStack.pop();
        if (op.type == TokType::FUNC) {
            int argc = argCount.top(); argCount.pop();
            std::vector<double> args(argc);
            for (int k = argc - 1; k >= 0; k--) {
                if (output.empty()) throw std::runtime_error("Too few arguments");
                args[k] = output.top(); output.pop();
            }
            output.push(applyFunc(op.func, args));
        } else if (op.op == 'u') {
            if (output.empty()) throw std::runtime_error("Missing operand for unary minus");
            double a = output.top(); output.pop();
            output.push(-a);
        } else {
            if (output.size() < 2) throw std::runtime_error("Too few operands");
            double b = output.top(); output.pop();
            double a = output.top(); output.pop();
            output.push(applyOp(op.op, a, b));
        }
    };

    for (const Token& tok : tokens) {
        switch (tok.type) {
            case TokType::NUMBER:
                output.push(tok.num);
                break;

            case TokType::FUNC:
                opStack.push(tok);
                argCount.push(1);
                break;

            case TokType::COMMA:
                while (!opStack.empty() && opStack.top().type != TokType::LPAREN) popOp();
                if (!argCount.empty()) argCount.top()++;
                break;

            case TokType::OP: {
                while (!opStack.empty() &&
                       opStack.top().type == TokType::OP &&
                       ((!rightAssoc(tok.op) && precedence(tok.op) <= precedence(opStack.top().op)) ||
                        (rightAssoc(tok.op) && precedence(tok.op) < precedence(opStack.top().op)))) {
                    popOp();
                }
                opStack.push(tok);
                break;
            }

            case TokType::LPAREN:
                opStack.push(tok);
                break;

            case TokType::RPAREN:
                while (!opStack.empty() && opStack.top().type != TokType::LPAREN) popOp();
                if (opStack.empty()) throw std::runtime_error("Mismatched parentheses");
                opStack.pop(); 
                if (!opStack.empty() && opStack.top().type == TokType::FUNC) popOp();
                break;
        }
    }

    while (!opStack.empty()) {
        if (opStack.top().type == TokType::LPAREN) throw std::runtime_error("Mismatched parentheses");
        popOp();
    }

    if (output.empty()) throw std::runtime_error("Empty expression");
    if (output.size() > 1) throw std::runtime_error("Too many values");
    return output.top();
}





std::string MathParser::evaluate(const std::string& expr) {
    if (expr.empty()) return "";
    try {
        auto tokens = tokenize(expr);
        if (tokens.empty()) return "0";
        double result = shuntingYard(tokens);

        
        if (std::isinf(result)) return result > 0 ? "∞" : "-∞";
        if (std::isnan(result)) return "NaN";

        std::ostringstream oss;
        if (result == std::floor(result) && std::abs(result) < 1e15) {
            oss << std::fixed << std::setprecision(0) << result;
        } else {
            oss << std::setprecision(12) << result;
            
            std::string s = oss.str();
            if (s.find('.') != std::string::npos) {
                size_t last = s.find_last_not_of('0');
                if (s[last] == '.') last--;
                s = s.substr(0, last + 1);
            }
            return s;
        }
        return oss.str();
    } catch (const std::exception& ex) {
        return std::string("Error: ") + ex.what();
    }
}

} 

#pragma once
#include <string>

namespace ravex {

/**
 * Full-featured math expression parser using Shunting-yard algorithm.
 * Supports: +, -, *, /, %, ^ (power), unary minus
 * Functions: sin, cos, tan, asin, acos, atan, sqrt, cbrt, log, log2, log10,
 *            abs, ceil, floor, round, exp, factorial, rad, deg
 * Constants: pi, e, tau, phi
 * Parentheses and correct operator precedence.
 */
class MathParser {
public:
    /**
     * Evaluate a math expression string.
     * Returns the result as a string (formatted), or an error message prefixed with "Error: ".
     */
    static std::string evaluate(const std::string& expr);
};

} // namespace ravex

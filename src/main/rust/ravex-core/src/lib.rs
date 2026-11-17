//! RaveX Rust core - first ported subsystem: search (translit, layouts, diacritics).
//! One-to-one port of ravex.utility.misc.SearchUtility Java implementation.
//! Exposed to Java through JNI, class ravex.utility.misc.SearchNative.

use jni::JNIEnv;
use jni::objects::{JClass, JDoubleArray, JString};
use jni::sys::{jboolean, jobjectArray};
use std::sync::OnceLock;
use unicode_normalization::UnicodeNormalization;
use unicode_normalization::char::is_combining_mark;

const EN_KEYS: &[char] = &[
    '`', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', 'a', 's', 'd', 'f', 'g', 'h',
    'j', 'k', 'l', ';', '\'', 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/',
];

const RU_KEYS: &[char] = &[
    'ё', 'й', 'ц', 'у', 'к', 'е', 'н', 'г', 'ш', 'щ', 'з', 'х', 'ъ', 'ф', 'ы', 'в', 'а', 'п', 'р',
    'о', 'л', 'д', 'ж', 'э', 'я', 'ч', 'с', 'м', 'и', 'т', 'ь', 'б', 'ю', '.',
];

// (from, to) pairs, scanned in the same order as the Java version
const TRANSLIT: &[(&str, &str)] = &[
    ("а", "a"), ("б", "b"), ("в", "v"), ("г", "g"), ("д", "d"), ("е", "e"), ("ё", "e"),
    ("ж", "zh"), ("з", "z"), ("и", "i"), ("й", "i"), ("к", "k"), ("л", "l"), ("м", "m"),
    ("н", "n"), ("о", "o"), ("п", "p"), ("р", "r"), ("с", "s"), ("т", "t"), ("у", "u"),
    ("ф", "f"), ("х", "h"), ("ц", "c"), ("ч", "ch"), ("ш", "sh"), ("щ", "sh"), ("ъ", ""),
    ("ы", "y"), ("ь", ""), ("э", "e"), ("ю", "yu"), ("я", "ya"),
    ("і", "i"), ("ї", "i"), ("є", "ye"), ("ґ", "g"), ("ў", "u"),
];

fn lower(s: &str) -> String {
    s.to_lowercase()
}

fn strip_diacritics(s: &str) -> String {
    if s.is_empty() {
        return String::new();
    }
    let mut out = String::with_capacity(s.len());
    // Java: NFD normalize, strip combining marks, then the replacement chain
    for c in s.nfd() {
        if !is_combining_mark(c) {
            out.push(c);
        }
    }
    out.replace("ß", "ss")
        .replace("æ", "ae")
        .replace("Æ", "ae")
        .replace("ø", "o")
        .replace("Ø", "o")
        .replace("å", "a")
        .replace("Å", "a")
        .replace("đ", "d")
        .replace("ł", "l")
        .replace("ı", "i")
        .replace("ş", "s")
        .replace("ț", "t")
}

fn map_keys(s: &str, ru_to_en: bool) -> String {
    if s.is_empty() {
        return String::new();
    }
    let (from, to): (&[char], &[char]) = if ru_to_en {
        (RU_KEYS, EN_KEYS)
    } else {
        (EN_KEYS, RU_KEYS)
    };
    let mut out = String::with_capacity(s.len());
    for c in s.chars() {
        let c = c.to_lowercase().next().unwrap_or(c);
        let idx = from.iter().position(|k| *k == c);
        match idx {
            Some(i) => out.push(to[i]),
            None => out.push(c),
        }
    }
    out
}

fn translit(s: &str) -> String {
    if s.is_empty() {
        return String::new();
    }
    let lower = lower(s);
    let mut out = String::with_capacity(lower.len());
    let mut i = 0usize;
    while i < lower.len() {
        let rest = &lower[i..];
        let mut hit = false;
        for (from, to) in TRANSLIT {
            if rest.starts_with(from) {
                out.push_str(to);
                i += from.len();
                hit = true;
                break;
            }
        }
        if !hit {
            let c = lower[i..].chars().next().unwrap();
            out.push(c);
            i += c.len_utf8();
        }
    }
    out
}

fn swap_sha_scha(s: &str) -> String {
    if s.is_empty() {
        return s.to_string();
    }
    let mut out = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            'ш' => out.push('щ'),
            'щ' => out.push('ш'),
            other => out.push(other),
        }
    }
    out
}

fn add_variant(list: &mut Vec<String>, v: &str) {
    let s = lower(v);
    if !s.is_empty() && !list.contains(&s) {
        list.push(s);
    }
}

fn build_variants(q: &str) -> Vec<String> {
    let mut variants: Vec<String> = Vec::new();
    if q.is_empty() {
        return variants;
    }
    add_variant(&mut variants, q);
    add_variant(&mut variants, &strip_diacritics(q));
    add_variant(&mut variants, &map_keys(q, true));
    add_variant(&mut variants, &map_keys(q, false));
    add_variant(&mut variants, &translit(q));
    add_variant(&mut variants, &translit(&map_keys(q, true)));
    add_variant(&mut variants, &strip_diacritics(&map_keys(q, true)));
    let swapped = swap_sha_scha(q);
    if swapped != q {
        add_variant(&mut variants, &map_keys(&swapped, true));
        add_variant(&mut variants, &translit(&map_keys(&swapped, true)));
        add_variant(&mut variants, &strip_diacritics(&map_keys(&swapped, true)));
    }
    variants
}

// ---- JNI exports (class ravex.utility.misc.SearchNative) ----

fn read_string(env: &mut JNIEnv, s: &JString) -> String {
    match env.get_string(s) {
        Ok(js) => js.to_string_lossy().into_owned(),
        Err(_) => String::new(),
    }
}

fn return_string<'local>(env: &mut JNIEnv<'local>, s: String) -> JString<'local> {
    env.new_string(s)
        .unwrap_or_else(|_| env.new_string("").expect("fallback string"))
}

#[no_mangle]
pub extern "system" fn Java_ravex_utility_misc_SearchNative_nativeLower<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    s: JString<'local>,
) -> JString<'local> {
    let value = read_string(&mut env, &s);
    return_string(&mut env, lower(&value))
}

#[no_mangle]
pub extern "system" fn Java_ravex_utility_misc_SearchNative_nativeStripDiacritics<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    s: JString<'local>,
) -> JString<'local> {
    let value = read_string(&mut env, &s);
    return_string(&mut env, strip_diacritics(&value))
}

#[no_mangle]
pub extern "system" fn Java_ravex_utility_misc_SearchNative_nativeMapKeys<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    s: JString<'local>,
    ru_to_en: jboolean,
) -> JString<'local> {
    let value = read_string(&mut env, &s);
    return_string(&mut env, map_keys(&value, ru_to_en != 0))
}

#[no_mangle]
pub extern "system" fn Java_ravex_utility_misc_SearchNative_nativeTranslit<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    s: JString<'local>,
) -> JString<'local> {
    let value = read_string(&mut env, &s);
    return_string(&mut env, translit(&value))
}

#[no_mangle]
pub extern "system" fn Java_ravex_utility_misc_SearchNative_nativeSwapShaScha<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    s: JString<'local>,
) -> JString<'local> {
    let value = read_string(&mut env, &s);
    return_string(&mut env, swap_sha_scha(&value))
}

#[no_mangle]
pub extern "system" fn Java_ravex_utility_misc_SearchNative_nativeQueryVariants<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    query: JString<'local>,
) -> jobjectArray {
    let q = read_string(&mut env, &query);
    // Java's SearchUtility.queryVariants() lowers the query BEFORE building
    // variants (chain functions assume lowered input); mirror that here.
    let q = lower(&q);
    let variants = build_variants(&q);
    let empty = env.new_string("").expect("empty string");
    match env.new_object_array(variants.len() as i32, "java/lang/String", empty) {
        Ok(arr) => {
            for (i, v) in variants.iter().enumerate() {
                if let Ok(js) = env.new_string(v) {
                    let _ = env.set_object_array_element(&arr, i as i32, js);
                }
            }
            arr.as_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

// ============================================================
// Calc: expression evaluator — port of the recursive-descent
// parser from ravex.cmd.cmds.CalcCmd (Java fallback now lives in
// CalcUtility). Must stay behavior-identical; the equivalence
// gate is check/CalcPortCheck.java (two-phase, java-fallback vs
// native).
//
// Documented, accepted deltas vs java.lang.Character:
//   * digits: ASCII only (Java Character.isDigit also accepts
//     exotic Unicode digits, e.g. Arabic-Indic ٥);
//   * astral-plane (surrogate) index arithmetic.
// Everything else mirrors Java semantics exactly, including the
// tolerated trailing garbage, optional ')', long-overflow
// factorial and the Math.{pow,round,min,max} special cases.
// ============================================================

fn java_round(x: f64) -> f64 {
    // Java: (long) Math.floor(x + 0.5) — NaN -> 0, saturating cast.
    if x.is_nan() {
        return 0.0;
    }
    (x + 0.5).floor() as i64 as f64
}

fn java_signum(x: f64) -> f64 {
    // Java Math.signum: ±0.0 -> ±0.0 (Rust f64::signum gives 1.0 for +0.0).
    if x.is_nan() {
        f64::NAN
    } else if x == 0.0 {
        x
    } else if x > 0.0 {
        1.0
    } else {
        -1.0
    }
}

fn java_min(a: f64, b: f64) -> f64 {
    // Java Math.min: NaN if EITHER argument is NaN.
    if a.is_nan() || b.is_nan() {
        f64::NAN
    } else {
        a.min(b)
    }
}

fn java_max(a: f64, b: f64) -> f64 {
    if a.is_nan() || b.is_nan() {
        f64::NAN
    } else {
        a.max(b)
    }
}

fn java_pow(a: f64, b: f64) -> f64 {
    // Mirrors the java.lang.Math.pow special cases on top of IEEE powf.
    if b.is_nan() {
        return if a == 1.0 { 1.0 } else { f64::NAN };
    }
    if b == 0.0 {
        return 1.0; // even for a == NaN (Java rule)
    }
    if a.is_nan() {
        return f64::NAN;
    }
    if b.is_infinite() {
        let aa = a.abs();
        if b > 0.0 {
            if aa > 1.0 {
                return f64::INFINITY;
            }
            if aa == 1.0 {
                return f64::NAN;
            }
            return 0.0;
        } else {
            if aa > 1.0 {
                return 0.0;
            }
            if aa == 1.0 {
                return f64::NAN;
            }
            return f64::INFINITY;
        }
    }
    if a == 0.0 {
        let odd_int = b.trunc() == b && (b as i64) % 2 != 0;
        if b > 0.0 {
            if a.is_sign_negative() && odd_int {
                return -0.0;
            }
            return 0.0;
        }
        if a.is_sign_negative() && odd_int {
            return f64::NEG_INFINITY;
        }
        return f64::INFINITY;
    }
    if a < 0.0 && b.trunc() != b {
        return f64::NAN; // negative base, non-integer exponent
    }
    a.powf(b)
}

fn calc_skip_ws(s: &[char], pos: &mut usize) {
    while *pos < s.len() && s[*pos] == ' ' {
        *pos += 1;
    }
}

fn calc_eval_expr(s: &[char], pos: &mut usize) -> Result<f64, ()> {
    let mut left = calc_eval_term(s, pos)?;
    while *pos < s.len() {
        match s[*pos] {
            '+' => {
                *pos += 1;
                left += calc_eval_term(s, pos)?;
            }
            '-' => {
                *pos += 1;
                left -= calc_eval_term(s, pos)?;
            }
            _ => break,
        }
    }
    Ok(left)
}

fn calc_eval_term(s: &[char], pos: &mut usize) -> Result<f64, ()> {
    let mut left = calc_eval_pow(s, pos)?;
    while *pos < s.len() {
        match s[*pos] {
            '*' | '\u{00D7}' => {
                *pos += 1;
                left *= calc_eval_pow(s, pos)?;
            }
            '/' | '\u{00F7}' => {
                *pos += 1;
                left /= calc_eval_pow(s, pos)?;
            }
            '%' => {
                *pos += 1;
                left %= calc_eval_pow(s, pos)?;
            }
            _ => break,
        }
    }
    Ok(left)
}

fn calc_eval_pow(s: &[char], pos: &mut usize) -> Result<f64, ()> {
    let base = calc_eval_unary(s, pos)?;
    if *pos < s.len() && s[*pos] == '^' {
        *pos += 1;
        let exp = calc_eval_pow(s, pos)?;
        return Ok(java_pow(base, exp));
    }
    Ok(base)
}

fn calc_eval_unary(s: &[char], pos: &mut usize) -> Result<f64, ()> {
    calc_skip_ws(s, pos);
    if *pos < s.len() && s[*pos] == '-' {
        *pos += 1;
        return calc_eval_unary(s, pos).map(|v| -v);
    }
    if *pos < s.len() && s[*pos] == '+' {
        *pos += 1;
        return calc_eval_unary(s, pos);
    }
    let v = calc_eval_atom(s, pos)?;
    calc_skip_ws(s, pos);
    if *pos < s.len() && s[*pos] == '!' {
        *pos += 1;
        // Java: (long) v — NaN -> 0, saturating; f is DOUBLE (no long overflow,
        // 171! -> Infinity, exactly like the Java fallback).
        let n = v as i64;
        let mut f: f64 = 1.0;
        let mut i: i64 = 2;
        while i <= n {
            f *= i as f64;
            i += 1;
        }
        return Ok(f);
    }
    Ok(v)
}

fn calc_eval_atom(s: &[char], pos: &mut usize) -> Result<f64, ()> {
    calc_skip_ws(s, pos);
    if *pos >= s.len() {
        return Err(()); // Java: "Unexpected end"
    }
    let c = s[*pos];
    if c == '(' {
        *pos += 1;
        let v = calc_eval_expr(s, pos)?;
        calc_skip_ws(s, pos);
        // Java tolerates a missing ')'.
        if *pos < s.len() && s[*pos] == ')' {
            *pos += 1;
        }
        return Ok(v);
    }
    if c.is_alphabetic() {
        return calc_eval_func(s, pos);
    }
    let start = *pos;
    let mut has_dot = false;
    while *pos < s.len() && (s[*pos].is_ascii_digit() || (!has_dot && s[*pos] == '.')) {
        if s[*pos] == '.' {
            has_dot = true;
        }
        *pos += 1;
    }
    if *pos == start {
        return Err(()); // Java: "Expected number"
    }
    let text: String = s[start..*pos].iter().collect();
    text.parse::<f64>().map_err(|_| ()) // Java: Double.parseDouble throws on garbage
}

fn calc_eval_func(s: &[char], pos: &mut usize) -> Result<f64, ()> {
    let start = *pos;
    while *pos < s.len() && s[*pos].is_alphabetic() {
        *pos += 1;
    }
    let name: String = s[start..*pos].iter().collect();
    match name.as_str() {
        "pi" => return Ok(std::f64::consts::PI),
        "e" => return Ok(std::f64::consts::E),
        _ => {}
    }
    calc_skip_ws(s, pos);
    if *pos >= s.len() || s[*pos] != '(' {
        return Err(()); // Java: "Expected '(' after function"
    }
    *pos += 1;
    let result = match name.as_str() {
        "sqrt" => calc_eval_expr(s, pos)?.sqrt(),
        "cbrt" => calc_eval_expr(s, pos)?.cbrt(),
        "sin" => calc_eval_expr(s, pos)?.sin(),
        "cos" => calc_eval_expr(s, pos)?.cos(),
        "tan" => calc_eval_expr(s, pos)?.tan(),
        "asin" => calc_eval_expr(s, pos)?.asin(),
        "acos" => calc_eval_expr(s, pos)?.acos(),
        "atan" => calc_eval_expr(s, pos)?.atan(),
        "sinh" => calc_eval_expr(s, pos)?.sinh(),
        "cosh" => calc_eval_expr(s, pos)?.cosh(),
        "tanh" => calc_eval_expr(s, pos)?.tanh(),
        "log" => calc_eval_expr(s, pos)?.log10(), // Java "log" == log10
        "ln" => calc_eval_expr(s, pos)?.ln(),
        "abs" => calc_eval_expr(s, pos)?.abs(),
        "floor" => calc_eval_expr(s, pos)?.floor(),
        "ceil" => calc_eval_expr(s, pos)?.ceil(),
        "round" => java_round(calc_eval_expr(s, pos)?),
        "sign" => java_signum(calc_eval_expr(s, pos)?),
        "exp" => calc_eval_expr(s, pos)?.exp(),
        "rad" => calc_eval_expr(s, pos)?.to_radians(),
        "deg" => calc_eval_expr(s, pos)?.to_degrees(),
        "min" | "max" => {
            let a = calc_eval_expr(s, pos)?;
            calc_skip_ws(s, pos);
            if *pos < s.len() && s[*pos] == ',' {
                *pos += 1;
            } else {
                return Err(()); // Java: "Expected ',' in min(a,b)"
            }
            let b = calc_eval_expr(s, pos)?;
            if name == "min" {
                java_min(a, b)
            } else {
                java_max(a, b)
            }
        }
        _ => return Err(()), // Java: "Unknown function"
    };
    calc_skip_ws(s, pos);
    // Java tolerates a missing ')'.
    if *pos < s.len() && s[*pos] == ')' {
        *pos += 1;
    }
    Ok(result)
}

// ---- JNI export (class ravex.cmd.cmds.CalcNative) ----
// Returns true and writes the value into out[0] on success; false on parse
// error (Java throws there — CalcCmd catches and reports "Invalid expression").
// Infinity/NaN results are NOT errors, exactly like Java.

#[no_mangle]
pub extern "system" fn Java_ravex_cmd_cmds_CalcNative_nativeEval<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    expr: JString<'local>,
    out: JDoubleArray<'local>,
) -> jboolean {
    let s = read_string(&mut env, &expr);
    let chars: Vec<char> = s.chars().collect();
    let mut pos: usize = 0;
    match calc_eval_expr(&chars, &mut pos) {
        Ok(v) => {
            let _ = env.set_double_array_region(&out, 0, &[v]);
            1
        }
        Err(_) => 0,
    }
}

#pragma once

#include <jni.h>
#include <cstddef>

namespace ravex {
namespace jni {

// сбрасывает pending-исключение JNI, true если оно было
inline bool checkAndClearException(JNIEnv* env) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return true;
    }
    return false;
}

// проверка pending-исключения без сброса
inline bool hasPendingException(JNIEnv* env) {
    return env->ExceptionCheck();
}

// raii для GetStringUTFChars
class JniStringUtfChars {
public:
    JniStringUtfChars(JNIEnv* env, jstring str)
        : env_(env), str_(str), chars_(nullptr) {
        if (str_) chars_ = env->GetStringUTFChars(str_, nullptr);
    }
    ~JniStringUtfChars() { reset(); }

    JniStringUtfChars(const JniStringUtfChars&) = delete;
    JniStringUtfChars& operator=(const JniStringUtfChars&) = delete;

    JniStringUtfChars(JniStringUtfChars&& other) noexcept
        : env_(other.env_), str_(other.str_), chars_(other.chars_) {
        other.chars_ = nullptr;
        other.str_ = nullptr;
    }

    JniStringUtfChars& operator=(JniStringUtfChars&& other) noexcept {
        if (this != &other) {
            reset();
            env_ = other.env_;
            str_ = other.str_;
            chars_ = other.chars_;
            other.chars_ = nullptr;
            other.str_ = nullptr;
        }
        return *this;
    }

    explicit operator bool() const { return chars_ != nullptr; }
    const char* get() const { return chars_; }
    const char* operator->() const { return chars_; }
    char operator[](std::size_t i) const { return chars_[i]; }

private:
    void reset() {
        if (chars_ && str_) env_->ReleaseStringUTFChars(str_, chars_);
        chars_ = nullptr;
        str_ = nullptr;
    }

    JNIEnv* env_;
    jstring str_;
    const char* chars_;
};

// разные JNI-функции для разных типов

template <typename T>
struct JniArrayElementTraits;

template <>
struct JniArrayElementTraits<jint> {
    using ArrayType = jintArray;
    using PtrType = jint*;
    static PtrType get(JNIEnv* env, jarray arr, jboolean* isCopy) {
        return env->GetIntArrayElements(static_cast<jintArray>(arr), isCopy);
    }
    static void release(JNIEnv* env, jarray arr, PtrType elems, jboolean /*isCopy*/) {
        env->ReleaseIntArrayElements(static_cast<jintArray>(arr), elems, JNI_ABORT);
    }
};

template <>
struct JniArrayElementTraits<jdouble> {
    using ArrayType = jdoubleArray;
    using PtrType = jdouble*;
    static PtrType get(JNIEnv* env, jarray arr, jboolean* isCopy) {
        return env->GetDoubleArrayElements(static_cast<jdoubleArray>(arr), isCopy);
    }
    static void release(JNIEnv* env, jarray arr, PtrType elems, jboolean /*isCopy*/) {
        env->ReleaseDoubleArrayElements(static_cast<jdoubleArray>(arr), elems, JNI_ABORT);
    }
};

template <>
struct JniArrayElementTraits<jfloat> {
    using ArrayType = jfloatArray;
    using PtrType = jfloat*;
    static PtrType get(JNIEnv* env, jarray arr, jboolean* isCopy) {
        return env->GetFloatArrayElements(static_cast<jfloatArray>(arr), isCopy);
    }
    static void release(JNIEnv* env, jarray arr, PtrType elems, jboolean /*isCopy*/) {
        env->ReleaseFloatArrayElements(static_cast<jfloatArray>(arr), elems, JNI_ABORT);
    }
};

template <>
struct JniArrayElementTraits<jboolean> {
    using ArrayType = jbooleanArray;
    using PtrType = jboolean*;
    static PtrType get(JNIEnv* env, jarray arr, jboolean* isCopy) {
        return env->GetBooleanArrayElements(static_cast<jbooleanArray>(arr), isCopy);
    }
    static void release(JNIEnv* env, jarray arr, PtrType elems, jboolean /*isCopy*/) {
        env->ReleaseBooleanArrayElements(static_cast<jbooleanArray>(arr), elems, JNI_ABORT);
    }
};

template <>
struct JniArrayElementTraits<jbyte> {
    using ArrayType = jbyteArray;
    using PtrType = jbyte*;
    static PtrType get(JNIEnv* env, jarray arr, jboolean* isCopy) {
        return env->GetByteArrayElements(static_cast<jbyteArray>(arr), isCopy);
    }
    static void release(JNIEnv* env, jarray arr, PtrType elems, jboolean /*isCopy*/) {
        env->ReleaseByteArrayElements(static_cast<jbyteArray>(arr), elems, JNI_ABORT);
    }
};

// raii для Get*ArrayElements

template <typename T>
class JniArrayElements {
public:
    using Traits = JniArrayElementTraits<T>;

    JniArrayElements(JNIEnv* env, jarray array)
        : env_(env), array_(array), elems_(nullptr), is_copy_(false) {
        if (array_) elems_ = Traits::get(env_, array_, &is_copy_);
    }
    ~JniArrayElements() { reset(); }

    JniArrayElements(const JniArrayElements&) = delete;
    JniArrayElements& operator=(const JniArrayElements&) = delete;

    JniArrayElements(JniArrayElements&& other) noexcept
        : env_(other.env_), array_(other.array_), elems_(other.elems_), is_copy_(other.is_copy_) {
        other.elems_ = nullptr;
        other.array_ = nullptr;
    }

    JniArrayElements& operator=(JniArrayElements&& other) noexcept {
        if (this != &other) {
            reset();
            env_ = other.env_;
            array_ = other.array_;
            elems_ = other.elems_;
            is_copy_ = other.is_copy_;
            other.elems_ = nullptr;
            other.array_ = nullptr;
        }
        return *this;
    }

    explicit operator bool() const { return elems_ != nullptr; }
    T* get() const { return elems_; }
    T* operator->() const { return elems_; }
    T& operator[](std::size_t i) const { return elems_[i]; }
    jboolean isCopy() const { return is_copy_; }

private:
    void reset() {
        if (elems_ && array_) Traits::release(env_, array_, elems_, is_copy_);
        elems_ = nullptr;
        array_ = nullptr;
    }

    JNIEnv* env_;
    jarray array_;
    T* elems_;
    jboolean is_copy_;
};

using JniIntElements       = JniArrayElements<jint>;
using JniDoubleElements    = JniArrayElements<jdouble>;
using JniFloatElements     = JniArrayElements<jfloat>;
using JniBooleanElements   = JniArrayElements<jboolean>;
using JniByteElements      = JniArrayElements<jbyte>;

} // namespace jni
} // namespace ravex

// старый код зовёт эти хелперы без квалификации
using ravex::jni::JniStringUtfChars;
using ravex::jni::JniIntElements;
using ravex::jni::JniDoubleElements;
using ravex::jni::JniFloatElements;
using ravex::jni::JniBooleanElements;
using ravex::jni::JniByteElements;

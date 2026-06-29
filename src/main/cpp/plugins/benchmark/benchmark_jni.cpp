#include <jni.h>
#include <sstream>
#include <thread>
#include <chrono>
#include "include/benchmark.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jstring JNICALL
Java_ravex_benchmark_BenchmarkBridge_runCPUBenchmark(
    JNIEnv* env, jclass)
{
    std::vector<ravex::benchmark::BenchResult> results;
    bool ok = ravex::benchmark::runBenchmark(ravex::benchmark::BenchType::CPU, results);
    if (!ok) {
        return env->NewStringUTF("CPU benchmark failed");
    }
    std::string formatted = ravex::benchmark::formatResults(results);
    return env->NewStringUTF(formatted.c_str());
}

JNIEXPORT jstring JNICALL
Java_ravex_benchmark_BenchmarkBridge_runMemoryBenchmark(
    JNIEnv* env, jclass)
{
    std::vector<ravex::benchmark::BenchResult> results;
    bool ok = ravex::benchmark::runBenchmark(ravex::benchmark::BenchType::Memory, results);
    if (!ok) {
        return env->NewStringUTF("Memory benchmark failed");
    }
    std::string formatted = ravex::benchmark::formatResults(results);
    return env->NewStringUTF(formatted.c_str());
}

JNIEXPORT jstring JNICALL
Java_ravex_benchmark_BenchmarkBridge_runDiskBenchmark(
    JNIEnv* env, jclass)
{
    std::vector<ravex::benchmark::BenchResult> results;
    bool ok = ravex::benchmark::runBenchmark(ravex::benchmark::BenchType::Disk, results);
    if (!ok) {
        return env->NewStringUTF("Disk benchmark failed");
    }
    std::string formatted = ravex::benchmark::formatResults(results);
    return env->NewStringUTF(formatted.c_str());
}

JNIEXPORT jstring JNICALL
Java_ravex_benchmark_BenchmarkBridge_getSystemInfo(
    JNIEnv* env, jclass)
{
    std::ostringstream oss;
    oss << "Platform: "
        << (sizeof(void*) == 8 ? "x86_64" : "x86")
        << ", Cores: " << std::thread::hardware_concurrency()
        << ", C++23";
    auto now = std::chrono::system_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(
        now.time_since_epoch()).count();
    oss << ", Timestamp: " << ms;
    std::string info = oss.str();
    return env->NewStringUTF(info.c_str());
}

}

#include <jni.h>
#include "checks.hpp"
#include "optimize.hpp"
#include <cstring>
#include <string>
#include <iostream>

static ravex::loader::SystemChecks g_checks;
static ravex::loader::SystemOptimizer g_optimizer;

static std::string escapeJson(const std::string& s) {
    std::string out;
    out.reserve(s.size());
    for (char c : s) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default: out += c;
        }
    }
    return out;
}

static std::string runChecksSafe() {
    try {
        auto report = g_checks.runAll();
        std::string json = "{";
        json += "\"osName\":\"" + escapeJson(report.osName) + "\",";
        json += "\"osVersion\":\"" + escapeJson(report.osVersion) + "\",";
        json += "\"osArch\":\"" + escapeJson(report.osArch) + "\",";
        json += "\"cpuCores\":" + std::to_string(report.cpuCores) + ",";
        json += "\"cpuLoad\":" + std::to_string(report.cpuLoad) + ",";
        json += "\"cpuTemp\":" + std::to_string(report.cpuTemp) + ",";
        json += "\"cpuGovernor\":\"" + escapeJson(report.cpuGovernor) + "\",";
        json += "\"totalRamMB\":" + std::to_string(report.totalRamKB / 1024) + ",";
        json += "\"freeRamMB\":" + std::to_string(report.freeRamKB / 1024) + ",";
        json += "\"availRamMB\":" + std::to_string(report.availRamKB / 1024) + ",";
        json += "\"swapTotalMB\":" + std::to_string(report.swapTotalKB / 1024) + ",";
        json += "\"swapFreeMB\":" + std::to_string(report.swapFreeKB / 1024) + ",";
        json += "\"loadAvg1m\":" + std::to_string(report.loadAvg1m) + ",";
        json += "\"processCount\":" + std::to_string(report.processCount) + ",";
        json += "\"score\":" + std::to_string(report.score) + ",";
        json += "\"selfRSSMB\":" + std::to_string(report.selfRSSKB / 1024);

        if (!report.warnings.empty()) {
            json += ",\"warnings\":[";
            for (size_t i = 0; i < report.warnings.size(); i++) {
                if (i > 0) json += ",";
                json += "\"" + escapeJson(report.warnings[i]) + "\"";
            }
            json += "]";
        }

        if (!report.topProcesses.empty()) {
            json += ",\"topProcesses\":[";
            for (size_t i = 0; i < report.topProcesses.size(); i++) {
                if (i > 0) json += ",";
                json += "{\"pid\":" + std::to_string(report.topProcesses[i].pid) + ",";
                json += "\"name\":\"" + escapeJson(report.topProcesses[i].name) + "\",";
                json += "\"memMB\":" + std::to_string(report.topProcesses[i].memMB) + "}";
            }
            json += "]";
        }

        json += "}";
        return json;
    } catch (const std::exception& e) {
        return "{\"error\":\"" + escapeJson(e.what()) + "\"}";
    }
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_ravex_loader_NativeBridge_runChecks(JNIEnv* env, jclass) {
    auto json = runChecksSafe();
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jstring JNICALL
Java_ravex_loader_NativeBridge_optimize(JNIEnv* env, jclass) {
    try {
        auto result = g_optimizer.runAll();
        std::string json = "{";
        json += "\"freedKB\":" + std::to_string(result.freedKB);

        if (!result.applied.empty()) {
            json += ",\"applied\":[";
            for (size_t i = 0; i < result.applied.size(); i++) {
                if (i > 0) json += ",";
                json += "{\"ok\":true,\"msg\":\"" + escapeJson(result.applied[i].message) + "\"}";
            }
            json += "]";
        }

        if (!result.failed.empty()) {
            json += ",\"failed\":[";
            for (size_t i = 0; i < result.failed.size(); i++) {
                if (i > 0) json += ",";
                json += "{\"ok\":false,\"msg\":\"" + escapeJson(result.failed[i].message) + "\"}";
            }
            json += "]";
        }

        json += "}";
        return env->NewStringUTF(json.c_str());
    } catch (const std::exception& e) {
        std::string err = "{\"error\":\"" + escapeJson(e.what()) + "\"}";
        return env->NewStringUTF(err.c_str());
    }
}

JNIEXPORT jint JNICALL
Java_ravex_loader_NativeBridge_trimMemory(JNIEnv*, jclass) {
    try {
        auto r = g_optimizer.trimMemory();
        return (jint)r.applied.size();
    } catch (...) {
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_ravex_loader_NativeBridge_setHighPriority(JNIEnv*, jclass) {
    try {
        auto r = g_optimizer.setHighPriority();
        return (jint)r.applied.size();
    } catch (...) {
        return 0;
    }
}

JNIEXPORT jstring JNICALL
Java_ravex_loader_NativeBridge_getSystemInfo(JNIEnv* env, jclass) {
    try {
        auto r = g_checks.runAll();
        std::string info = r.osName + " " + r.osVersion + " | "
            + std::to_string(r.cpuCores) + " cores @ " + std::to_string((int)(r.cpuLoad * 100)) + "% | "
            + "RAM " + std::to_string(r.availRamKB / 1024) + "/" + std::to_string(r.totalRamKB / 1024) + " MB | "
            + "Score: " + std::to_string(r.score) + "/100";
        return env->NewStringUTF(info.c_str());
    } catch (const std::exception& e) {
        std::string fallback = "Native check failed: ";
        fallback += e.what();
        return env->NewStringUTF(fallback.c_str());
    }
}

JNIEXPORT jint JNICALL
Java_ravex_loader_NativeBridge_getScore(JNIEnv*, jclass) {
    try {
        auto r = g_checks.runAll();
        return (jint)r.score;
    } catch (...) {
        return 50;
    }
}

}

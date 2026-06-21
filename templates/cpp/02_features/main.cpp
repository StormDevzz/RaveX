// ══════════════════════════════════════════════════════════════════════════════
//  02_features / main.cpp
//
//  RU: Полнофункциональный RaveX нативный аддон. Демонстрирует почти все
//      возможности SDK, которые тебе понадобятся в реальном аддоне:
//        1. Кроссплатформенный код (Windows / Linux) — одна реализация
//           для двух ОС через условную компиляцию (#ifdef _WIN32)
//        2. Работа с конфигом AddonConfig (чтение/запись настроек)
//        3. Обработка событий через AddonListener / AddonEvent
//        4. Фоновый поток с платформенным приоритетом
//        5. Определение аппаратных характеристик (RAM, CPU)
//        6. JNI-мост для двусторонней связи с Java
//        7. platform.hpp — единый заголовок с ADDON_* макросами
//
//  EN: Full-featured RaveX native addon. Demonstrates almost all SDK
//      capabilities you will need in a real addon:
//        1. Cross-platform code (Windows / Linux) — one implementation
//           for two OSes via conditional compilation (#ifdef _WIN32)
//        2. AddonConfig usage (read/write settings)
//        3. Event handling via AddonListener / AddonEvent
//        4. Background thread with platform-specific priority
//        5. Hardware detection (RAM, CPU)
//        6. JNI bridge for bidirectional Java communication
//        7. platform.hpp — unified header with ADDON_* macros
//
//  Подробности / Details: см. GUIDE.md → "02 Полнофункциональный аддон"
// ══════════════════════════════════════════════════════════════════════════════

// RU: platform.hpp — наш собственный кроссплатформенный заголовок.
//     Он определяет ADDON_* макросы (ADDON_SLEEP, ADDON_SET_HIGH_PRIORITY,
//     ADDON_GET_TOTAL_RAM_MB и др.), которые работают одинаково
//     на Windows и Linux. Загляни в этот файл — это ключ к портируемости.
// EN: platform.hpp — our own cross-platform header.
//     It defines ADDON_* macros (ADDON_SLEEP, ADDON_SET_HIGH_PRIORITY,
//     ADDON_GET_TOTAL_RAM_MB, etc.) that work identically
//     on Windows and Linux. Look into this file — it is the key to portability.
#include "platform.hpp"

// RU: Для SHGetFolderPathA (Win32 API получения стандартных папок) нужен shlobj.h.
// EN: SHGetFolderPathA (Win32 API for standard folders) requires shlobj.h.
#ifdef _WIN32
    #include <shlobj.h>
#endif

// RU: Подключаем заголовки RaveX SDK.
//     Каждый из них отвечает за свою часть API:
//     Addon.h        — базовый класс аддона
//     AddonContext.h — контекст (логгер, конфиг, реестр)
//     AddonRegistry.h — реестр аддонов (получение информации о других аддонах)
//     AddonConfig.h  — конфигурация (ключ-значение)
//     AddonEvent.h   — система событий
//     AddonListener.h — подписка на события
//     AddonThread.h  — утилиты для работы с потоками
//     AddonMath.h    — математические функции (lerp, clamp...)
//     AddonVersion.h — проверка совместимости версий API
//     AddonLogger.h  — дополнительное логирование
//     SystemUtils.h  — системные утилиты (информация об ОС, память)
// EN: Include RaveX SDK headers.
//     Each one covers its own part of the API.
#include <Addon.h>
#include <AddonContext.h>
#include <AddonRegistry.h>
#include <AddonConfig.h>
#include <AddonEvent.h>
#include <AddonListener.h>
#include <AddonThread.h>
#include <AddonMath.h>
#include <AddonVersion.h>
#include <AddonLogger.h>
#include <SystemUtils.h>

// RU: Стандартная библиотека C++ для работы с потоками и временем.
// EN: C++ standard library for threads and time handling.
#include <iostream>
#include <thread>
#include <chrono>
#include <mutex>
#include <string>

// RU: Для JNI-функций (JavaVM, JNIEnv, JNIEXPORT и т.д.) нужен jni.h.
//     Он поставляется вместе с JDK.
// EN: JNI functions (JavaVM, JNIEnv, JNIEXPORT, etc.) require jni.h.
//     It ships with the JDK.
#include <jni.h>

namespace ravex {
namespace addon {
namespace my_addon {

// ─── Платформенные утилиты / Platform utilities ──────────────────────────────

// RU: Определяем директорию для данных аддона.
//     На разных ОС — разные стандартные пути:
//     Windows: %LOCALAPPDATA%\RaveX\FeatureAddon
//     Linux:   ~/.ravex/FeatureAddon
//     Если не удалось определить — используем запасной путь.
// EN: Determine the addon data directory.
//     Different OSes have different standard paths:
//     Windows: %LOCALAPPDATA%\RaveX\FeatureAddon
//     Linux:   ~/.ravex/FeatureAddon
//     If detection fails — use a fallback path.
static std::string getAddonDataDir() {
#ifdef _WIN32
    char path[MAX_PATH];
    // RU: SHGetFolderPathA — Win32 API для получения стандартных папок.
    //     CSIDL_LOCAL_APPDATA — папка локальных данных приложения.
    // EN: SHGetFolderPathA — Win32 API to get standard folders.
    //     CSIDL_LOCAL_APPDATA — local application data folder.
    if (SHGetFolderPathA(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, path) == S_OK) {
        return std::string(path) + "\\RaveX\\FeatureAddon";
    }
    return "C:\\RaveX\\FeatureAddon";
#else
    // RU: На Linux используем $HOME. Если переменная не установлена — /tmp.
    // EN: On Linux use $HOME. If the variable is not set — /tmp.
    const char* home = getenv("HOME");
    if (home) return std::string(home) + "/.ravex/FeatureAddon";
    return "/tmp/ravex_FeatureAddon";
#endif
}

// ─── Слушатель событий / Event listener ──────────────────────────────────────

// RU: FeatureListener — подписывается на события RaveX.
//     События приходят из ядра: загрузка мира, изменение настроек,
//     подключение к серверу и т.д. Ты можешь реагировать на них.
// EN: FeatureListener — subscribes to RaveX events.
//     Events come from the core: world load, settings change,
//     server connection, etc. You can react to them.
class FeatureListener : public AddonListener {
public:
    // RU: onEvent вызывается каждый раз, когда происходит событие,
    //     на которое подписан этот слушатель.
    //     event.getName() — имя события (строка).
    //     event.isCancelled() — отменено ли событие (если применимо).
    // EN: onEvent is called each time an event this listener
    //     is subscribed to occurs.
    //     event.getName() — event name (string).
    //     event.isCancelled() — whether the event was cancelled (if applicable).
    void onEvent(AddonEvent& event) override {
        std::cout << "[Feature] Событие: " << event.getName();
        if (event.isCancelled()) std::cout << " (отменено)";
        std::cout << std::endl;
    }
};

// ─── ГЛАВНЫЙ КЛАСС АДДОНА / MAIN ADDON CLASS ─────────────────────────────────

// RU: FeatureAddon — основной класс. Демонстрирует:
//     - хранение контекста, конфига, слушателя, фонового потока
//     - onLoad: инициализация всего
//     - onUnload: корректная остановка и очистка
// EN: FeatureAddon — the main class. Demonstrates:
//     - storing context, config, listener, background thread
//     - onLoad: initialize everything
//     - onUnload: clean shutdown and cleanup
class FeatureAddon : public Addon {
private:
    AddonContext* ctx = nullptr;
    AddonConfig config;
    FeatureListener listener;
    bool running = false;
    std::thread worker;
    std::mutex mtx;

    // RU: Фоновый рабочий поток. Запускается в onLoad, останавливается в onUnload.
    //     ADDON_SET_NORMAL_PRIORITY() — устанавливает нормальный приоритет
    //     потока (на Windows — SetThreadPriority, на Linux — setpriority).
    //     ADDON_SLEEP(50) — платформенно-независимая задержка (50 мс).
    //     Внутри цикла можно делать периодические проверки, обновления и т.д.
    //     Используем мьютекс для потокобезопасного доступа к ctx.
    // EN: Background worker thread. Started in onLoad, stopped in onUnload.
    //     ADDON_SET_NORMAL_PRIORITY() — sets normal thread priority
    //     (on Windows — SetThreadPriority, on Linux — setpriority).
    //     ADDON_SLEEP(50) — platform-independent delay (50 ms).
    //     Inside the loop you can do periodic checks, updates, etc.
    //     We use a mutex for thread-safe access to ctx.
    void workerThread() {
        ADDON_SET_NORMAL_PRIORITY();
        while (running) {
            {
                std::lock_guard<std::mutex> lock(mtx);
                if (ctx) {
                    // RU: Пример использования AddonMath::lerp (линейная интерполяция).
                    //     В реальном аддоне здесь была бы твоя логика.
                    // EN: Example using AddonMath::lerp (linear interpolation).
                    //     In a real addon your logic would go here.
                    double v = AddonMath::lerp(0.0, 1.0, 0.5);
                    (void)v;
                }
            }
            ADDON_SLEEP(50);
        }
    }

public:
    // RU: Вызывается при загрузке аддона. Это главный метод инициализации.
    //     Получаем контекст, настраиваем конфиг, логируем информацию
    //     о платформе, проверяем версию API, запускаем фоновый поток.
    // EN: Called when the addon is loaded. This is the main init method.
    //     Get context, set up config, log platform info,
    //     check API version, start background thread.
    void onLoad(AddonContext* context) override {
        ctx = context;
        ctx->logInfo("FeatureAddon: загрузка...");

        // RU: Конфиг — простой key-value store. Значения хранятся как строки.
        //     Можно сохранять/загружать из файла. RaveX сам управляет
        //     файлом конфига для каждого аддона.
        // EN: Config — simple key-value store. Values are stored as strings.
        //     Can be saved/loaded from file. RaveX manages the config file
        //     for each addon automatically.
        config.set("enabled", "true");
        config.set("sensitivity", "0.5");

        // RU: Выводим путь к директории данных аддона.
        // EN: Print addon data directory path.
        ctx->logInfo("Data dir: " + getAddonDataDir());

        // RU: Платформенная информация — определяем ОС и объём RAM.
        //     ADDON_GET_TOTAL_RAM_MB() — макрос из platform.hpp,
        //     который на Windows использует GlobalMemoryStatusEx,
        //     а на Linux — sysconf(_SC_PHYS_PAGES) * sysconf(_SC_PAGE_SIZE).
        // EN: Platform info — detect OS and total RAM.
        //     ADDON_GET_TOTAL_RAM_MB() — macro from platform.hpp,
        //     which on Windows uses GlobalMemoryStatusEx,
        //     and on Linux — sysconf(_SC_PHYS_PAGES) * sysconf(_SC_PAGE_SIZE).
#ifdef _WIN32
        ctx->logInfo("Платформа: Windows");
        ctx->logInfo("RAM: " + std::to_string(ADDON_GET_TOTAL_RAM_MB()) + " MB");
#else
        ctx->logInfo("Платформа: Linux");
        long pages = sysconf(_SC_PHYS_PAGES);
        long pageSize = sysconf(_SC_PAGE_SIZE);
        if (pages > 0 && pageSize > 0) {
            long long ram = (long long)pages * pageSize / 1048576;
            ctx->logInfo("RAM: " + std::to_string(ram) + " MB");
        }
#endif

        // RU: Проверяем совместимость версии API.
        //     Если API несовместим — лучше предупредить пользователя,
        //     потому что некоторые функции могут работать неправильно.
        // EN: Check API version compatibility.
        //     If the API is incompatible — better warn the user,
        //     because some features may not work correctly.
        std::string ver = AddonVersion::getApiVersion();
        ctx->logInfo("API версия: " + ver);
        if (!AddonVersion::isCompatible(ver)) {
            ctx->logInfo("ВНИМАНИЕ: API может быть несовместим!");
        }

        // RU: Демо-событие — создаём и отправляем через слушатель.
        //     В реальном аддоне события приходят из RaveX.
        // EN: Demo event — create and dispatch through the listener.
        //     In a real addon events come from RaveX.
        AddonEvent ev("FeatureAddon:loaded");
        listener.onEvent(ev);

        // RU: Запускаем фоновый поток. Аддон продолжает работу,
        //     даже когда onLoad завершился.
        // EN: Start background thread. The addon keeps running
        //     even after onLoad returns.
        running = true;
        worker = std::thread(&FeatureAddon::workerThread, this);
    }

    // RU: Вызывается при выгрузке. Останавливаем поток, чистим ресурсы.
    //     Это важно — если не остановить поток, после выгрузки аддона
    //     он продолжит работать с висячим указателем ctx, что приведёт к крашу.
    // EN: Called on unload. Stop the thread, clean up resources.
    //     This is important — if you don't stop the thread, after addon
    //     unload it will keep running with a dangling ctx pointer, causing a crash.
    void onUnload() override {
        ctx->logInfo("FeatureAddon: выгрузка...");
        running = false;
        if (worker.joinable()) worker.join();

        // RU: Сбрасываем приоритет процесса обратно в нормальный.
        // EN: Reset process priority back to normal.
        ADDON_SET_NORMAL_PRIORITY();
        ctx = nullptr;
    }

    std::string getName()    const override { return "FeatureAddon"; }
    std::string getVersion() const override { return "1.4.1"; }
};

} // namespace my_addon
} // namespace addon
} // namespace ravex

// ─── JNI: вызов Java из C++ / Calling Java from C++ ─────────────────────────
//
// RU: JNI (Java Native Interface) позволяет Java-классам вызывать C++ функции,
//     а C++ — вызывать Java-методы. Это нужно, когда тебе нужна
//     производительность C++ в Java-аддоне или доступ к системным вызовам.
//
//     Java-сторона должна объявить native-методы в классе:
//       public class FeatureAddon {
//           public static native void nativeLog(String msg);
//           public static native int nativeGetPid();
//           public static native long nativeGetTotalRamMB();
//       }
//
//     Имена C++ функций должны строго соответствовать схеме:
//       Java_<package>_<Class>_<method>
//     где пакет использует подчёркивания вместо точек.
//
// EN: JNI (Java Native Interface) lets Java classes call C++ functions,
//     and C++ call Java methods. This is needed when you need
//     C++ performance in a Java addon or access to system calls.
//
//     The Java side must declare native methods in the class:
//       public class FeatureAddon {
//           public static native void nativeLog(String msg);
//           public static native int nativeGetPid();
//           public static native long nativeGetTotalRamMB();
//       }
//
//     C++ function names must strictly follow the scheme:
//       Java_<package>_<Class>_<method>
//     where the package uses underscores instead of dots.

// RU: Глобальный указатель на JavaVM — нужен для получения JNIEnv
//     из любого потока. Сохраняем его в JNI_OnLoad.
// EN: Global JavaVM pointer — needed to get JNIEnv from any thread.
//     We save it in JNI_OnLoad.
static JavaVM* g_jvm = nullptr;

// RU: JNI_OnLoad вызывается Java при System.loadLibrary().
//     Сохраняем vm для последующего использования.
//     Должен вернуть минимальную поддерживаемую версию JNI.
// EN: JNI_OnLoad is called by Java on System.loadLibrary().
//     Save vm for later use.
//     Must return the minimum supported JNI version.
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_8;
}

// RU: Native-реализация Java_ravex_addon_feature_FeatureAddon_nativeLog.
//     Принимает строку из Java, выводит в C++ консоль.
//     env->GetStringUTFChars конвертирует Java String в C-строку.
//     После использования обязательно ReleaseStringUTFChars.
// EN: Native implementation of Java_ravex_addon_feature_FeatureAddon_nativeLog.
//     Receives a string from Java, prints to C++ console.
//     env->GetStringUTFChars converts Java String to C string.
//     Must call ReleaseStringUTFChars after use.
extern "C" JNIEXPORT void JNICALL
Java_ravex_addon_feature_FeatureAddon_nativeLog(JNIEnv* env, jclass, jstring msg) {
    const char* str = env->GetStringUTFChars(msg, nullptr);
    std::cout << "[C++] " << str << std::endl;
    env->ReleaseStringUTFChars(msg, str);
}

// RU: Возвращает PID текущего процесса.
//     Java не может получить PID без JNI — это типичный случай,
//     когда нужен C++.
// EN: Returns the current process PID.
//     Java cannot get PID without JNI — a typical case
//     where C++ is needed.
extern "C" JNIEXPORT jint JNICALL
Java_ravex_addon_feature_FeatureAddon_nativeGetPid(JNIEnv*, jclass) {
#ifdef _WIN32
    return (jint)GetCurrentProcessId();
#else
    return (jint)getpid();
#endif
}

// RU: Возвращает общий объём RAM в мегабайтах.
//     Использует ADDON_GET_TOTAL_RAM_MB() из platform.hpp.
//     Java не имеет прямого доступа к этой информации (OperatingSystemMXBean
//     даёт только объём, доступный JVM, а не физической памяти).
// EN: Returns total physical RAM in megabytes.
//     Uses ADDON_GET_TOTAL_RAM_MB() from platform.hpp.
//     Java has no direct access to this info (OperatingSystemMXBean
//     only gives JVM-available memory, not physical RAM).
extern "C" JNIEXPORT jlong JNICALL
Java_ravex_addon_feature_FeatureAddon_nativeGetTotalRamMB(JNIEnv*, jclass) {
    return (jlong)ADDON_GET_TOTAL_RAM_MB();
}

// ─── Обязательные точки входа / Required entry points ────────────────────────
//
// RU: RaveX находит эти функции через dlsym / GetProcAddress.
//     createAddon — создаёт экземпляр аддона.
//     destroyAddon — удаляет его.
//     Они должны быть extern "C" и иметь видимость "default".
//
// EN: RaveX finds these functions via dlsym / GetProcAddress.
//     createAddon — creates the addon instance.
//     destroyAddon — destroys it.
//     They must be extern "C" with "default" visibility.

extern "C" {

#ifdef _WIN32
    __declspec(dllexport) ravex::addon::Addon* createAddon() {
#else
    __attribute__((visibility("default")))
    ravex::addon::Addon* createAddon() {
#endif
        return new ravex::addon::my_addon::FeatureAddon();
    }

#ifdef _WIN32
    __declspec(dllexport) void destroyAddon(ravex::addon::Addon* addon) {
#else
    __attribute__((visibility("default")))
    void destroyAddon(ravex::addon::Addon* addon) {
#endif
        delete addon;
    }

}

// ═════════════════════════════════════════════════════════════════════════════
//  Альтернатива / Alternative: использование отдельного JniBridge.hpp/cpp
//
//  RU: В этом примере JNI-функции встроены прямо в main.cpp.
//      В больших проектах лучше вынести их в отдельные файлы
//      JniBridge.hpp / JniBridge.cpp — они уже лежат в этой папке.
//      Для этого раскомментируй в CMakeLists.txt строку "JniBridge.cpp"
//      и перенеси JNI_OnLoad и native-функции туда.
//
//  EN: In this example JNI functions are embedded directly in main.cpp.
//      In larger projects it is better to move them to separate files
//      JniBridge.hpp / JniBridge.cpp — they are already in this folder.
//      To do so, uncomment the "JniBridge.cpp" line in CMakeLists.txt
//      and move JNI_OnLoad and native functions there.
// ═════════════════════════════════════════════════════════════════════════════

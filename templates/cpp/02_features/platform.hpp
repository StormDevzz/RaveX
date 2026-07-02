// ══════════════════════════════════════════════════════════════════════════════
//  02_features / platform.hpp
//
//  RU: Единый кроссплатформенный заголовок для RaveX C++ аддонов.
//      Все ADDON_* макросы работают одинаково на Windows и Linux.
//      Это ключевой файл для портируемости - добавляя новый макрос
//      здесь, ты обеспечиваешь его работу на обеих ОС.
//
//      Что здесь есть:
//        - ADDON_API / ADDON_IMPORT - экспорт/импорт символов DLL/SO
//        - ADDON_FORCEINLINE - форсированный инлайн
//        - ADDON_LIKELY / ADDON_UNLIKELY - подсказки branch prediction
//        - ADDON_DEBUG_BREAK - точка останова (дебаг)
//        - ADDON_SLEEP - платформенно-независимый сон потока
//        - ADDON_LOAD_LIB / ADDON_GET_SYM / ADDON_FREE_LIB - загрузка DLL/SO
//        - ADDON_SET_HIGH_PRIORITY / ADDON_SET_NORMAL_PRIORITY - приоритет
//        - ADDON_TRIM_MEMORY / ADDON_GET_TOTAL_RAM_MB - работа с памятью
//
//  EN: Single cross-platform header for RaveX C++ addons.
//      All ADDON_* macros work identically on Windows and Linux.
//      This is the key file for portability - adding a new macro
//      here ensures it works on both OSes.
//
//      What is here:
//        - ADDON_API / ADDON_IMPORT - DLL/SO symbol export/import
//        - ADDON_FORCEINLINE - forced inlining
//        - ADDON_LIKELY / ADDON_UNLIKELY - branch prediction hints
//        - ADDON_DEBUG_BREAK - debug breakpoint
//        - ADDON_SLEEP - platform-independent thread sleep
//        - ADDON_LOAD_LIB / ADDON_GET_SYM / ADDON_FREE_LIB - DLL/SO loading
//        - ADDON_SET_HIGH_PRIORITY / ADDON_SET_NORMAL_PRIORITY - priority
//        - ADDON_TRIM_MEMORY / ADDON_GET_TOTAL_RAM_MB - memory management
// ══════════════════════════════════════════════════════════════════════════════

#pragma once

// ─── Экспорт/импорт символов / Symbol export/import ─────────────────────────
//
// RU: ADDON_API - помечает функцию как видимую из DLL/SO.
//     ADDON_IMPORT - для импорта из другой DLL (редко нужно в аддонах).
//     ADDON_CC - соглашение о вызове (__stdcall на Win, ничего на Linux).
// EN: ADDON_API - marks a function as visible from DLL/SO.
//     ADDON_IMPORT - for importing from another DLL (rarely needed).
//     ADDON_CC - calling convention (__stdcall on Win, nothing on Linux).
#ifdef _WIN32
    #define ADDON_API __declspec(dllexport)
    #define ADDON_IMPORT __declspec(dllimport)
    #define ADDON_CC __stdcall
#else
    #define ADDON_API __attribute__((visibility("default")))
    #define ADDON_IMPORT
    #define ADDON_CC
#endif

// ─── Форсированный инлайн / Force inline ────────────────────────────────────
//
// RU: ADDON_FORCEINLINE - заставляет компилятор встроить функцию.
//     На Windows - __forceinline, на Linux - __attribute__((always_inline)).
// EN: ADDON_FORCEINLINE - forces the compiler to inline a function.
//     On Windows - __forceinline, on Linux - __attribute__((always_inline)).
#ifdef _WIN32
    #define ADDON_FORCEINLINE __forceinline
#else
    #define ADDON_FORCEINLINE inline __attribute__((always_inline))
#endif

// ─── Подсказки branch prediction / Branch prediction hints ───────────────────
//
// RU: ADDON_LIKELY(x) - подсказывает компилятору, что x скорее всего true.
//     ADDON_UNLIKELY(x) - подсказывает, что x скорее всего false.
//     Улучшает производительность предсказателя переходов CPU.
//     На Windows нет __builtin_expect, поэтому макросы пассивные.
// EN: ADDON_LIKELY(x) - hints the compiler that x is likely true.
//     ADDON_UNLIKELY(x) - hints that x is likely false.
//     Improves CPU branch predictor performance.
//     Windows lacks __builtin_expect, so macros are no-ops.
#ifdef _WIN32
    #define ADDON_LIKELY(x)   x
    #define ADDON_UNLIKELY(x) x
#else
    #define ADDON_LIKELY(x)   __builtin_expect(!!(x), 1)
    #define ADDON_UNLIKELY(x) __builtin_expect(!!(x), 0)
#endif

// ─── Точка останова / Debug break ───────────────────────────────────────────
//
// RU: ADDON_DEBUG_BREAK() - останавливает выполнение в отладчике.
//     На Windows - __debugbreak (intrinsic), на Linux - __builtin_trap.
// EN: ADDON_DEBUG_BREAK() - breaks execution in the debugger.
//     On Windows - __debugbreak (intrinsic), on Linux - __builtin_trap.
#ifdef _WIN32
    #define ADDON_DEBUG_BREAK() __debugbreak()
#else
    #define ADDON_DEBUG_BREAK() __builtin_trap()
#endif

// ─── Thread-local storage ────────────────────────────────────────────────────
//
// RU: ADDON_TLS - thread_local, доступно в C++11+ на всех платформах.
// EN: ADDON_TLS - thread_local, available in C++11+ on all platforms.
#define ADDON_TLS thread_local

// ─── Маркировка устаревшего / Deprecation marking ──────────────────────────
//
// RU: ADDON_DEPRECATED - помечает функцию/переменную как устаревшую.
//     Компилятор выдаст предупреждение при использовании.
// EN: ADDON_DEPRECATED - marks a function/variable as deprecated.
//     The compiler will emit a warning on use.
#ifdef _WIN32
    #define ADDON_DEPRECATED __declspec(deprecated)
#else
    #define ADDON_DEPRECATED __attribute__((deprecated))
#endif

// ─── Маркировка неиспользуемого / Unused marking ────────────────────────────
//
// RU: ADDON_UNUSED - подавляет предупреждение о неиспользуемой переменной.
// EN: ADDON_UNUSED - suppresses unused variable warnings.
#ifdef _WIN32
    #define ADDON_UNUSED
#else
    #define ADDON_UNUSED __attribute__((unused))
#endif

// ─── Разделитель путей / Path separator ─────────────────────────────────────
//
// RU: Windows использует обратную косую черту, Linux - прямую.
//     ADDON_PATH_SEP - char, ADDON_PATH_SEP_STR - строка.
// EN: Windows uses backslash, Linux uses forward slash.
//     ADDON_PATH_SEP - char, ADDON_PATH_SEP_STR - string.
#ifdef _WIN32
    #define ADDON_PATH_SEP '\\'
    #define ADDON_PATH_SEP_STR "\\"
#else
    #define ADDON_PATH_SEP '/'
    #define ADDON_PATH_SEP_STR "/"
#endif

// ─── Сон потока / Thread sleep ──────────────────────────────────────────────
//
// RU: ADDON_SLEEP(ms) - приостанавливает поток на ms миллисекунд.
//     На Windows - Sleep(DWORD), на Linux - usleep(useconds_t).
//     Важно: Sleep принимает миллисекунды, usleep - микросекунды,
//     поэтому умножаем ms * 1000.
// EN: ADDON_SLEEP(ms) - suspends the thread for ms milliseconds.
//     On Windows - Sleep(DWORD), on Linux - usleep(useconds_t).
//     Important: Sleep takes ms, usleep takes microseconds,
//     so we multiply ms * 1000.
#ifdef _WIN32
    #include <windows.h>
    #define ADDON_SLEEP(ms) Sleep(ms)
#else
    #include <unistd.h>
    #define ADDON_SLEEP(ms) usleep((ms) * 1000)
#endif

// ─── Загрузка динамических библиотек / Dynamic library loading ──────────────
//
// RU: ADDON_LOAD_LIB(name) - загружает DLL/SO по имени.
//     ADDON_GET_SYM(handle, fn) - получает указатель на функцию.
//     ADDON_FREE_LIB(handle) - выгружает библиотеку.
//     На Windows используется LoadLibraryEx / GetProcAddress / FreeLibrary.
//     На Linux - dlopen / dlsym / dlclose.
// EN: ADDON_LOAD_LIB(name) - loads a DLL/SO by name.
//     ADDON_GET_SYM(handle, fn) - gets a function pointer.
//     ADDON_FREE_LIB(handle) - unloads the library.
//     On Windows - LoadLibraryEx / GetProcAddress / FreeLibrary.
//     On Linux - dlopen / dlsym / dlclose.
#ifdef _WIN32
    using AddonLibHandle = HMODULE;
    #define ADDON_LOAD_LIB(name)      LoadLibraryExA(name, NULL, 0)
    #define ADDON_GET_SYM(handle, fn) GetProcAddress(handle, fn)
    #define ADDON_FREE_LIB(handle)    FreeLibrary(handle)
#else
    #include <dlfcn.h>
    using AddonLibHandle = void*;
    #define ADDON_LOAD_LIB(name)      dlopen(name, RTLD_NOW | RTLD_LOCAL)
    #define ADDON_GET_SYM(handle, fn) dlsym(handle, fn)
    #define ADDON_FREE_LIB(handle)    dlclose(handle)
#endif

// ─── Приоритет процесса / Process priority ──────────────────────────────────
//
// RU: ADDON_SET_HIGH_PRIORITY - повышает приоритет процесса.
//     ADDON_SET_NORMAL_PRIORITY - возвращает нормальный приоритет.
//     На Windows - SetPriorityClass с HIGH_PRIORITY_CLASS / NORMAL_PRIORITY_CLASS.
//     На Linux - setpriority с nice -20 / nice 0.
// EN: ADDON_SET_HIGH_PRIORITY - raises process priority.
//     ADDON_SET_NORMAL_PRIORITY - restores normal priority.
//     On Windows - SetPriorityClass with HIGH_PRIORITY_CLASS / NORMAL_PRIORITY_CLASS.
//     On Linux - setpriority with nice -20 / nice 0.
#ifdef _WIN32
    #define ADDON_SET_HIGH_PRIORITY()   SetPriorityClass(GetCurrentProcess(), HIGH_PRIORITY_CLASS)
    #define ADDON_SET_NORMAL_PRIORITY() SetPriorityClass(GetCurrentProcess(), NORMAL_PRIORITY_CLASS)
#else
    #include <sys/resource.h>
    #define ADDON_SET_HIGH_PRIORITY()   setpriority(PRIO_PROCESS, 0, -20)
    #define ADDON_SET_NORMAL_PRIORITY() setpriority(PRIO_PROCESS, 0, 0)
#endif

// ─── Работа с памятью / Memory management ───────────────────────────────────
//
// RU: ADDON_TRIM_MEMORY() - очищает рабочий набор памяти процесса.
//     На Windows - EmptyWorkingSet (сбрасывает неиспользуемые страницы).
//     На Linux - malloc_trim(0) (возвращает память кучи системе).
// EN: ADDON_TRIM_MEMORY() - trims the process working set.
//     On Windows - EmptyWorkingSet (flushes unused pages).
//     On Linux - malloc_trim(0) (returns heap memory to the system).
#ifdef _WIN32
    #include <psapi.h>
    #pragma comment(lib, "psapi.lib")
    #define ADDON_TRIM_MEMORY() EmptyWorkingSet(GetCurrentProcess())

    // RU: ADDON_GET_TOTAL_RAM_MB() - возвращает общий объём физической
    //     памяти в мегабайтах. Использует GlobalMemoryStatusEx.
    // EN: ADDON_GET_TOTAL_RAM_MB() - returns total physical RAM in MB.
    //     Uses GlobalMemoryStatusEx.
    inline size_t ADDON_GET_TOTAL_RAM_MB() {
        MEMORYSTATUSEX ms = { sizeof(MEMORYSTATUSEX) };
        return GlobalMemoryStatusEx(&ms) ? (size_t)(ms.ullTotalPhys / 1048576) : 0;
    }
#else
    #define ADDON_TRIM_MEMORY() malloc_trim(0)

    // RU: Linux-версия использует sysconf для получения страниц памяти.
    //     _SC_PHYS_PAGES - общее количество физических страниц.
    //     _SC_PAGE_SIZE - размер одной страницы в байтах.
    //     Перемножаем и делим на 1048576 (1024^2) для перевода в MB.
    // EN: Linux version uses sysconf to get memory page info.
    //     _SC_PHYS_PAGES - total number of physical pages.
    //     _SC_PAGE_SIZE - size of one page in bytes.
    //     Multiply and divide by 1048576 (1024^2) to convert to MB.
    inline size_t ADDON_GET_TOTAL_RAM_MB() {
        long pages = sysconf(_SC_PHYS_PAGES);
        long size  = sysconf(_SC_PAGE_SIZE);
        return (pages > 0 && size > 0) ? (size_t)((long long)pages * size / 1048576) : 0;
    }
#endif

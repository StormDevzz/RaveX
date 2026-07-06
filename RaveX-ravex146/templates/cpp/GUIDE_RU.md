# RaveX Нативные C++ Аддоны — Полное руководство

> Версия: 1.0 | Платформы: Windows 10/11, Linux | Язык: C++23

---

## Содержание

1. [Что такое нативный аддон?](#1-что-такое-нативный-аддон)
2. [Структура проекта](#2-структура-проекта)
3. [01 Минимальный аддон](#3-01-минимальный-аддон)
4. [02 Полнофункциональный аддон](#4-02-полнофункциональный-аддон)
5. [03 Оверлейное окно](#5-03-оверлейное-окно)
6. [04 GitHub авто-обновление](#6-04-github-авто-обновление)
7. [Кроссплатформенное программирование](#7-кроссплатформенное-программирование)
8. [JNI: Java + C++](#8-jni-java--c)
9. [Сборка и установка](#9-сборка-и-установка)
10. [Справочник API](#10-справочник-api)

---

## 1. Что такое нативный аддон?

**Нативный аддон** — это динамическая библиотека (`.dll` на Windows, `.so` на Linux), которую RaveX загружает в рантайме через `LoadLibrary` / `dlopen`.

### Зачем нужен C++, если есть Java?

| Задача | Java | C++ |
|--------|------|-----|
| Скорость | ~100-500 оп/ms | ~1 000 000+ оп/ms |
| Доступ к Win32 API | Нет | Да `CreateWindowEx`, `DirectX`, `WinHTTP` |
| Доступ к X11/Linux API | Нет | Да `Xlib`, `POSIX sockets`, `ioctl` |
| Чтение/запись памяти | Нет (Sandbox) | Да `ReadProcessMemory`, `ptrace` |
| Оверлейное окно | Нет | Да Win32 layered / X11 override-redirect |
| HTTP-запросы | Да `java.net.URL` | Да WinHTTP / POSIX sockets |
| Размер бинарника | ~2 KB JAR | ~50-500 KB DLL/SO |
| Простота разработки | Да Высокая | Средняя |

### Когда использовать C++ аддон?

- **Оверлей**: FPS-счётчик, радар, информация о сервере поверх игры
- **Производительность**: математические расчёты, рендеринг, обработка данных
- **Системные вызовы**: управление процессами, приоритетами, памятью
- **Сеть**: прямые HTTP-запросы (без Java-прослойки)
- **Аппаратное ускорение**: Win32 GDI, Direct2D, X11

---

## 2. Структура проекта

```
templates/cpp/
├── 01_minimal/              # 1. Минимальный аддон (53 строки)
│   ├── CMakeLists.txt        #   Конфигурация сборки
│   └── main.cpp              #   Исходный код
├── 02_features/             # 2. Полнофункциональный аддон
│   ├── CMakeLists.txt        #   + pthread/winmm, JNI
│   ├── main.cpp              #   ~200 строк с платформенными ветками
│   ├── platform.hpp          #   ADDON_* макросы (116 строк)
│   ├── JniBridge.hpp         #   JNI хелперы: jstring2str, fireCallback
│   └── JniBridge.cpp         #   Реализация JNI-моста
├── 03_overlay/              # 3. Оверлейное окно
│   ├── CMakeLists.txt        #   + gdi32, dwmapi (Win) / X11 (Linux)
│   └── main.cpp              #   Win32Overlay + X11Overlay классы
├── 04_github/               # 4. GitHub авто-обновление
│   ├── CMakeLists.txt        #   + ravex_github_tools библиотека
│   └── main.cpp              #   ReleaseChecker, ReleaseManager
├── scripts/
│   ├── build.bat             # Универсальный скрипт сборки (Windows)
│   └── build.sh              # Универсальный скрипт сборки (Linux)
├── GUIDE.md                  # This guide (English)
├── GUIDE_RU.md               # Это руководство (Russian)
└── README.md                 # Быстрый старт + навигация
```

### Принцип: каждый пример самодостаточен

Каждый каталог `01_minimal`, `02_features` и т.д. — это **полноценный CMake-проект**:

```bash
cd 02_features
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -G "MinGW Makefiles"
cmake --build . -j
```

Или используй общий скрипт:
```bash
cd scripts
./build.sh 02_features --install     # Linux
build.bat 02_features --install      # Windows
```

---

## 3. 01 Минимальный аддон

**Цель**: понять обязательный минимум для любого нативного аддона.

### Что нужно сделать?

1. Унаследоваться от `ravex::addon::Addon`
2. Переопределить 4 метода: `onLoad`, `onUnload`, `getName`, `getVersion`
3. Экспортировать две `extern "C"` функции: `createAddon` и `destroyAddon`

### Весь код (`01_minimal/main.cpp`):

```cpp
#include "../../../src/main/cpp/addon/include/Addon.h"
#include "../../../src/main/cpp/addon/include/AddonContext.h"

namespace ravex { namespace addon { namespace minimal_addon {

class MinimalAddon : public Addon {
public:
    void onLoad(AddonContext* ctx) override {
        ctx->logInfo("MinimalAddon loaded!");
    }

    void onUnload() override { /* cleanup */ }
    std::string getName()    const override { return "MinimalAddon"; }
    std::string getVersion() const override { return "1.4.3"; }
};

}}}

extern "C" {
    ADDON_API ravex::addon::Addon* createAddon() {
        return new ravex::addon::minimal_addon::MinimalAddon();
    }
    ADDON_API void destroyAddon(ravex::addon::Addon* addon) {
        delete addon;
    }
}
```

### Как это работает?

1. RaveX находит `.dll`/`.so` в папке аддонов
2. Вызывает `LoadLibrary` / `dlopen`
3. Ищет символ `createAddon` через `GetProcAddress` / `dlsym`
4. Создаёт экземпляр аддона
5. Вызывает `onLoad(ctx)` — аддон готов к работе
6. При выгрузке: вызывает `onUnload()`, потом `destroyAddon()`

### Ключевые моменты

- **ADDON_API** — макрос из `platform.hpp`:
  - Windows: `__declspec(dllexport)`
  - Linux: `__attribute__((visibility("default")))`
- Без этого макроса Linux не экспортирует символы (из-за `-fvisibility=hidden`)
- Названия `createAddon` и `destroyAddon` **обязательны** — RaveX ищет именно их

---

## 4. 02 Полнофункциональный аддон

**Цель**: научиться работать с API RaveX, платформенными ветками, конфигом, событиями, тредами и JNI.

### CMakeLists.txt

```cmake
cmake_minimum_required(VERSION 3.16)
project(FeatureAddon VERSION 1.4.3 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(WIN32)
    if(MSVC)
        add_link_options(/MT)           # Статический CRT
    else()
        add_link_options(-static -static-libgcc -static-libstdc++)
    endif()
    target_link_libraries(FeatureAddon PRIVATE winmm psapi)
else()
    find_package(Threads REQUIRED)
    target_link_libraries(FeatureAddon PRIVATE Threads::Threads)
endif()

add_library(FeatureAddon SHARED main.cpp JniBridge.cpp)
target_include_directories(FeatureAddon PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}                       # platform.hpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/main/cpp/addon/include  # Addon.h etc.
)
```

### Разбор файлов

#### `main.cpp` — логика аддона

Файл разделён на 3 логические части:

**Часть 1: Платформенные утилиты**
```cpp
static std::string getAddonDataDir() {
#ifdef _WIN32
    // Windows: %LOCALAPPDATA%\RaveX\FeatureAddon
    char path[MAX_PATH];
    SHGetFolderPathA(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, path);
    return std::string(path) + "\\RaveX\\FeatureAddon";
#else
    // Linux: ~/.ravex/FeatureAddon
    const char* home = getenv("HOME");
    return std::string(home) + "/.ravex/FeatureAddon";
#endif
}
```

**Часть 2: Слушатель событий и тред**
```cpp
class FeatureListener : public AddonListener {
    void onEvent(AddonEvent& event) override {
        // Реагируем на события RaveX
    }
};

class FeatureAddon : public Addon {
    std::thread worker;     // Фоновый тред
    std::mutex  mtx;

    void workerThread() {
        ADDON_SET_NORMAL_PRIORITY();      // Кроссплатформенный приоритет
        while (running) {
            // Работа с данными...
            ADDON_SLEEP(50);              // 50 мс сон
        }
    }
};
```

**Часть 3: JNI-функции**
```cpp
extern "C" JNIEXPORT void JNICALL
Java_ravex_addon_feature_FeatureAddon_nativeLog(JNIEnv* env, jclass, jstring msg) {
    const char* str = env->GetStringUTFChars(msg, nullptr);
    std::cout << "[C++] " << str << std::endl;
    env->ReleaseStringUTFChars(msg, str);
}
```

#### `platform.hpp` — кроссплатформенные макросы

Единый заголовок, который скрывает различия платформ. Примеры:

```cpp
ADDON_SLEEP(100)              // Sleep(100) на Win, usleep(100000) на Linux
ADDON_SET_HIGH_PRIORITY()     // SetPriorityClass(...) на Win, setpriority() на Linux
ADDON_TRIM_MEMORY()           // EmptyWorkingSet() на Win, malloc_trim(0) на Linux
ADDON_LOAD_LIB("mylib.dll")   // LoadLibraryA() на Win, dlopen() на Linux
ADDON_GET_SYM(handle, "fn")   // GetProcAddress() на Win, dlsym() на Linux
```

Полный список макросов — в файле `02_features/platform.hpp`.

#### `JniBridge.hpp/cpp` — мост между Java и C++

```cpp
// В Java:
int result = JniBridge.nativeAdd(40, 2);
String info = JniBridge.nativeGetPlatformInfo();
```

```cpp
// В C++ (JniBridge.cpp):
JNIEXPORT jint JNICALL
Java_ravex_addon_jni_JniBridge_nativeAdd(JNIEnv*, jclass, jint a, jint b) {
    return a + b;                           // 42
}

JNIEXPORT jstring JNICALL
Java_ravex_addon_jni_JniBridge_nativeGetPlatformInfo(JNIEnv* env, jclass) {
    std::string info;
#ifdef _WIN32
    info = "Windows " + std::to_string(ADDON_GET_TOTAL_RAM_MB()) + "MB RAM";
#else
    info = "Linux " + std::to_string(ADDON_GET_TOTAL_RAM_MB()) + "MB RAM";
#endif
    return str2jstring(env, info);
}
```

---

## 5. 03 Оверлейное окно

**Цель**: создать прозрачное некликабельное окно поверх игры, которое показывает FPS, информацию о сервере и т.д.

### Windows: Win32 Overlay

Ключевые моменты реализации:

| Аспект | Детали |
|--------|--------|
| Тип окна | Layered Window (`WS_EX_LAYERED`) |
| Прозрачность | `SetLayeredWindowAttributes` + `ULW_ALPHA` |
| Кликабельность | `WS_EX_TRANSPARENT` — пропускает клики мыши |
| Поверх всего | `HWND_TOPMOST` |
| Рендеринг | GDI double-buffering в `m_memDc` |
| Акрил/блур | `DwmEnableBlurBehindWindow` (Win10+) |

**Жизненный цикл кадра:**
```
beginFrame()
  → Clear (черный фон)
  → SetBkMode(TRANSPARENT)

endFrame()
  → Вызов renderCallback(custom rendering)
  → UpdateLayeredWindow() — альфа-блендинг на экран
```

### Linux: X11 Overlay

```cpp
Display* d = XOpenDisplay(nullptr);
Window w = XCreateSimpleWindow(d, root, x, y, w, h, 0, 0, 0);

// Отключаем управление окном от WM (override_redirect)
XSetWindowAttributes attrs;
attrs.override_redirect = True;
XChangeWindowAttributes(d, w, CWOverrideRedirect, &attrs);

// Поверх всех окон
Atom above = XInternAtom(d, "_NET_WM_STATE_ABOVE", False);
XChangeProperty(d, w, XInternAtom(d, "_NET_WM_STATE", False),
    XA_ATOM, 32, PropModeReplace, (unsigned char*)&above, 1);

XMapWindow(d, w);
XFlush(d);
```

### Запуск оверлея из аддона

Оверлей работает в **отдельном потоке** — иначе он заблокирует основной поток аддона:

```cpp
void onLoad(AddonContext* ctx) override {
    running = true;
    overlayThread = new std::thread(&OverlayAddon::overlayLoop, this);
}

void overlayLoop() {
    overlay.create(0, 0, 400, 200);
    while (running) {
        overlay.beginFrame();
        // Обновление метрик (FPS, CPU, RAM)
        overlay.endFrame();
        ADDON_SLEEP(33);  // ~30 FPS
    }
    overlay.destroy();
}
```

---

## 6. 04 GitHub авто-обновление

**Цель**: автоматически проверять новые версии RaveX на GitHub, скачивать и устанавливать их.

Использует библиотеку `ravex_github_tools` из `src/main/cpp/plugins/github/`.

### ReleaseChecker

```cpp
ravex::github::ReleaseChecker checker("StormDevzz", "RaveX");

auto info = checker.checkForUpdates("1.4.3");
// info.available → true/false
// info.remoteVersion → "1.5.1"
// info.matchingAssets → список файлов под твою платформу
```

### ReleaseManager (полный цикл)

```cpp
ravex::github::GithubConfig cfg;
cfg.owner = "StormDevzz";
cfg.repo  = "RaveX";
cfg.currentVersion = "1.4.3";

ravex::github::ReleaseManager manager(cfg);

manager.onProgress([](int64_t dl, int64_t total) {
    printf("\rСкачано: %lld / %lld", dl, total);
});

auto info = manager.check();          // 1. Проверка
auto dl   = manager.download(info);   // 2. Скачивание
manager.install(dl);                  // 3. Установка
manager.rollback();                   // 4. Откат (если нужно)
```

### Как это работает под капотом

| Компонент | Windows | Linux |
|-----------|---------|-------|
| HTTP-клиент | `WinHTTP` (встроен в ОС) | POSIX sockets + OpenSSL |
| JSON-парсер | Собственный (620 строк) | Собственный (тот же код) |
| Semver | `Version::compare` | `Version::compare` |
| Скачивание | `WinHttpReadData` в цикле | `SSL_read` / `recv` |

Библиотека **не требует внешних зависимостей** на Windows (WinHTTP — часть ОС).
На Linux требует `libssl-dev` (OpenSSL).

---

## 7. Кроссплатформенное программирование

### Таблица аналогов

| Задача | Windows | Linux |
|--------|---------|-------|
| Загрузить DLL/SO | `LoadLibraryA("lib.dll")` | `dlopen("lib.so", RTLD_NOW)` |
| Взять функцию | `GetProcAddress(h, "fn")` | `dlsym(h, "fn")` |
| Приоритет процесса | `SetPriorityClass(hProc, HIGH)` | `setpriority(PRIO_PROCESS, 0, -20)` |
| Потоки | `CreateThread` / `winmm` | `pthread_create` |
| Сон | `Sleep(ms)` | `usleep(ms * 1000)` |
| Память системы | `GlobalMemoryStatusEx` | `sysconf(_SC_PHYS_PAGES)` |
| ID процесса | `GetCurrentProcessId()` | `getpid()` |
| Разделитель пути | `\` | `/` |
| Окно поверх игры | Win32 Layered Window | X11 override_redirect |
| HTTP-запросы | WinHTTP | POSIX sockets + OpenSSL |

### Шаблон `#ifdef`

```cpp
void myFunction() {
#ifdef _WIN32
    // Windows-реализация
    SetPriorityClass(GetCurrentProcess(), HIGH_PRIORITY_CLASS);
#else
    // Linux-реализация
    setpriority(PRIO_PROCESS, 0, -20);
#endif
}
```

### Шаблон CMake

```cmake
if(WIN32)
    target_link_libraries(myaddon PRIVATE winmm psapi winhttp)
else()
    find_package(Threads REQUIRED)
    find_package(OpenSSL REQUIRED)
    target_link_libraries(myaddon PRIVATE Threads::Threads OpenSSL::SSL)
endif()
```

---

## 8. JNI: Java + C++

### Сценарий 1: Java вызывает C++

**Java-сторона** (`MyAddon.java`):
```java
package ravex.addon.myaddon;

public class MyAddon implements Addon {
    static {
        System.loadLibrary("FeatureAddon");
    }

    public static native void nativeLog(String message);
    public static native int  nativeGetPid();
    public static native long nativeGetTotalRamMB();
}
```

**C++-сторона** (`main.cpp`):
```cpp
extern "C" JNIEXPORT void JNICALL
Java_ravex_addon_myaddon_MyAddon_nativeLog(JNIEnv* env, jclass, jstring msg) {
    const char* str = env->GetStringUTFChars(msg, nullptr);
    std::cout << "[Native] " << str << std::endl;
    env->ReleaseStringUTFChars(msg, str);
}

extern "C" JNIEXPORT jint JNICALL
Java_ravex_addon_myaddon_MyAddon_nativeGetPid(JNIEnv*, jclass) {
#ifdef _WIN32
    return GetCurrentProcessId();
#else
    return getpid();
#endif
}
```

### Сценарий 2: C++ вызывает Java (Callback)

```cpp
static JavaVM* g_jvm = nullptr;

// 1. Сохраняем VM при загрузке
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_8;
}

// 2. Java регистрирует callback-объект
extern "C" JNIEXPORT void JNICALL
Java_MyAddon_registerCallback(JNIEnv* env, jclass, jobject obj) {
    g_obj = env->NewGlobalRef(obj);  // Не даём GC удалить
    jclass c = env->GetObjectClass(obj);
    g_callback = env->GetMethodID(c, "onNativeEvent", "(Ljava/lang/String;)V");
}

// 3. Вызываем из C++ в любой момент
void fireEvent(const char* data) {
    JNIEnv* env;
    g_jvm->AttachCurrentThread((void**)&env, nullptr);
    env->CallVoidMethod(g_obj, g_callback, env->NewStringUTF(data));
}
```

### JniBridge.hpp — готовые хелперы

В `02_features/JniBridge.hpp` уже есть всё необходимое:

```cpp
// Java string → std::string
std::string str = jstring2str(env, javaString);

// std::string → jstring
jstring js = str2jstring(env, "Hello from C++");

// Безопасный getEnv (attach если нужно)
JNIEnv* env = getEnv();

// Вызвать статический Java-метод
fireCallback("onData", "(Ljava/lang/String;)V", arg);
```

---

## 9. Сборка и установка

### Быстрая сборка (скрипты)

```bash
# Linux
cd templates/cpp/scripts
chmod +x build.sh
./build.sh                       # Собирает 02_features
./build.sh 01_minimal            # Собирает 01_minimal
./build.sh 04_github --install   # Собирает и устанавливает
```

```cmd
REM Windows
cd templates\cpp\scripts
build.bat                        % Собирает 02_features
build.bat 03_overlay             % Собирает 03_overlay
build.bat 01_minimal --install   % Собирает и устанавливает
```

### Ручная сборка (любой пример)

```bash
cd 02_features
mkdir build && cd build

# Linux
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . -j$(nproc)

# Windows (MinGW)
cmake .. -DCMAKE_BUILD_TYPE=Release -G "MinGW Makefiles"
cmake --build .

# Windows (MSVC)
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

### Установка

Готовый `.dll`/`.so` нужно положить в папку нативных аддонов:

| Платформа | Путь |
|-----------|------|
| Windows | `%USERPROFILE%\.minecraft\ravex\addons\native\` |
| Linux | `~/.minecraft/ravex/addons/native/` |

Скрипты делают это автоматически с флагом `--install`.

---

## 10. Справочник API

### RaveX Addon API (`src/main/cpp/addon/include/`)

| Заголовок | Класс/Макросы | Назначение |
|-----------|--------------|------------|
| `Addon.h` | `class Addon` | Базовый класс (onLoad, onUnload, getName, getVersion) |
| `AddonContext.h` | `class AddonContext` | Контекст (logInfo, getAddonName, getDataDir) |
| `AddonConfig.h` | `class AddonConfig` | Конфиг ключ-значение (set, get, save, load) |
| `AddonEvent.h` | `class AddonEvent` | Событие (getName, isCancelled, setCancelled) |
| `AddonListener.h` | `class AddonListener` | Слушатель событий (onEvent) |
| `AddonRegistry.h` | `class AddonRegistry` | Реестр аддонов (registerAddon, findAddon) |
| `AddonThread.h` | `class AddonMutex` | Mutex-обёртка (lock, unlock) |
| `AddonMath.h` | `clamp`, `lerp` | Математические утилиты |
| `AddonVersion.h` | `getApiVersion`, `isCompatible` | Проверка версии API |
| `AddonLogger.h` | `class AddonLogger` | Логирование (info, warn, error) |
| `AddonMeta.h` | `class AddonMeta` | Метаданные аддона |
| `SystemUtils.h` | `getMinecraftDir`, `getAddonsDir` | Файловые утилиты |

### platform.hpp — кроссплатформенные макросы

| Макрос | Windows | Linux |
|--------|---------|-------|
| `ADDON_API` | `__declspec(dllexport)` | `__attribute__((visibility("default")))` |
| `ADDON_SLEEP(ms)` | `Sleep(ms)` | `usleep(ms*1000)` |
| `ADDON_LOAD_LIB(n)` | `LoadLibraryA(n)` | `dlopen(n, RTLD_NOW)` |
| `ADDON_GET_SYM(h,f)` | `GetProcAddress(h,f)` | `dlsym(h,f)` |
| `ADDON_FREE_LIB(h)` | `FreeLibrary(h)` | `dlclose(h)` |
| `ADDON_SET_HIGH_PRIORITY()` | `SetPriorityClass(h, HIGH)` | `setpriority(PRIO_PROC,0,-20)` |
| `ADDON_SET_NORMAL_PRIORITY()` | `SetPriorityClass(h, NORMAL)` | `setpriority(PRIO_PROC,0,0)` |
| `ADDON_TRIM_MEMORY()` | `EmptyWorkingSet(h)` | `malloc_trim(0)` |
| `ADDON_GET_TOTAL_RAM_MB()` | `GlobalMemoryStatusEx` | `sysconf(_SC_PHYS_PAGES)` |
| `ADDON_PATH_SEP` | `'\\'` | `'/'` |
| `ADDON_FORCEINLINE` | `__forceinline` | `__attribute__((always_inline))` |
| `ADDON_DEBUG_BREAK()` | `__debugbreak()` | `__builtin_trap()` |
| `ADDON_LIKELY(x)` | `x` | `__builtin_expect(!!(x),1)` |

### GitHub Tools API (`src/main/cpp/plugins/github/include/`)

| Класс | Методы | Назначение |
|-------|--------|------------|
| `Version` | `fromString`, `toString`, `compare` | Semver-версия |
| `ReleaseChecker` | `checkForUpdates`, `listReleases`, `getLatestRelease` | Проверка обновлений |
| `ReleaseManager` | `check`, `download`, `install`, `rollback` | Полный цикл обновления |
| `HttpClient` | `get`, `post`, `download`, `setToken` | HTTP-запросы |
| `JsonValue` | `parse`, `serialize`, `operator[]`, `asString` | JSON-парсер |

---

## Следующие шаги

1. **Начни с `01_minimal`** — пойми минимальную структуру
2. **Изучи `02_features`** — посмотри на platform.hpp и JNI
3. **Поэкспериментируй с `03_overlay`** — если нужен рендеринг
4. **Используй `04_github`** — если нужно авто-обновление
5. **Читай `src/main/cpp/addon/include/`** — полный API для твоего аддона

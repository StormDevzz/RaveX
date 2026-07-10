# RaveX C Аддоны - руководство

> Версия: 1.0 | Платформа: Linux | Язык: C23

---

## Содержание

1. [Что такое C аддон?](#1-что-такое-c-аддон)
2. [Структура проекта](#2-структура-проекта)
3. [01 Минимальный аддон](#3-01-минимальный-аддон)
4. [02 Полнофункциональный аддон](#4-02-полнофункциональный-аддон)
5. [Сборка и установка](#5-сборка-и-установка)
6. [Справочник API](#6-справочник-api)

---

## 1. Что такое C аддон?

**C аддон** - это динамическая библиотека (`.so`), которую RaveX загружает через `dlopen` в рантайме. В отличие от C++ аддонов, C аддоны работают **только на Linux** и не имеют доступа к JNI напрямую.

### Зачем C, если есть C++ и Java?

| Задача | Java | C++ | C |
|--------|------|-----|---|
| Простота написания | Высокая | Средняя | Средняя |
| Размер бинарника | ~2 KB JAR | ~50-500 KB .so | ~10-50 KB .so |
| Зависимости | Нет | STL + возможны libpthread и т.д. | Только libc |
| Кроссплатформенность | Да | Windows + Linux | Только Linux |
| Минимальный оверхед | Нет (JVM) | Нет | Нет |

C аддоны полезны, когда тебе нужен минимальный бинарник без привязки к C++ рантайму. Например: кастомные проверки, легковесные утилиты, сендеры пакетов.

### Ограничения

- C аддоны работают **только на Linux**
- У C аддона **нет прямого доступа к Java** - только через API, которое предоставляет загрузчик
- Нет RAII, нет исключений - управляй памятью вручную

---

## 2. Структура проекта

```
templates/c/
├── 01_minimal/               # 1. Минимальный аддон
│   ├── CMakeLists.txt          # Конфигурация сборки
│   └── src/addon.c             # Исходный код
├── 02_features/              # 2. Аддон с tick и key обработчиками
│   ├── CMakeLists.txt
│   └── src/addon.c
├── scripts/
│   └── build.sh               # Скрипт сборки для Linux
├── GUIDE.md                   # Документация (английский)
├── GUIDE_RU.md                # Документация (русский)
└── README.md
```

---

## 3. 01 Минимальный аддон

Файл: `templates/c/01_minimal/src/addon.c`

```c
#include "../../../src/main/c/addon/include/c_addon_api.h"
#include <stdio.h>

static ravex_c_addon_api* g_api = NULL;

int ravex_c_addon_init(ravex_c_addon_api* api) {
    g_api = api;
    if (g_api && g_api->log_info)
        g_api->log_info("MinimalCAddon loaded");
    return 0;
}

void ravex_c_addon_shutdown(void) {
    if (g_api && g_api->log_info)
        g_api->log_info("MinimalCAddon unloaded");
    g_api = NULL;
}

ravex_c_addon_meta ravex_c_addon_meta_info = {
    .api_version = RAVEX_C_ADDON_API_VERSION,
    .name        = "MinimalCAddon",
    .version     = "1.0.0",
    .description = "Minimal C addon",
    .author      = "You"
};
```

### Что здесь происходит?

- `ravex_c_addon_meta_info` - обязательная глобальная структура. Загрузчик читает её через `dlsym`, чтобы узнать имя, версию и описание аддона.
- `ravex_c_addon_init` - вызывается сразу после загрузки. Получает указатель на API-структуру с функциями логирования. Должна вернуть 0 при успехе.
- `ravex_c_addon_shutdown` - вызывается при выгрузке аддона. Освободи здесь все ресурсы.

### Как это работает

1. RaveX находит `.so` с префиксом `c_addon_` в папке `RaveX/addons/c_native/`
2. Загружает его через `dlopen`
3. Ищет символ `ravex_c_addon_meta_info`, читает метаданные
4. Вызывает `ravex_c_addon_init`, передавая указатель на API
5. При завершении вызывает `ravex_c_addon_shutdown` и делает `dlclose`

---

## 4. 02 Полнофункциональный аддон

Файл: `templates/c/02_features/src/addon.c`

```c
#include "../../../src/main/c/addon/include/c_addon_api.h"
#include <stdio.h>

static ravex_c_addon_api* g_api = NULL;
static int g_tick_count = 0;
static int g_key_presses = 0;

int ravex_c_addon_init(ravex_c_addon_api* api) {
    g_api = api;
    if (g_api && g_api->log_info)
        g_api->log_info("FeatureCAddon loaded");
    return 0;
}

void ravex_c_addon_shutdown(void) {
    if (g_api && g_api->log_info) {
        char buf[128];
        snprintf(buf, sizeof(buf),
            "FeatureCAddon unloaded. Ticks: %d, key presses: %d",
            g_tick_count, g_key_presses);
        g_api->log_info(buf);
    }
    g_api = NULL;
}

void ravex_c_addon_on_tick(void) {
    g_tick_count++;
}

void ravex_c_addon_on_key(int key, int action) {
    if (action == 1) {
        g_key_presses++;
        if (g_api && g_api->log_info) {
            char buf[64];
            snprintf(buf, sizeof(buf), "FeatureCAddon: key %d pressed", key);
            g_api->log_info(buf);
        }
    }
}

ravex_c_addon_meta ravex_c_addon_meta_info = {
    .api_version = RAVEX_C_ADDON_API_VERSION,
    .name        = "FeatureCAddon",
    .version     = "1.0.0",
    .description = "C addon with tick and key handlers",
    .author      = "You"
};
```

### Что добавилось?

- `ravex_c_addon_on_tick` - вызывается каждый игровой тик. Опциональна.
- `ravex_c_addon_on_key` - вызывается при нажатии/отпускании клавиши. `action == 1` - нажатие, `0` - отпускание. Опциональна.

Обе функции опциональны. Если ты их не экспортируешь, загрузчик просто их не вызывает.

---

## 5. Сборка и установка

### Требования

- Linux
- GCC или Clang
- CMake >= 3.16

### Сборка примера

```bash
cd templates/c
chmod +x scripts/build.sh
./scripts/build.sh 01_minimal
```

Если хочешь сразу установить в Minecraft:

```bash
./scripts/build.sh 01_minimal --install
```

Аддон появится в `~/.minecraft/RaveX/addons/c_native/c_addon_minimal.so`.

### Сборка своего аддона

Скопируй папку `01_minimal`, переименуй, напиши свой код. Главное - сохрани префикс `c_addon_` в имени `.so` файла, иначе загрузчик его не увидит.

```
my_addon/
├── CMakeLists.txt
└── src/addon.c
```

В `CMakeLists.txt` обязательно укажи:

```cmake
set_target_properties(my_addon PROPERTIES
    OUTPUT_NAME "c_addon_my_addon"
    PREFIX ""
    SUFFIX ".so"
)
```

Имя `.so` должно начинаться с `c_addon_`.

---

## 6. Справочник API

### Заголовочный файл

Путь: `src/main/c/addon/include/c_addon_api.h`

Подключается относительным путём из шаблона:

```c
#include "../../../src/main/c/addon/include/c_addon_api.h"
```

### Структуры

```c
typedef struct {
    int         api_version;     // Must be RAVEX_C_ADDON_API_VERSION
    const char* name;            // Addon name
    const char* version;         // Version string
    const char* description;     // Short description
    const char* author;          // Author name
} ravex_c_addon_meta;
```

### API, передаваемое в init

```c
typedef struct {
    void (*log_info)(const char* msg);       // Log at info level
    void (*log_warn)(const char* msg);       // Log at warning level
    void (*log_error)(const char* msg);      // Log at error level
    const char* (*get_mc_version)(void);     // Get Minecraft version
    bool (*is_key_down)(int key_code);       // Check if key is held
} ravex_c_addon_api;
```

### Обязательные символы для экспорта

| Символ | Тип | Описание |
|--------|-----|----------|
| `ravex_c_addon_meta_info` | `ravex_c_addon_meta` | Метаданные аддона (глобальная переменная) |
| `ravex_c_addon_init` | `int (*)(ravex_c_addon_api*)` | Инициализация, вернуть 0 при успехе |
| `ravex_c_addon_shutdown` | `void (*)(void)` | Очистка при выгрузке |

### Опциональные символы для экспорта

| Символ | Тип | Описание |
|--------|------|----------|
| `ravex_c_addon_on_tick` | `void (*)(void)` | Вызывается каждый тик |
| `ravex_c_addon_on_key` | `void (*)(int key, int action)` | Событие клавиатуры |

---

## 7. Подпись и безопасность

Все аддоны (Java, C++, C) должны быть подписаны перед загрузкой. Неподписанные аддоны отклоняются.

### Как работает подпись

1. RaveX содержит встроенный RSA-2048 публичный ключ
2. Каждый файл аддона имеет файл подписи с расширением `.ravex-sig`
3. Подпись - это SHA-256 хеш файла аддона, зашифрованный соответствующим приватным ключом
4. Загрузчик проверяет подпись перед загрузкой аддона

### Генерация ключей

```bash
cd RaveX
javac -d /tmp/keys src/main/java/ravex/addon/security/KeyGenerator.java \
    src/main/java/ravex/addon/security/AddonSignature.java \
    src/main/java/ravex/addon/security/AddonSigner.java \
    src/main/java/ravex/addon/util/AddonException.java
java -cp /tmp/keys ravex.addon.security.KeyGenerator /path/to/output
```

Создаются `public.der` и `private.der`. Скопируй `public.der` в `src/main/resources/assets/ravex/security/` перед сборкой RaveX.

### Подпись аддона

```bash
java -cp /tmp/keys ravex.addon.security.AddonSigner \
    /path/to/private.der \
    /path/to/c_addon_my_addon.so
```

Создаёт `c_addon_my_addon.so.ravex-sig` рядом с файлом аддона.

### Песочница для Java аддонов

Java аддоны загружаются через `SecureAddonClassLoader`, который блокирует:

- Reflection API (`java.lang.reflect`, `java.lang.invoke`)
- Запуск процессов (`java.lang.Runtime`, `java.lang.ProcessBuilder`)
- Запись в файловую систему (`java.io.FileOutputStream`, `java.io.FileWriter` и т.д.)
- Сетевой доступ (`java.net.Socket`, `java.net.HttpURLConnection`)
- Security и crypto API

C и C++ аддоны изолированы архитектурно:
- C аддоны не имеют прямого JNI доступа - только ограниченная структура `ravex_c_addon_api`
- C++ аддоны загружаются как отдельные shared library с контролируемыми точками входа

### Структура файлов

```
~/.minecraft/RaveX/addons/
├── my_addon.jar              # Java аддон
├── my_addon.jar.ravex-sig    # Подпись Java аддона
├── native/
│   ├── MyAddon.so            # C++ аддон
│   └── MyAddon.so.ravex-sig  # Подпись C++ аддона
└── c_native/
    ├── c_addon_my.so         # C аддон
    └── c_addon_my.so.ravex-sig # Подпись C аддона
```

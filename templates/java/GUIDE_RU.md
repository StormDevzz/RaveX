# RaveX Java Аддоны — Полное руководство

> Версия: 1.0 | Платформы: Windows 10/11, Linux | Язык: Java 17+

---

## Содержание

1. [Что такое Java-аддон?](#1-что-такое-java-аддон)
2. [Структура проекта](#2-структура-проекта)
3. [MainAddon.java — главный класс](#3-mainaddonjava--главный-класс)
4. [DemoModule.java — модуль](#4-demomodulejava--модуль)
5. [MANIFEST.MF — манифест JAR](#5-manifestmf--манифест-jar)
6. [Кроссплатформенное программирование](#6-кроссплатформенное-программирование)
7. [Интеграция с C++ через JNI](#7-интеграция-с-c-через-jni)
8. [Сборка и установка](#8-сборка-и-установка)
9. [API аддонов RaveX](#9-api-аддонов-ravex)
10. [Часто задаваемые вопросы](#10-часто-задаваемые-вопросы)

---

## 1. Что такое Java-аддон?

**Java-аддон** — это JAR-файл, который RaveX загружает в рантайме через кастомный ClassLoader.

### Зачем Java, если есть C++?

| Аспект | Java-аддон | C++ нативный аддон |
|--------|-----------|-------------------|
| Сложность разработки | Низкая | Высокая |
| Кроссплатформенность | Да "Write once, run anywhere" | Две реализации |
| Доступ к API RaveX | Да Полный | Да Через JNI |
| Производительность | Средняя | Высокая |
| Системные вызовы | Нет (Sandbox) | Да Win32 / X11 |
| Оверлей | Нет | Да Win32 / X11 |
| Размер бинарника | Да Маленький (JAR) | Средний (DLL/SO) |
| Обновление без перезапуска | Нет | Нет |

**Когда выбирать Java:**
- Простая логика (команды, GUI, слушатели событий)
- Нет потребности в системных вызовах
- Быстрый прототип
- Интеграция с существующими Java-библиотеками

**Когда выбирать C++:**
- Максимальная производительность
- Оверлейные окна
- Работа с памятью процессов
- Аппаратное ускорение

---

## 2. Структура проекта

```
templates/java/
├── src/
│   ├── ravex/addon/template/
│   │   ├── MainAddon.java       # Главный класс аддона
│   │   └── DemoModule.java      # Пример модуля
│   └── META-INF/
│       └── MANIFEST.MF          # Манифест JAR
├── scripts/
│   ├── build.bat                # Сборка под Windows
│   └── build.sh                 # Сборка под Linux
├── GUIDE.md                     # This guide (English)
├── GUIDE_RU.md                  # Это руководство (Russian)
└── README.md                    # Быстрый старт
```

### Что куда?

| Файл | Назначение |
|------|------------|
| `MainAddon.java` | Точка входа: инициализация, загрузка натива, регистрация модулей |
| `DemoModule.java` | Модуль с параметрами и платформенными ветками |
| `MANIFEST.MF` | JAR-манифест с указанием главного класса |
| `build.bat` / `build.sh` | Сборка и установка в один клик |

---

## 3. MainAddon.java — главный класс

### Обязательный минимум

```java
package ravex.addon.template;

import ravex.addon.Addon;
import ravex.addon.AddonContext;
import ravex.addon.AddonInfo;

public class MainAddon implements Addon {

    @Override
    public void onLoad(AddonContext context) {
        // Инициализация
    }

    @Override
    public void onUnload() {
        // Очистка
    }

    @Override
    public AddonInfo getAddonInfo() {
        return new AddonInfo(
            "MainAddon",        // Имя
            "Description",      // Описание
            "1.4.1",           // Версия
            "Author",          // Автор
            "ravex.addon.template.MainAddon"  // Главный класс
        );
    }
}
```

### Определение платформы

```java
public static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
}

public static String getPathSep() {
    return isWindows() ? "\\" : "/";
}

public static String getNativeDir() {
    String mcBase = System.getProperty("user.home")
        + getPathSep() + ".minecraft";
    return mcBase + getPathSep() + "ravex"
        + getPathSep() + "addons" + getPathSep() + "native";
}
```

### Загрузка нативной библиотеки

```java
private void loadNativeLibrary() {
    // System.mapLibraryName даёт "MyAddon.dll" или "libMyAddon.so"
    String path = nativeDir + getPathSep()
        + System.mapLibraryName("MyAddon");

    try {
        System.load(path);         // Полный путь
    } catch (UnsatisfiedLinkError e) {
        try {
            System.loadLibrary("MyAddon");  // java.library.path
        } catch (UnsatisfiedLinkError e2) {
            // Аддон продолжает работу без натива
        }
    }
}
```

---

## 4. DemoModule.java — модуль

### Базовая структура

```java
public class DemoModule extends AddonModule {

    public DemoModule() {
        super("DemoModule", "Демо", AddonModuleInfo.Category.CUSTOM);
    }

    @Override
    public void onEnable() { /* Включение */ }
    @Override
    public void onDisable() { /* Выключение */ }
    @Override
    public void onTick() { /* Каждый тик */ }
}
```

### Параметры

Параметры автоматически отображаются в GUI RaveX:

```java
private final BooleanParameter enabled = new BooleanParameter("enabled", true);
private final NumberParameter  speed   = new NumberParameter("speed", 1.0, 0.1, 5.0);
private final StringParameter  mode    = new StringParameter("mode", "default");
private final ColorParameter   color   = new ColorParameter("color", 0x00FF00);
```

### Платформенные ветки в onTick

```java
@Override
public void onTick() {
    if (MainAddon.isWindows()) {
        tickWindows();   // Windows-специфичная логика
    } else {
        tickLinux();     // Linux-специфичная логика
    }
}
```

---

## 5. MANIFEST.MF — манифест JAR

```mf
Addon-Name: MainAddon
Addon-Version: 1.0
Addon-Author: RaveX Team
Addon-Main-Class: ravex.addon.template.MainAddon
```

**Важно:**
- `Addon-Main-Class` — полное имя класса, реализующего `Addon`
- Файл должен лежать в `META-INF/MANIFEST.MF` внутри JAR
- Сборщик (`jar cfm`) сам добавляет стандартные поля `Manifest-Version` и `Created-By`

---

## 6. Кроссплатформенное программирование

### Определение ОС в Java

```java
String os = System.getProperty("os.name").toLowerCase();

if (os.contains("win")) {
    // Windows
} else if (os.contains("nix") || os.contains("nux")) {
    // Linux
} else if (os.contains("mac")) {
    // macOS
}
```

### Пути к файлам

| Операция | Windows | Linux |
|----------|---------|-------|
| Разделитель | `\` | `/` |
| Домашняя папка | `C:\Users\Имя` | `/home/имя` |
| Minecraft | `%USERPROFILE%\.minecraft` | `~/.minecraft` |
| Аддоны | `%USERPROFILE%\.minecraft\ravex\addons\` | `~/.minecraft/ravex/addons/` |
| Нативные | `%USERPROFILE%\.minecraft\ravex\addons\native\` | `~/.minecraft/ravex/addons/native/` |

### Загрузка нативных библиотек

| Платформа | Имя файла | Java-имя |
|-----------|-----------|----------|
| Windows | `MyAddon.dll` | `"MyAddon"` → `System.mapLibraryName` → `"MyAddon.dll"` |
| Linux | `libMyAddon.so` | `"MyAddon"` → `System.mapLibraryName` → `"libMyAddon.so"` |

---

## 7. Интеграция с C++ через JNI

Мощь RaveX — в связке Java + C++. Java-аддон может загрузить нативную библиотеку и вызывать её функции.

### Шаг 1: C++ пишет native-функции

В файле `02_features/JniBridge.cpp`:

```cpp
extern "C" JNIEXPORT jint JNICALL
Java_ravex_addon_jni_JniBridge_nativeAdd(JNIEnv*, jclass, jint a, jint b) {
    return a + b;
}

extern "C" JNIEXPORT jstring JNICALL
Java_ravex_addon_jni_JniBridge_nativeGetPlatformInfo(JNIEnv* env, jclass) {
    // Платформенная информация прямо из C++
    return env->NewStringUTF("Windows 10, 16384MB RAM");
}
```

### Шаг 2: Java объявляет native-методы

```java
package ravex.addon.jni;

public class JniBridge {
    static { System.loadLibrary("FeatureAddon"); }

    public static native int  nativeAdd(int a, int b);
    public static native String nativeGetPlatformInfo();
}
```

### Шаг 3: Вызов из Java

```java
int sum = JniBridge.nativeAdd(40, 2);           // 42
String info = JniBridge.nativeGetPlatformInfo(); // "Windows 10, 16384MB RAM"
```

### Вызов Java из C++ (Callback)

```cpp
// C++ вызывает Java-метод обратно
void fireEvent(const char* data) {
    JNIEnv* env;
    g_jvm->AttachCurrentThread((void**)&env, nullptr);
    env->CallVoidMethod(g_obj, g_callback, env->NewStringUTF(data));
}
```

```java
// Java принимает callback
public class MyAddon implements Addon {
    public void onNativeEvent(String data) {
        System.out.println("C++ сказал: " + data);
    }
}
```

### Когда использовать JNI?

| Сценарий | Java | JNI → C++ |
|----------|------|-----------|
| Оверлей | Нет | Да Win32/X11 |
| HTTP-запросы | Да `java.net.URL` | Да WinHTTP/sockets |
| Парсинг JSON | Да Gson/Jackson | Да Собственный парсер |
| FPS-счётчик | Нет | Да Чтение памяти |
| Системная информация | Нет | Да Win32 API / POSIX |
| Работа с файлами | Да `java.nio.file` | Да `fstream` |

---

## 8. Сборка и установка

### Быстрая сборка (скрипты)

```bash
# Linux
cd templates/java/scripts
chmod +x build.sh
./build.sh                    # Сборка
./build.sh --install          # Сборка + установка
```

```cmd
REM Windows
cd templates\java\scripts
build.bat                     % Сборка
build.bat --install           % Сборка + установка
```

### Ручная сборка

```bash
cd templates/java

# 1. Собрать RaveX (если ещё не собран)
cd ../..
./gradlew build

# 2. Скомпилировать аддон
cd templates/java
javac -cp ../../build/libs/RaveX.jar \
    -d build/classes \
    src/ravex/addon/template/MainAddon.java \
    src/ravex/addon/template/DemoModule.java

# 3. Скопировать манифест
cp src/META-INF/MANIFEST.MF build/classes/META-INF/

# 4. Упаковать JAR
cd build/classes
jar cfm ../MainAddon.jar META-INF/MANIFEST.MF ravex/*.class
```

### Установка

Готовый JAR нужно положить в папку аддонов:

| Платформа | Путь |
|-----------|------|
| Windows | `%USERPROFILE%\.minecraft\ravex\addons\` |
| Linux | `~/.minecraft/ravex/addons/` |

---

## 9. API аддонов RaveX

### Основные интерфейсы

| Интерфейс | Методы | Назначение |
|-----------|--------|------------|
| `Addon` | `onLoad`, `onUnload`, `getAddonInfo` | Главный класс аддона |
| `AddonModule` | `onEnable`, `onDisable`, `onTick` | Модуль (функциональность) |
| `AddonListener` | `onEvent` | Слушатель событий |

### Управление

| Класс | Методы | Назначение |
|-------|--------|------------|
| `AddonContext` | `getLogger`, `getAddonName`, `getDataDir` | Контекст аддона |
| `AddonModuleManager` | `registerModule`, `unregisterModule`, `getLogger` | Регистрация модулей |
| `AddonInfo` | (конструктор с name, description, version, author, mainClass) | Метаданные |

### Параметры модуля

| Класс | Тип | Пример |
|-------|-----|--------|
| `BooleanParameter` | `boolean` | `new BooleanParameter("enabled", true)` |
| `NumberParameter` | `double` | `new NumberParameter("speed", 1.0, 0.1, 5.0)` |
| `StringParameter` | `String` | `new StringParameter("mode", "default")` |
| `ColorParameter` | `int` (0xRRGGBB) | `new ColorParameter("color", 0x00FF00)` |

---

## 10. Часто задаваемые вопросы

### ❓ Мой аддон не загружается

1. Проверь `MANIFEST.MF` — правильный ли `Addon-Main-Class`?
2. Проверь консоль Minecraft (`.minecraft/logs/latest.log`)
3. Попробуй `java -jar MyAddon.jar` — ошибка компиляции?

### ❓ Не загружается нативная библиотека

1. Проверь архитектуру: 64-битная Java требует 64-битную DLL
2. Проверь путь: `C:/Users/.../.minecraft/ravex/addons/native/MyAddon.dll`
3. Проверь зависимости: `Dependency Walker` (Windows) или `ldd` (Linux)

### ❓ Как отлаживать?

```java
getLogger().info("value = " + value);       // В лог RaveX
System.out.println("value = " + value);     // В stdout (консоль лаунчера)
```

### ❓ Hot-reload?

Java-аддоны **не поддерживают** hot-reload. Нужно:
1. Выключить аддон в GUI RaveX
2. Заменить JAR-файл
3. Перезапустить майнкрафт
4. Включить аддон

### ❓ Что делать, если нужно больше скорости?

Вынеси тяжёлые вычисления в C++ через JNI:
1. Напиши C++ библиотеку (см. `02_features/`)
2. Объяви `native` методы в Java
3. Вызывай их из модуля

---

## Дальнейшие шаги

1. **Собери пример** — `cd scripts && build.bat` (Windows) или `./build.sh` (Linux)
2. **Изучи `MainAddon.java`** — пойми жизненный цикл
3. **Добавь свои модули** — по образу `DemoModule.java`
4. **Интегрируй с C++** — загрузи нативную библиотеку через `System.load()`
5. **Посмотри примеры C++** — в `templates/cpp/` для продвинутых задач

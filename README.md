# RaveX

**Разработчик: StormDevzz** (github.com/StormDevzz)

---

## RU

RaveX — клиентский мод для Minecraft от организации StormDevzz.

### Языки в проекте

| Язык | Назначение |
|---|---|
| **Java** | Основная логика мода — ClickGUI, модули, миксины, рендеринг |
| **C++** | Нативный код для производительных операций: оптимизатор, анти-AFK, хуки шейдеров, JNI-мост |
| **Lua** | Скриптинг — rich presence, взаимодействие с Discord, кастомные модули |
| **Makefile** | Генерируется CMake для сборки C++ компонентов (native-библиотеки) |

### Сборка

```bash
./gradlew build
```

Готовый JAR находится в `build/libs/`.
Готовые сборки также доступны в разделе Releases.

### Установка

1. Установите Fabric Loader
2. Поместите JAR в папку `mods`
3. Запустите игру

### Аддоны

Вы можете создавать собственные Java и C++ дополнения для RaveX. Подробные руководства по созданию, сборке и установке аддонов доступны в соответствующих папках шаблонов:
* [Руководство по Java-аддонам](templates/java/README_RU.md) (English version: [Java Addons Guide](templates/java/README.md))
* [Руководство по C++ нативным аддонам](templates/cpp/README_RU.md) (English version: [C++ Addons Guide](templates/cpp/README.md))

### Лицензия

GNU General Public License v3.0 — см. файл LICENSE.

---

[English version →](README_EN.md)

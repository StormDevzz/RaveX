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
| **Lua** | Скриптинг — rich presence, взаимодействие с Discord, кастомные модули и Lua-аддоны |
| **Makefile** | Генерируется CMake для сборки C++ компонентов (native-библиотеки) |

### Сборка

```bash
./gradlew build
```

Готовый JAR находится в `build/libs/`.
Готовые сборки также доступны в разделе Releases.

### Установка

1. Установи Fabric Loader
2. Помести JAR в папку `mods`
3. Запусти игру

### Аддоны

Ты можешь создавать собственные Java, C++ и Lua дополнения для RaveX. Подробные руководства по созданию, сборке и установке аддонов доступны в соответствующих папках шаблонов:
* [Руководство по Java-аддонам](templates/java/GUIDE_RU.md) (English version: [Java Addons Guide](templates/java/GUIDE.md))
* [Руководство по C++ нативным аддонам](templates/cpp/GUIDE_RU.md) (English version: [C++ Addons Guide](templates/cpp/GUIDE.md))
* [Руководство по Lua-аддонам](templates/lua/GUIDE_RU.md) (English version: [Lua Addons Guide](templates/lua/GUIDE.md))

### Лицензия

GNU General Public License v3.0 — см. файл LICENSE.

---

[English version →](README_EN.md)

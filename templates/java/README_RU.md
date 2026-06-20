# Сборка и установка Java-аддонов RaveX

Этот каталог содержит шаблон Java-аддона для RaveX, который добавляет категорию `Custom` и модуль `Another`.

Аддоны спроектированы так, чтобы работать **только совместно с RaveX**. Без установленного клиента RaveX они функционировать не будут.

---

## Папка установки

Собранный JAR-файл аддона должен быть помещен в специальную папку `ravex/addons/` внутри каталога твоего инстанса (профиля) Minecraft.

| Лаунчер / ОС | Путь к папке аддонов |
| :--- | :--- |
| **Обычный лаунчер** | `<Minecraft-game-directory>/ravex/addons/` |
| **Prism Launcher (Flatpak на Linux)** | `~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/<Имя_Инстанса>/minecraft/ravex/addons/` |

> [!WARNING]
> Не помещай JAR-файл аддона в папку `mods/` лаунчера. Это вызовет ошибку Fabric Loader (Found 1 non-fabric mod) и аддон не будет загружен клиентом RaveX.

---

## Инструкция по сборке

### Требования
* Установленный JDK 21 или новее.
* Собранная библиотека клиента RaveX (`ravex-1.4.jar`).

### Шаги сборки

1. **Собери основной клиент RaveX** в корне проекта:
   ```bash
   ./gradlew build
   ```
   Файл библиотеки появится по пути `build/libs/ravex-1.4.jar`.

2. **Скомпилируй исходный код аддона**:
   ```bash
   mkdir -p build
   javac -cp "../../build/libs/ravex-1.4.jar" -d build AnotherAddon.java AnotherModule.java
   ```

3. **Сформируй файл манифеста** (`MANIFEST.MF`):
   ```manifest
   Manifest-Version: 1.0
   Addon-Name: AnotherAddon
   Addon-Version: 1.0.0
   Addon-Author: RaveXDeveloper
   Addon-Main-Class: ravex.addon.template.AnotherAddon
   ```
   *(Убедитесь, что в конце файла манифеста оставлена одна пустая строка).*

4. **Упакуйте классы в итоговый JAR-файл**:
   ```bash
   jar cfm AnotherAddon.jar MANIFEST.MF -C build .
   ```

5. **Скопируй `AnotherAddon.jar`** в папку `ravex/addons/` твоего инстанса Minecraft.

> [!TIP]
> Все шаги компиляции и копирования автоматизированы в скрипте [build.sh](file:///home/nprevenant/RaveX/templates/java/build.sh). Можешь запустить его локально.

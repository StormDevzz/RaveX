// ══════════════════════════════════════════════════════════════════════════════
//  DemoModule.java
//
//  RU: Пример модуля RaveX. Модуль — это подключаемая часть аддона,
//      которую пользователь может включать/выключать в GUI.
//
//      Демонстрирует:
//        1. Параметры модуля (BooleanParameter, NumberParameter)
//        2. Платформенные ветки в onTick() — разное поведение
//           на Windows и Linux
//        3. Жизненный цикл модуля (onEnable, onDisable, onTick)
//        4. Логирование через AddonModuleManager.getLogger()
//
//  EN: Example RaveX module. A module is a pluggable part of an addon
//      that the user can enable/disable in the GUI.
//
//      Demonstrates:
//        1. Module parameters (BooleanParameter, NumberParameter)
//        2. Platform branches in onTick() — different behavior
//           on Windows and Linux
//        3. Module lifecycle (onEnable, onDisable, onTick)
//        4. Logging via AddonModuleManager.getLogger()
// ══════════════════════════════════════════════════════════════════════════════

package ravex.addon.template;

// RU: Импорты RaveX API.
//     AddonModule — базовый класс для всех модулей.
//     AddonModuleInfo — метаданные модуля (категория для GUI).
//     AddonModuleManager — менеджер модулей (логгер и управление).
//     BooleanParameter / NumberParameter — типы параметров модуля,
//     которые отображаются в GUI RaveX как переключатели/ползунки.
// EN: RaveX API imports.
//     AddonModule — base class for all modules.
//     AddonModuleInfo — module metadata (GUI category).
//     AddonModuleManager — module manager (logger and control).
//     BooleanParameter / NumberParameter — module parameter types
//     displayed in the RaveX GUI as toggles/sliders.
import ravex.addon.AddonContext;
import ravex.addon.AddonModule;
import ravex.addon.AddonModuleInfo;
import ravex.addon.AddonModuleManager;
import ravex.addon.module.BooleanParameter;
import ravex.addon.module.NumberParameter;

public class DemoModule extends AddonModule {

    // ─── Параметры модуля / Module parameters ──────────────────────────────
    //
    // RU: Параметры отображаются в GUI RaveX и могут быть изменены
    //     пользователем. Они автоматически сохраняются и загружаются.
    //
    //     BooleanParameter: переключатель (true/false).
    //       - Первый аргумент — уникальное имя (для сохранения в конфиг)
    //       - Второй — значение по умолчанию
    //
    //     NumberParameter: числовой ползунок.
    //       - Имя, значение по умолчанию, минимум, максимум
    //       - В GUI отображается как слайдер
    //
    // EN: Module parameters are displayed in the RaveX GUI and can be
    //     changed by the user. They are automatically saved and loaded.
    //
    //     BooleanParameter: toggle (true/false).
    //       - First argument — unique name (for config persistence)
    //       - Second — default value
    //
    //     NumberParameter: numeric slider.
    //       - Name, default value, minimum, maximum
    //       - Displayed in the GUI as a slider

    private final BooleanParameter someSetting = new BooleanParameter("someSetting", true);
    private final NumberParameter  speedMul    = new NumberParameter("speedMultiplier", 1.5, 0.1, 5.0);

    // ─── Конструктор / Constructor ─────────────────────────────────────────

    // RU: Вызывается при создании модуля.
    //     Передаём имя, описание и категорию (для группировки в GUI).
    //     Добавляем параметры, чтобы они появились в интерфейсе.
    //
    // EN: Called when the module is created.
    //     Pass name, description and category (for GUI grouping).
    //     Add parameters so they appear in the interface.

    public DemoModule() {
        super(
            "DemoModule",
            "Демонстрационный модуль",
            AddonModuleInfo.Category.CUSTOM
        );

        addParameter(someSetting);
        addParameter(speedMul);
    }

    // ─── Жизненный цикл / Lifecycle ────────────────────────────────────────
    //
    // RU: onEnable — вызывается при включении модуля (пользователем в GUI).
    //     onDisable — при выключении.
    //     onTick — вызывается каждый игровой тик (~20 раз/сек).
    //
    // EN: onEnable — called when the module is enabled (by user in GUI).
    //     onDisable — when disabled.
    //     onTick — called every game tick (~20 times/sec).

    @Override
    public void onEnable() {
        log("DemoModule включён!");
        log("someSetting = " + someSetting.getValue());
        log("speedMultiplier = " + speedMul.getValue());
    }

    @Override
    public void onDisable() {
        log("DemoModule выключен.");
    }

    // RU: onTick — вызывается каждый тик. Здесь мы используем платформенную
    //     ветку: на Windows выполняем tickWindows(), на Linux — tickLinux().
    //     Это позволяет иметь разную реализацию для разных ОС, не смешивая
    //     код в одном методе.
    // EN: onTick — called every tick. Here we use platform branching:
    //     on Windows call tickWindows(), on Linux — tickLinux().
    //     This allows different implementations for different OSes
    //     without mixing code in one method.
    @Override
    public void onTick() {
        if (MainAddon.isWindows()) {
            tickWindows();
        } else {
            tickLinux();
        }
    }

    // ─── Платформенные реализации / Platform implementations ──────────────
    //
    // RU: Эти методы содержат платформенно-специфичную логику.
    //     tickWindows() — для Win32 API через JNI.
    //     tickLinux() — для X11/POSIX через JNI.
    //
    //     В реальном аддоне здесь может быть:
    //       - Чтение памяти процесса (Windows: ReadProcessMemory)
    //       - Получение информации об окнах (Windows: EnumWindows)
    //       - Взаимодействие с X11 (Linux: XGetInputFocus)
    //       - HTTP-запросы или парсинг данных
    //
    // EN: These methods contain platform-specific logic.
    //     tickWindows() — for Win32 API via JNI.
    //     tickLinux() — for X11/POSIX via JNI.
    //
    //     In a real addon this could be:
    //       - Process memory reading (Windows: ReadProcessMemory)
    //       - Window info retrieval (Windows: EnumWindows)
    //       - X11 interaction (Linux: XGetInputFocus)
    //       - HTTP requests or data parsing

    private void tickWindows() {
        // RU: Специфичная для Windows логика.
        //     Например: вызов native-методов через JNI.
        // EN: Windows-specific logic.
        //     For example: calling native methods via JNI.
    }

    private void tickLinux() {
        // RU: Специфичная для Linux логика.
        // EN: Linux-specific logic.
    }

    // ─── Утилиты / Utilities ───────────────────────────────────────────────

    // RU: Удобный метод для логирования с префиксом имени модуля.
    //     Использует AddonModuleManager.getLogger() — стандартный
    //     логгер RaveX для модулей.
    // EN: Convenience method for logging with module name prefix.
    //     Uses AddonModuleManager.getLogger() — the standard
    //     RaveX logger for modules.
    private void log(String msg) {
        AddonModuleManager.getLogger().info("[" + getName() + "] " + msg);
    }
}

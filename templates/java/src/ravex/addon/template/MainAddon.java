// ══════════════════════════════════════════════════════════════════════════════
//  MainAddon.java
//
//  RU: Главный класс Java-аддона для RaveX. Демонстрирует:
//        1. Определение платформы (Windows / Linux)
//        2. Загрузку нативной библиотеки (.dll или .so)
//        3. Работу с AddonContext (логгер, регистрация модулей)
//        4. Регистрацию модулей (DemoModule)
//        5. Метаданные аддона (AddonInfo)
//
//      Этот класс реализует интерфейс Addon — точки входа,
//      которые вызывает RaveX при загрузке/выгрузке аддона.
//
//  EN: Main Java addon class for RaveX. Demonstrates:
//        1. Platform detection (Windows / Linux)
//        2. Native library loading (.dll or .so)
//        3. AddonContext usage (logger, module registration)
//        4. Module registration (DemoModule)
//        5. Addon metadata (AddonInfo)
//
//      This class implements the Addon interface — entry points
//      called by RaveX when loading/unloading the addon.
// ══════════════════════════════════════════════════════════════════════════════

package ravex.addon.template;

// RU: Импортируем классы RaveX API. Addon — интерфейс, который
//     должен реализовать каждый аддон. AddonContext — контекст
//     для взаимодействия с ядром. AddonInfo — метаданные аддона.
// EN: Import RaveX API classes. Addon is the interface that
//     every addon must implement. AddonContext is the context
//     for interacting with the core. AddonInfo is addon metadata.
import ravex.addon.Addon;
import ravex.addon.AddonContext;
import ravex.addon.AddonInfo;

public class MainAddon implements Addon {

    // RU: Храним контекст для использования в методах класса.
    // EN: Store context for use in class methods.
    private AddonContext ctx;

    // ─── Определение платформы / Platform detection ─────────────────────────
    //
    // RU: Методы для определения операционной системы.
    //     Используем System.getProperty("os.name") — стандартный
    //     способ в Java. На Windows os.name содержит "Windows".
    //     Эти методы нужны для платформенно-зависимых ветвлений.
    //
    // EN: Methods for operating system detection.
    //     Use System.getProperty("os.name") — the standard way
    //     in Java. On Windows os.name contains "Windows".
    //     These methods are needed for platform-specific branching.

    // RU: Определяет, запущены ли мы на Windows.
    //     os.name может быть "Windows 10", "Windows 11" и т.д.
    //     contains("win") работает для всех версий.
    // EN: Detects whether we are running on Windows.
    //     os.name can be "Windows 10", "Windows 11", etc.
    //     contains("win") works for all versions.
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    // RU: Возвращает разделитель путей для текущей ОС.
    //     Windows — обратная косая черта, Linux — прямая.
    //     Можно также использовать File.separator.
    // EN: Returns the path separator for the current OS.
    //     Windows — backslash, Linux — forward slash.
    //     You can also use File.separator.
    public static String getPathSep() {
        return isWindows() ? "\\" : "/";
    }

    // RU: Возвращает путь к папке с нативными библиотеками.
    //     RaveX хранит .dll/.so в ~/.minecraft/ravex/addons/native/.
    //     user.home — домашняя папка пользователя.
    // EN: Returns the path to the native libraries folder.
    //     RaveX stores .dll/.so in ~/.minecraft/ravex/addons/native/.
    //     user.home — user's home directory.
    public static String getNativeDir() {
        String mcBase = System.getProperty("user.home")
            + getPathSep() + ".minecraft";
        return mcBase + getPathSep() + "ravex" + getPathSep() + "addons" + getPathSep() + "native";
    }

    // ─── Жизненный цикл аддона / Addon lifecycle ────────────────────────────
    //
    // RU: RaveX вызывает эти методы в определённом порядке:
    //     1. Создаёт экземпляр через рефлексию (по AddonInfo.className)
    //     2. Вызывает onLoad(context) — инициализация
    //     3. При выгрузке вызывает onUnload() — очистка
    //
    // EN: RaveX calls these methods in a specific order:
    //     1. Creates instance via reflection (by AddonInfo.className)
    //     2. Calls onLoad(context) — initialization
    //     3. On unload calls onUnload() — cleanup

    @Override
    public void onLoad(AddonContext context) {
        // RU: Сохраняем контекст — он понадобится для логирования
        //     и регистрации модулей.
        // EN: Save context — needed for logging and module registration.
        this.ctx = context;
        ctx.getLogger().info("MainAddon загружается...");

        // RU: Определяем платформу и логируем.
        // EN: Detect platform and log it.
        if (isWindows()) {
            ctx.getLogger().info("Платформа: Windows");
        } else {
            ctx.getLogger().info("Платформа: Linux");
        }

        // RU: Загружаем нативную библиотеку (C++ аддон), если нужна.
        //     Если библиотека не найдена — аддон продолжит работать
        //     без нативного кода (graceful degradation).
        // EN: Load native library (C++ addon) if needed.
        //     If the library is not found — the addon continues
        //     without native code (graceful degradation).
        loadNativeLibrary();

        // RU: Регистрируем модуль. RaveX покажет его в GUI,
        //     и пользователь сможет включать/выключать его.
        // EN: Register the module. RaveX will show it in the GUI,
        //     and the user can enable/disable it.
        ctx.registerModule(new DemoModule());

        ctx.getLogger().info("MainAddon загружен!");
    }

    @Override
    public void onUnload() {
        // RU: При выгрузке освобождаем ресурсы.
        //     Java GC сам позаботится об остальном.
        // EN: On unload release resources.
        //     Java GC handles the rest.
        ctx.getLogger().info("MainAddon выгружается...");
    }

    // ─── Загрузка нативной библиотеки / Native library loading ──────────────
    //
    // RU: Пытается загрузить нативную библиотеку (.dll / .so).
    //     Использует два способа:
    //       1. System.load(fullPath) — загрузка по полному пути
    //       2. System.loadLibrary(name) — поиск в java.library.path
    //
    //     Если оба не сработали — просто предупреждаем, не крашимся.
    //     Это позволяет аддону работать даже без C++ компонента.
    //
    // EN: Tries to load the native library (.dll / .so).
    //     Uses two approaches:
    //       1. System.load(fullPath) — load by full path
    //       2. System.loadLibrary(name) — search in java.library.path
    //
    //     If both fail — just warn, don't crash.
    //     This lets the addon work even without the C++ component.

    private void loadNativeLibrary() {
        // RU: Имя библиотеки без расширения и префикса.
        //     На Windows будет MyAddon.dll, на Linux libMyAddon.so.
        //     System.mapLibraryName добавляет lib и .so/.dll автоматически.
        // EN: Library name without extension or prefix.
        //     On Windows becomes MyAddon.dll, on Linux libMyAddon.so.
        //     System.mapLibraryName adds lib and .so/.dll automatically.
        String libName = isWindows() ? "MyAddon" : "MyAddon";
        String nativeDir = getNativeDir();
        String fullPath = nativeDir + getPathSep()
            + System.mapLibraryName(libName);

        ctx.getLogger().info("Нативная библиотека: " + fullPath);

        try {
            // RU: Загрузка по полному пути — наиболее надёжный способ.
            // EN: Load by full path — the most reliable method.
            System.load(fullPath);
            ctx.getLogger().info("Нативная библиотека загружена!");
        } catch (UnsatisfiedLinkError e) {
            // RU: Не удалось загрузить — не критично, продолжаем без C++.
            // EN: Failed to load — not critical, continue without C++.
            ctx.getLogger().warn("Не удалось загрузить нативную библиотеку: "
                + e.getMessage());
            ctx.getLogger().info("Аддон продолжит работу без нативного кода.");

            // RU: Пробуем альтернативный способ — поиск в java.library.path.
            // EN: Try alternative — search in java.library.path.
            try {
                System.loadLibrary(libName);
                ctx.getLogger().info("Нативная библиотека загружена (java.library.path)!");
            } catch (UnsatisfiedLinkError e2) {
                ctx.getLogger().warn("И java.library.path не помог: " + e2.getMessage());
            }
        }
    }

    // ─── Метаданные / Metadata ─────────────────────────────────────────────
    //
    // RU: Возвращает информацию об аддоне: имя, описание, версию,
    //     автора и полное имя главного класса.
    //     RaveX использует className для создания экземпляра через рефлексию.
    //     Описание зависит от платформы (Windows Edition / Linux Edition).
    //
    // EN: Returns addon info: name, description, version,
    //     author, and fully qualified main class name.
    //     RaveX uses className to create the instance via reflection.
    //     Description depends on platform (Windows Edition / Linux Edition).

    @Override
    public AddonInfo getAddonInfo() {
        return new AddonInfo(
            "MainAddon",
            isWindows() ? "Windows Edition" : "Linux Edition",
<<<<<<< HEAD
            "1.4.3",
=======
            "1.4.6",
>>>>>>> d5789b70550118b35e864d8afa6cd32033b90fc8
            "RaveX Team",
            "ravex.addon.template.MainAddon"
        );
    }
}

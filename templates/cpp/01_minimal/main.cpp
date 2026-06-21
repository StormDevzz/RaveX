// ══════════════════════════════════════════════════════════════════════════════
//  01_minimal / main.cpp
//
//  RU: Минимальный RaveX нативный аддон. Это самый простой пример,
//      который показывает обязательный минимум для любого аддона:
//        - класс-наследник Addon с методами onLoad / onUnload / getName / getVersion
//        - extern "C" точки входа createAddon / destroyAddon
//      Ты можешь использовать этот файл как шаблон для своего первого аддона.
//
//  EN: Minimal RaveX native addon. This is the simplest example
//      showing the required minimum for any addon:
//        - a class inheriting Addon with onLoad / onUnload / getName / getVersion
//        - extern "C" entry points createAddon / destroyAddon
//      You can use this file as a template for your first addon.
//
//  Подробности / Details: см. GUIDE.md → "01 Минимальный аддон"
// ══════════════════════════════════════════════════════════════════════════════

// RU: Подключаем заголовочные файлы RaveX SDK.
//     Addon.h  — базовый класс для всех аддонов (содержит виртуальные методы)
//     AddonContext.h — контекст, через который аддон общается с ядром RaveX
// EN: Include RaveX SDK headers.
//     Addon.h  — base class for all addons (contains virtual methods)
//     AddonContext.h — context through which the addon communicates with RaveX core
#include "../../../src/main/cpp/addon/include/Addon.h"
#include "../../../src/main/cpp/addon/include/AddonContext.h"
#include <iostream>

// RU: Помещаем весь код в пространство имён ravex::addon::minimal_addon,
//     чтобы избежать конфликтов имён с другими аддонами.
// EN: Place all code in the ravex::addon::minimal_addon namespace
//     to avoid name conflicts with other addons.
namespace ravex {
namespace addon {
namespace minimal_addon {

// RU: MinimalAddon — главный (и единственный) класс аддона.
//     Он наследует Addon и переопределяет четыре обязательных метода.
//     RaveX вызывает эти методы в определённые моменты жизненного цикла.
// EN: MinimalAddon — the main (and only) addon class.
//     It inherits Addon and overrides four required methods.
//     RaveX calls these methods at specific lifecycle moments.
class MinimalAddon : public Addon {
public:
    // RU: Вызывается один раз при загрузке аддона.
    //     Здесь ты инициализируешь всё необходимое:
    //     конфиги, потоки, подписки на события.
    //     AddonContext* ctx даёт доступ к логгеру, конфигу и API.
    // EN: Called once when the addon is loaded.
    //     Here you initialize everything needed:
    //     configs, threads, event subscriptions.
    //     AddonContext* ctx provides access to logger, config and API.
    void onLoad(AddonContext* ctx) override {
        // RU: Используем ctx->logInfo для логирования — сообщение попадёт
        //     в основной лог RaveX. std::cout дублирует в консоль.
        // EN: Use ctx->logInfo for logging — the message goes
        //     to the main RaveX log. std::cout duplicates to console.
        ctx->logInfo("MinimalAddon загружен! (Windows/Linux)");
        std::cout << "[MinimalAddon] Работает на "
#ifdef _WIN32
            << "Windows"
#else
            << "Linux"
#endif
            << std::endl;
    }

    // RU: Вызывается при выгрузке аддона.
    //     Освободи здесь все ресурсы: останови потоки, закрой файлы,
    //     удали выделенную память. Аддон должен вернуть состояние "как было".
    // EN: Called when the addon is unloaded.
    //     Release all resources here: stop threads, close files,
    //     free allocated memory. The addon must restore the original state.
    void onUnload() override {
        std::cout << "[MinimalAddon] Выгружен" << std::endl;
    }

    // RU: Возвращает уникальное имя аддона. Используется в GUI RaveX
    //     и для идентификации в логах. Должно быть константным.
    // EN: Returns the unique addon name. Used in RaveX GUI
    //     and for log identification. Must be constant.
    std::string getName() const override { return "MinimalAddon"; }

    // RU: Возвращает версию аддона в формате semver (major.minor.patch).
    //     Используется системой авто-обновления для сравнения версий.
    // EN: Returns the addon version in semver format (major.minor.patch).
    //     Used by the auto-update system for version comparison.
    std::string getVersion() const override { return "1.4.1"; }
};

} // namespace minimal_addon
} // namespace addon
} // namespace ravex

// ─── Обязательные точки входа / Required entry points ────────────────────────
//
// RU: RaveX находит эти две функции через dlsym (Linux) или
//     GetProcAddress (Windows) при загрузке аддона. Без них
//     RaveX не сможет ни создать, ни удалить твой аддон.
//     Они должны быть объявлены как extern "C", чтобы компилятор
//     не искажал (mangle) их имена.
//
// EN: RaveX finds these two functions via dlsym (Linux) or
//     GetProcAddress (Windows) when loading the addon. Without them
//     RaveX cannot create or destroy your addon.
//     They must be declared as extern "C" so the compiler
//     does not mangle their names.

extern "C" {

// RU: Создаёт экземпляр аддона и возвращает указатель на Addon.
//     Макросы __declspec(dllexport) / __attribute__((visibility("default")))
//     делают функцию видимой из динамической библиотеки.
// EN: Creates an addon instance and returns a pointer to Addon.
//     The macros __declspec(dllexport) / __attribute__((visibility("default")))
//     make the function visible from the dynamic library.
#ifdef _WIN32
    __declspec(dllexport) ravex::addon::Addon* createAddon() {
#else
    __attribute__((visibility("default")))
    ravex::addon::Addon* createAddon() {
#endif
        // RU: new — создаём объект в куче. RaveX сам вызовет destroyAddon,
        //     когда аддон больше не нужен.
        // EN: new — allocate on the heap. RaveX will call destroyAddon
        //     when the addon is no longer needed.
        return new ravex::addon::minimal_addon::MinimalAddon();
    }

// RU: Уничтожает экземпляр аддона. Просто delete.
//     RaveX гарантирует, что перед вызовом будет вызван onUnload().
// EN: Destroys the addon instance. Simply delete.
//     RaveX guarantees that onUnload() is called before this.
#ifdef _WIN32
    __declspec(dllexport) void destroyAddon(ravex::addon::Addon* addon) {
#else
    __attribute__((visibility("default")))
    void destroyAddon(ravex::addon::Addon* addon) {
#endif
        delete addon;
    }

}

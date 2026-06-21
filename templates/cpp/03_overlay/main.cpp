// ══════════════════════════════════════════════════════════════════════════════
//  03_overlay / main.cpp
//
//  RU: Оверлейное окно — аппаратно-ускоренное прозрачное окно поверх игры.
//      Оверлей нужен для отображения информации (FPS, радар, чат) прямо
//      на экране, поверх полноэкранных приложений.
//
//      Реализации:
//        - Windows: Win32 Layered Window + DWM (Desktop Window Manager) +
//                   GDI double-buffering (рисуем в памяти, потом DC)
//        - Linux: X11 Simple Window с override_redirect (обходит оконный
//                 менеджер) + XDrawString для текста
//
//      Архитектура:
//        1. Класс Win32Overlay — Win32-специфичная реализация
//        2. Класс X11Overlay — X11-специфичная реализация
//        3. OverlayAddon — обёртка, выбирающая нужную реализацию
//           через #ifdef _WIN32 / #ifndef _WIN32
//
//  EN: Overlay window — hardware-accelerated transparent window on top of games.
//      The overlay is used to display info (FPS, radar, chat) directly
//      on screen, on top of fullscreen applications.
//
//      Implementations:
//        - Windows: Win32 Layered Window + DWM (Desktop Window Manager) +
//                   GDI double-buffering (draw to memory, then DC)
//        - Linux: X11 Simple Window with override_redirect (bypasses the
//                 window manager) + XDrawString for text
//
//  Подробности / Details: см. GUIDE.md → "03 Оверлейное окно"
// ══════════════════════════════════════════════════════════════════════════════

#include <Addon.h>
#include <AddonContext.h>

#include <string>
#include <functional>
#include <iostream>
#include <thread>
#include <chrono>

// RU: Определяем платформенно-независимый сон потока,
//     чтобы не зависеть от platform.hpp из другого примера.
// EN: Define platform-independent thread sleep
//     so we don't depend on platform.hpp from another example.
#ifdef _WIN32
    #include <windows.h>
    #define OVERLAY_SLEEP(ms) Sleep(ms)
#else
    #include <unistd.h>
    #define OVERLAY_SLEEP(ms) usleep((ms) * 1000)
#endif

// ══════════════════════════════════════════════════════════════════════════════
//  Win32 реализация / Win32 implementation
// ══════════════════════════════════════════════════════════════════════════════

// RU: Этот блок компилируется только на Windows.
//     Класс Win32Overlay создаёт прозрачное окно поверх всех окон
//     с использованием Layered Window API (WS_EX_LAYERED).
//     DWM (DwmEnableBlurBehind) делает фон полностью прозрачным.
//     Double-buffering: рисуем в memory DC, затем UpdateLayeredWindow.
// EN: This block compiles only on Windows.
//     Win32Overlay creates a transparent window on top of all windows
//     using the Layered Window API (WS_EX_LAYERED).
//     DWM (DwmEnableBlurBehind) makes the background fully transparent.
//     Double-buffering: draw to memory DC, then UpdateLayeredWindow.
#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <dwmapi.h>
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "dwmapi.lib")

// RU: Структура с метриками для отрисовки оверлея.
//     fps — кадры в секунду (для отображения)
//     cpu, gpu, mem — нагрузка (для мониторинга)
//     customText — произвольный текст (например, название аддона)
// EN: Metrics structure for overlay rendering.
//     fps — frames per second (for display)
//     cpu, gpu, mem — load (for monitoring)
//     customText — arbitrary text (e.g. addon name)
struct OverlayMetrics {
    int width = 0, height = 0;
    int fps = 0;
    float cpu = 0, gpu = 0, mem = 0;
    std::string customText;
};

// RU: Win32Overlay — полноценное прозрачное окно.
//     Особенности:
//       - WS_EX_TOPMOST — всегда поверх всех окон
//       - WS_EX_LAYERED — поддержка прозрачности per-pixel
//       - WS_EX_TRANSPARENT — пропуск кликов мыши (оверлей не мешает игре)
//       - WS_EX_NOACTIVATE — окно не перехватывает фокус ввода
//       - Double-buffering через memory DC и BitBlt
//       - UpdateLayeredWindow для отображения с альфа-каналом
// EN: Win32Overlay — full transparent window.
//     Features:
//       - WS_EX_TOPMOST — always on top of all windows
//       - WS_EX_LAYERED — per-pixel transparency support
//       - WS_EX_TRANSPARENT — pass mouse clicks through (overlay doesn't interfere)
//       - WS_EX_NOACTIVATE — window does not steal input focus
//       - Double-buffering via memory DC and BitBlt
//       - UpdateLayeredWindow for alpha-channel display
class Win32Overlay {
    HWND     m_hwnd   = nullptr;
    HDC      m_hdc    = nullptr;
    HDC      m_memDc  = nullptr;
    HBITMAP  m_bitmap = nullptr;
    bool     m_visible = false;
    OverlayMetrics m_metrics;
    std::function<void(HDC, const OverlayMetrics&)> m_renderCb;

    // RU: Оконная процедура — обрабатывает сообщения Windows.
    //     WM_PAINT — перерисовка окна (BitBlt из memory DC).
    //     WM_ERASEBKGND — подавляем стирание фона (чтобы не мерцало).
    //     WM_DESTROY — завершение.
    // EN: Window procedure — handles Windows messages.
    //     WM_PAINT — redraw (BitBlt from memory DC).
    //     WM_ERASEBKGND — suppress background erase (anti-flicker).
    //     WM_DESTROY — quit.
    static LRESULT CALLBACK wndProc(HWND h, UINT m, WPARAM w, LPARAM l) {
        if (m == WM_DESTROY) { PostQuitMessage(0); return 0; }
        if (m == WM_CREATE) {
            SetWindowLongPtrW(h, GWLP_USERDATA, (LONG_PTR)((CREATESTRUCT*)l)->lpCreateParams);
        }
        auto* self = (Win32Overlay*)GetWindowLongPtrW(h, GWLP_USERDATA);
        if (self && m == WM_PAINT) {
            PAINTSTRUCT ps; BeginPaint(h, &ps);
            if (self->m_memDc) BitBlt(ps.hdc, 0, 0, self->m_metrics.width,
                                      self->m_metrics.height, self->m_memDc, 0, 0, SRCCOPY);
            EndPaint(h, &ps); return 0;
        }
        if (m == WM_ERASEBKGND) return 1;
        return DefWindowProcW(h, m, w, l);
    }

public:
    Win32Overlay() = default;
    ~Win32Overlay() { destroy(); }

    // RU: Создаёт окно с расширенными стилями для прозрачного оверлея.
    //     Параметры: заголовок, позиция (x, y), размер (w, h).
    //     Регистрирует класс окна, создаёт окно, настраивает Layered Window,
    //     включает прозрачность через DWM, создаёт memory DC для двойной буферизации.
    // EN: Creates a window with extended styles for transparent overlay.
    //     Parameters: title, position (x, y), size (w, h).
    //     Registers window class, creates window, configures Layered Window,
    //     enables transparency via DWM, creates memory DC for double buffering.
    bool create(const std::string& title, int x, int y, int w, int h) {
        HINSTANCE inst = GetModuleHandleW(nullptr);
        WNDCLASSW wc = {};
        wc.lpfnWndProc = wndProc;
        wc.hInstance = inst;
        wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
        wc.lpszClassName = L"RavexOverlay";
        RegisterClassW(&wc);

        // RU: WS_EX_TOPMOST — поверх всех окон.
        //     WS_EX_LAYERED — per-pixel alpha (прозрачность).
        //     WS_EX_TRANSPARENT — пропускать клики мыши сквозь окно.
        //     WS_EX_NOACTIVATE — не перехватывать фокус.
        // EN: WS_EX_TOPMOST — on top of all windows.
        //     WS_EX_LAYERED — per-pixel alpha (transparency).
        //     WS_EX_TRANSPARENT — let mouse clicks pass through.
        //     WS_EX_NOACTIVATE — do not steal focus.
        m_hwnd = CreateWindowExW(
            WS_EX_TOPMOST | WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE,
            L"RavexOverlay", L"", WS_POPUP, x, y, w, h,
            nullptr, nullptr, inst, this);
        if (!m_hwnd) return false;

        // RU: Устанавливаем полную прозрачность (альфа = 0).
        // EN: Set full transparency (alpha = 0).
        SetLayeredWindowAttributes(m_hwnd, 0, 0, LWA_ALPHA);
        SetWindowPos(m_hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);

        // RU: Включаем прозрачность фона через DWM.
        // EN: Enable background transparency via DWM.
        DWM_BLURBEHIND bb = { DWM_BB_ENABLE };
        DwmEnableBlurBehindWindow(m_hwnd, &bb);

        // RU: Создаём memory DC для двойной буферизации.
        //     Рисуем в m_memDc, затем копируем на экран через UpdateLayeredWindow.
        // EN: Create memory DC for double buffering.
        //     Draw to m_memDc, then copy to screen via UpdateLayeredWindow.
        m_hdc = GetDC(m_hwnd);
        m_memDc = CreateCompatibleDC(m_hdc);
        m_bitmap = CreateCompatibleBitmap(m_hdc, w, h);
        SelectObject(m_memDc, m_bitmap);

        m_visible = true;
        ShowWindow(m_hwnd, SW_SHOW);
        UpdateWindow(m_hwnd);
        return true;
    }

    // RU: Уничтожает окно и освобождает все ресурсы GDI.
    //     Порядок важен: сначала bitmap, потом DC, потом hwnd.
    // EN: Destroys the window and frees all GDI resources.
    //     Order matters: bitmap first, then DC, then hwnd.
    void destroy() {
        if (m_bitmap) { DeleteObject(m_bitmap); m_bitmap = nullptr; }
        if (m_memDc)  { DeleteDC(m_memDc); m_memDc = nullptr; }
        if (m_hdc)    { ReleaseDC(m_hwnd, m_hdc); m_hdc = nullptr; }
        if (m_hwnd)   { DestroyWindow(m_hwnd); m_hwnd = nullptr; }
        m_visible = false;
    }

    bool isVisible() const { return m_visible; }

    // RU: Начинает новый кадр: очищает фон, получает размеры окна.
    //     FillRect с чёрным фоном — потом этот фон станет прозрачным
    //     через UpdateLayeredWindow с альфа-каналом.
    // EN: Begins a new frame: clears background, gets window dimensions.
    //     FillRect with black — this background becomes transparent
    //     via UpdateLayeredWindow with alpha channel.
    void beginFrame() {
        RECT rc; GetClientRect(m_hwnd, &rc);
        m_metrics.width = rc.right; m_metrics.height = rc.bottom;
        FillRect(m_memDc, &rc, (HBRUSH)GetStockObject(BLACK_BRUSH));
        SetBkMode(m_memDc, TRANSPARENT);
    }

    // RU: Завершает кадр: вызывает колбэк рендеринга и отображает
    //     memory DC на экран через UpdateLayeredWindow с альфа-каналом.
    //     BLENDFUNCTION настраивает режим смешивания (AC_SRC_ALPHA для per-pixel alpha).
    // EN: Ends the frame: calls render callback and displays memory DC
    //     on screen via UpdateLayeredWindow with alpha channel.
    //     BLENDFUNCTION configures blend mode (AC_SRC_ALPHA for per-pixel alpha).
    void endFrame() {
        if (m_renderCb) m_renderCb(m_memDc, m_metrics);
        BLENDFUNCTION blend = { AC_SRC_OVER, 0, 255, AC_SRC_ALPHA };
        POINT pt = {}; SIZE sz = { m_metrics.width, m_metrics.height };
        UpdateLayeredWindow(m_hwnd, m_hdc, &pt, &sz, m_memDc, &pt, 0, &blend, ULW_ALPHA);
    }

    // RU: Устанавливает колбэк для отрисовки содержимого оверлея.
    //     Колбэк получает HDC (контекст устройства) и метрики.
    //     Используй GDI-функции (TextOut, Rectangle и т.д.) для рисования.
    // EN: Sets the render callback for drawing overlay content.
    //     The callback receives HDC (device context) and metrics.
    //     Use GDI functions (TextOut, Rectangle, etc.) for drawing.
    void setRenderCallback(std::function<void(HDC, const OverlayMetrics&)> cb) { m_renderCb = std::move(cb); }
    void updateMetrics(const OverlayMetrics& m) { m_metrics = m; }
};

#endif // _WIN32

// ══════════════════════════════════════════════════════════════════════════════
//  Linux X11 реализация / Linux X11 implementation
// ══════════════════════════════════════════════════════════════════════════════

// RU: Этот блок компилируется только на Linux (не-Windows).
//     Использует Xlib для создания простого окна с override_redirect,
//     которое игнорирует оконный менеджер и отображается поверх всего.
//     Текст рисуется через XDrawString.
// EN: This block compiles only on Linux (not Windows).
//     Uses Xlib to create a simple window with override_redirect,
//     which bypasses the window manager and displays on top of everything.
//     Text is drawn via XDrawString.
#ifndef _WIN32

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <cstdio>

// RU: X11Overlay — простое оверлейное окно для Linux.
//     В отличие от Win32, здесь нет per-pixel alpha (только
//     сплошной фон с текстом). Для полноценного оверлея с
//     прозрачностью используй Compositing Manager (Compton/ Picom)
//     и XRender или OpenGL.
// EN: X11Overlay — simple overlay window for Linux.
//     Unlike Win32, there is no per-pixel alpha (only
//     solid background with text). For a full overlay with
//     transparency use a Compositing Manager (Compton/Picom)
//     and XRender or OpenGL.
class X11Overlay {
    Display* m_display = nullptr;
    Window   m_window  = 0;
    int      m_width = 0, m_height = 0;
    GC       m_gc = nullptr;
    bool     m_visible = false;
    std::string m_text;

public:
    X11Overlay() = default;
    ~X11Overlay() { destroy(); }

    // RU: Создаёт X11 окно с override_redirect.
    //     override_redirect = True — окно не управляется оконным менеджером.
    //     _NET_WM_STATE_ABOVE — подсказка быть поверх других окон.
    //     XSelectInput — подписываемся на ExposureMask (перерисовка) и
    //     StructureNotifyMask (изменение размеров).
    // EN: Creates X11 window with override_redirect.
    //     override_redirect = True — window is not managed by the WM.
    //     _NET_WM_STATE_ABOVE — hint to be above other windows.
    //     XSelectInput — subscribe to ExposureMask (redraw) and
    //     StructureNotifyMask (size changes).
    bool create(int x, int y, int w, int h) {
        m_display = XOpenDisplay(nullptr);
        if (!m_display) return false;

        int screen = DefaultScreen(m_display);
        Window root = RootWindow(m_display, screen);

        // RU: Создаём простое окно с чёрным фоном.
        // EN: Create simple window with black background.
        m_window = XCreateSimpleWindow(m_display, root, x, y, w, h, 0,
                                       BlackPixel(m_display, screen),
                                       BlackPixel(m_display, screen));
        if (!m_window) { XCloseDisplay(m_display); m_display = nullptr; return false; }

        // RU: override_redirect — обходим оконный менеджер.
        // EN: override_redirect — bypass the window manager.
        XSetWindowAttributes attrs = {};
        attrs.override_redirect = True;
        XChangeWindowAttributes(m_display, m_window, CWOverrideRedirect, &attrs);

        // RU: Устанавливаем свойство _NET_WM_STATE_ABOVE для "поверх всех".
        // EN: Set _NET_WM_STATE_ABOVE property for "always on top".
        Atom wmStateAbove = XInternAtom(m_display, "_NET_WM_STATE_ABOVE", False);
        if (wmStateAbove) {
            XChangeProperty(m_display, m_window,
                XInternAtom(m_display, "_NET_WM_STATE", False),
                XA_ATOM, 32, PropModeReplace,
                (unsigned char*)&wmStateAbove, 1);
        }

        XSelectInput(m_display, m_window, ExposureMask | StructureNotifyMask);
        XMapWindow(m_display, m_window);
        XFlush(m_display);

        m_gc = XCreateGC(m_display, m_window, 0, nullptr);
        m_width = w; m_height = h;
        m_visible = true;
        return true;
    }

    void destroy() {
        if (m_visible && m_display && m_window) {
            XDestroyWindow(m_display, m_window);
            XCloseDisplay(m_display);
        }
        m_visible = false;
        m_display = nullptr; m_window = 0;
    }

    bool isVisible() const { return m_visible; }

    // RU: Рисует текст на окне. Очистка не требуется — X11 сам
    //     управляет перерисовкой. Для более сложной графики
    //     используй XRender, Cairo или OpenGL.
    // EN: Draws text on the window. No clearing needed — X11
    //     handles redraw itself. For more complex graphics
    //     use XRender, Cairo, or OpenGL.
    void render() {
        if (!m_visible || !m_display) return;
        XSetForeground(m_display, m_gc, WhitePixel(m_display, DefaultScreen(m_display)));
        XDrawString(m_display, m_window, m_gc, 10, 20, m_text.c_str(), m_text.size());
        XFlush(m_display);
    }

    // RU: Устанавливает текст для отображения.
    // EN: Sets text to display.
    void setText(const std::string& t) { m_text = t; }
};

#endif // !_WIN32

// ══════════════════════════════════════════════════════════════════════════════
//  Аддон-обёртка / Addon wrapper
// ══════════════════════════════════════════════════════════════════════════════

// RU: OverlayAddon — обёртка, которая выбирает нужную реализацию
//     (Win32Overlay или X11Overlay) на этапе компиляции через #ifdef.
//     Запускает поток с циклом отрисовки оверлея.
// EN: OverlayAddon — wrapper that selects the right implementation
//     (Win32Overlay or X11Overlay) at compile time via #ifdef.
//     Starts a thread with the overlay draw loop.
namespace ravex { namespace addon { namespace overlay_example {

class OverlayAddon : public Addon {
    std::thread* overlayThread = nullptr;
    bool running = false;

#ifdef _WIN32
    Win32Overlay overlay;
#else
    X11Overlay overlay;
#endif

    // RU: Цикл оверлея — работает в отдельном потоке.
    //     На Windows: beginFrame, обновление метрик, endFrame (~30 FPS).
    //     На Linux: установка текста, render.
    //     Скорость обновления 33 мс (примерно 30 FPS) через ADDON_SLEEP.
    // EN: Overlay loop — runs in a separate thread.
    //     On Windows: beginFrame, update metrics, endFrame (~30 FPS).
    //     On Linux: set text, render.
    //     Update rate 33 ms (roughly 30 FPS) via ADDON_SLEEP.
    void overlayLoop() {
#ifdef _WIN32
        if (!overlay.create("RaveX Overlay", 0, 0, 400, 200)) return;
        overlay.setRenderCallback([](HDC hdc, const OverlayMetrics& m) {
            SetTextColor(hdc, RGB(0, 255, 0));
            std::string text = "FPS: " + std::to_string(m.fps)
                             + " | CPU: " + std::to_string((int)m.cpu) + "%"
                             + " | RAM: " + std::to_string((int)m.mem) + "MB";
            TextOutA(hdc, 10, 10, text.c_str(), (int)text.size());
            if (!m.customText.empty())
                TextOutA(hdc, 10, 30, m.customText.c_str(), (int)m.customText.size());
        });
        while (running) {
            overlay.beginFrame();
            OverlayMetrics m;
            m.fps = 60; m.cpu = 23.5f; m.mem = 2048;
            m.customText = "FeatureAddon overlay (Win32)";
            overlay.updateMetrics(m);
            overlay.endFrame();
            OVERLAY_SLEEP(33);
        }
        overlay.destroy();
#else
        if (!overlay.create(0, 0, 400, 200)) return;
        while (running) {
            overlay.setText("RaveX Overlay (X11) | FPS: 60 | CPU: 23%");
            overlay.render();
            OVERLAY_SLEEP(33);
        }
        overlay.destroy();
#endif
    }

public:
    void onLoad(AddonContext* ctx) override {
        ctx->logInfo("OverlayAddon: запуск оверлея...");
        running = true;
        overlayThread = new std::thread(&OverlayAddon::overlayLoop, this);
    }

    void onUnload() override {
        running = false;
        if (overlayThread) {
            overlayThread->join();
            delete overlayThread;
            overlayThread = nullptr;
        }
    }

    std::string getName()    const override { return "OverlayAddon"; }
    std::string getVersion() const override { return "1.4.1"; }
};

}}} // namespace

// ─── Точки входа / Entry points ──────────────────────────────────────────────

extern "C" {
#ifdef _WIN32
    __declspec(dllexport) ravex::addon::Addon* createAddon() {
#else
    __attribute__((visibility("default")))
    ravex::addon::Addon* createAddon() {
#endif
        return new ravex::addon::overlay_example::OverlayAddon();
    }

#ifdef _WIN32
    __declspec(dllexport) void destroyAddon(ravex::addon::Addon* a) {
#else
    __attribute__((visibility("default")))
    void destroyAddon(ravex::addon::Addon* a) {
#endif
        delete a;
    }
}

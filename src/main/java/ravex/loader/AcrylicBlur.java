package ravex.loader;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

public class AcrylicBlur {

    private interface User32Ext extends StdCallLibrary {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);
        boolean SetWindowCompositionAttribute(WinDef.HWND hwnd, Pointer data);
        int GetWindowLongA(WinDef.HWND hwnd, int nIndex);
        int SetWindowLongA(WinDef.HWND hwnd, int nIndex, int dwNewLong);
        boolean SetWindowPos(WinDef.HWND hwnd, WinDef.HWND hWndInsertAfter, int x, int y, int cx, int cy, int uFlags);
        int GetWindowThreadProcessId(WinDef.HWND hwnd, int[] lpdwProcessId);
        int GetClassNameA(WinDef.HWND hwnd, byte[] lpClassName, int nMaxCount);
    }

    public static class AccentPolicy extends Structure {
        public int AccentState;
        public int AccentFlags;
        public int GradientColor;
        public int AnimationId;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("AccentState", "AccentFlags", "GradientColor", "AnimationId");
        }
    }

    public static class WindowCompositionAttributeData extends Structure {
        public int Attribute;
        public Pointer Data;
        public int SizeOfData;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("Attribute", "Data", "SizeOfData");
        }
    }

    public static final int WCA_ACCENT_POLICY = 19;
    public static final int ACCENT_ENABLE_ACRYLICBLURBEHIND = 4;

    private interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);
        int DwmEnableBlurBehindWindow(WinDef.HWND hwnd, DWM_BLURBEHIND bb);
        int DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    public static final int DWMWA_SYSTEMBACKDROP_TYPE = 38;
    public static final int DWMSBT_DISABLE = 1;
    public static final int DWMSBT_MAINWINDOW = 2;
    public static final int DWMSBT_TRANSIENTWINDOW = 3;
    public static final int DWMSBT_TABBEDWINDOW = 4;

    public static class DWM_BLURBEHIND extends Structure {
        public int dwFlags;
        public boolean fEnable;
        public Pointer hRgnBlur;
        public boolean fTransitionOnMaximized;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwFlags", "fEnable", "hRgnBlur", "fTransitionOnMaximized");
        }
    }

    public static final int DWM_BB_ENABLE = 0x00000001;

    public static final int GWL_EXSTYLE = -20;
    public static final int WS_EX_LAYERED = 0x00080000;
    public static final int SWP_NOMOVE = 0x0002;
    public static final int SWP_NOSIZE = 0x0001;
    public static final int SWP_NOZORDER = 0x0004;
    public static final int SWP_FRAMECHANGED = 0x0020;
    public static final WinDef.HWND HWND_TOP = new WinDef.HWND(Pointer.NULL);

    private static boolean supported = checkSupported();

    private static boolean checkSupported() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("windows")) return false;
        WinNT.OSVERSIONINFOEX vex = new WinNT.OSVERSIONINFOEX();
        if (Kernel32.INSTANCE.GetVersionEx(vex)) {
            try {
                return Integer.parseInt(vex.dwBuildNumber.toString()) >= 17134;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    public static void addTransparencyToBackground(Component component) {
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                addTransparencyToBackground(child);
            }
        }
    }

    private static Pointer getWindowHandle(Window window) {
        // Method 1: JNA's Native.getWindowPointer
        Pointer hWndPtr = Native.getWindowPointer(window);
        if (hWndPtr != null && !hWndPtr.equals(Pointer.NULL)) {
            System.err.println("[AcrylicBlur] hwnd via Native.getWindowPointer = " + hWndPtr);
            return hWndPtr;
        }
        // Method 2: reflection getPeer().getHWnd()
        try {
            java.lang.reflect.Method getPeer = java.awt.Component.class.getDeclaredMethod("getPeer");
            getPeer.setAccessible(true);
            Object peer = getPeer.invoke(window);
            if (peer != null) {
                java.lang.reflect.Method getHWnd = peer.getClass().getDeclaredMethod("getHWnd");
                getHWnd.setAccessible(true);
                Object r = getHWnd.invoke(peer);
                if (r instanceof Number) {
                    long val = ((Number) r).longValue();
                    if (val != 0) {
                        System.err.println("[AcrylicBlur] hwnd via reflection getHWnd = " + val);
                        return new Pointer(val);
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[AcrylicBlur] reflection getHWnd failed: " + t);
        }
        // Method 3: enumerate windows by current process id, pick a SunAwtWindow class
        try {
            int pid = Kernel32.INSTANCE.GetCurrentProcessId();
            final Pointer[] found = new Pointer[1];
            User32.WNDENUMPROC cb = new User32.WNDENUMPROC() {
                @Override
                public boolean callback(WinDef.HWND hwnd, Pointer data) {
                    int[] pids = new int[1];
                    User32Ext.INSTANCE.GetWindowThreadProcessId(hwnd, pids);
                    if (pids[0] == pid) {
                        byte[] buf = new byte[256];
                        int len = User32Ext.INSTANCE.GetClassNameA(hwnd, buf, buf.length);
                        if (len > 0) {
                            String cls = new String(buf, 0, len);
                            if (cls.toLowerCase().contains("sunawt") || cls.toLowerCase().contains("awt")) {
                                found[0] = hwnd.getPointer();
                                return false; // stop enumeration
                            }
                        }
                    }
                    return true;
                }
            };
            User32.INSTANCE.EnumWindows(cb, Pointer.NULL);
            if (found[0] != null) {
                System.err.println("[AcrylicBlur] hwnd via EnumWindows = " + found[0]);
                return found[0];
            }
        } catch (Throwable t) {
            System.err.println("[AcrylicBlur] EnumWindows failed: " + t);
        }
        System.err.println("[AcrylicBlur] could not obtain HWND");
        return null;
    }

    public static void apply(Window window, int opacity, int background) {
        if (!supported || window == null) return;

        Pointer hWndPtr = getWindowHandle(window);
        if (hWndPtr == null || hWndPtr.equals(Pointer.NULL)) return;
        WinDef.HWND hwnd = new WinDef.HWND(hWndPtr);

        // Opaque window: do NOT set WS_EX_LAYERED (that would make it layered/per-pixel-alpha
        // and bypass DWM compositing, hiding the acrylic backdrop).

        boolean ok = false;
        // SetWindowCompositionAttribute: acrylic (Win10+).
        AccentPolicy policy = new AccentPolicy();
        policy.AccentState = ACCENT_ENABLE_ACRYLICBLURBEHIND;
        policy.AccentFlags = 0;
        policy.GradientColor = (opacity << 24) | (background & 0xFFFFFF);
        policy.AnimationId = 0;
        policy.write();

        WindowCompositionAttributeData data = new WindowCompositionAttributeData();
        data.Attribute = WCA_ACCENT_POLICY;
        data.Data = policy.getPointer();
        data.SizeOfData = policy.size();
        data.write();

        try {
            ok = User32Ext.INSTANCE.SetWindowCompositionAttribute(hwnd, data.getPointer());
            System.err.println("[AcrylicBlur] SetWindowCompositionAttribute = " + ok);
        } catch (Throwable t) {
            System.err.println("[AcrylicBlur] SetWindowCompositionAttribute threw: " + t);
        }

        try {
            User32Ext.INSTANCE.SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
        } catch (Throwable ignored) {}
    }

    public static void applyGlass(Window window) {
        if (!supported || window == null) return;
        addTransparencyToBackground(window);
        apply(window, 0x40, 0x000000);
    }
}

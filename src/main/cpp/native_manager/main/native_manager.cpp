// RaveX native manager: a tiny windowless process that lives while the game
// runs, so the task manager shows a RaveX entry and it is obvious the native
// side is loaded. It does no work, it only waits for the game process to exit.
#include <windows.h>
#include <wchar.h>

static DWORD parsePid(const wchar_t* text) {
    if (!text) return 0;
    while (*text == L' ' || *text == L'=') text++;
    DWORD pid = 0;
    while (*text >= L'0' && *text <= L'9') {
        pid = pid * 10 + (DWORD)(*text - L'0');
        text++;
    }
    return pid;
}

static DWORD parentPid() {
    const wchar_t* cmd = GetCommandLineW();
    if (!cmd) return 0;
    const wchar_t* flag = wcsstr(cmd, L"--parent");
    return flag ? parsePid(flag + 8) : 0;
}

// ansi entry point, the command line is read as wide chars inside
int WINAPI WinMain(HINSTANCE, HINSTANCE, LPSTR, int) {
    DWORD pid = parentPid();
    // spawned without a game to watch, nothing to stay for
    if (pid == 0) return 0;

    HANDLE game = OpenProcess(SYNCHRONIZE, FALSE, pid);
    if (!game) return 0;

    WaitForSingleObject(game, INFINITE);
    CloseHandle(game);
    return 0;
}

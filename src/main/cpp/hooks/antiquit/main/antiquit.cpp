#include "antiquit.hpp"
#include <atomic>
#include <cstdlib>
#include <cstring>

static std::atomic<bool> g_quitBlocked{false};

#ifdef _WIN32
#include <windows.h>
static BOOL WINAPI consoleHandler(DWORD dwCtrlType) {
    if (g_quitBlocked) {
        int result = MessageBoxW(NULL, L"Are you sure bro?", L"RaveX",
            MB_YESNO | MB_ICONQUESTION | MB_SYSTEMMODAL);
        if (result == IDYES) {
            g_quitBlocked = false;
            exit(0);
        }
        return TRUE;
    }
    return FALSE;
}
#else
#include <signal.h>
#include <unistd.h>
#include <sys/wait.h>
#endif

#ifdef _WIN32
static bool g_handlerInstalled = false;
#endif

static bool showConfirmDialog() {
#ifdef _WIN32
    int result = MessageBoxW(NULL, L"Are you sure bro?", L"RaveX",
        MB_YESNO | MB_ICONQUESTION | MB_SYSTEMMODAL);
    return result == IDYES;
#else
    pid_t pid = fork();
    if (pid == 0) {
        execlp("zenity", "zenity", "--question",
            "--title=RaveX",
            "--text=Are you sure bro?",
            "--ok-label=Yep",
            "--cancel-label=Nah",
            (char*)NULL);
        execlp("xmessage", "xmessage", "-buttons", "Yep:0,Nah:1",
            "Are you sure bro?", (char*)NULL);
        _exit(1);
    } else if (pid > 0) {
        int status;
        waitpid(pid, &status, 0);
        return WIFEXITED(status) && WEXITSTATUS(status) == 0;
    }
    return false;
#endif
}

static void installHandler() {
#ifdef _WIN32
    if (!g_handlerInstalled) {
        SetConsoleCtrlHandler(consoleHandler, TRUE);
        g_handlerInstalled = true;
    }
#else
    struct sigaction sa;
    sa.sa_handler = [](int sig) {
        if (g_quitBlocked) {
            if (showConfirmDialog()) {
                g_quitBlocked = false;
                _exit(128 + sig);
            }
        } else {
            _exit(128 + sig);
        }
    };
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = 0;
    sigaction(SIGINT, &sa, nullptr);
    sigaction(SIGTERM, &sa, nullptr);
    sigaction(SIGHUP, &sa, nullptr);
    sigaction(SIGQUIT, &sa, nullptr);
#endif
}

void blockQuit(bool block) {
    g_quitBlocked = block;
    if (block) {
        installHandler();
    }
}

bool isQuitBlocked() {
    return g_quitBlocked.load();
}

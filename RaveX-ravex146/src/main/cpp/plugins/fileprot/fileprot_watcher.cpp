#include "include/fileprot_watcher.hpp"
#include <algorithm>

#ifdef _WIN32
#include <windows.h>
#else
#include <sys/inotify.h>
#include <unistd.h>
#include <poll.h>
#endif

namespace ravex {
namespace fileprot {

FileWatcher::FileWatcher()
#ifdef _WIN32
    : dirHandle(nullptr)
#else
    : fd(-1), wd(-1)
#endif
    , running(false) {}

FileWatcher::~FileWatcher() {
    unwatch();
}

bool FileWatcher::watch(const std::string& path, bool rec) {
    if (isWatching()) unwatch();

    watchPath = path;
    recursive = rec;
    running = true;

#ifdef _WIN32
    dirHandle = CreateFile(
        path.c_str(),
        FILE_LIST_DIRECTORY,
        FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
        nullptr,
        OPEN_EXISTING,
        FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OVERLAPPED,
        nullptr
    );
    if (dirHandle == INVALID_HANDLE_VALUE) {
        dirHandle = nullptr;
        running = false;
        return false;
    }
#else
    fd = inotify_init1(IN_NONBLOCK);
    if (fd < 0) {
        running = false;
        return false;
    }

    uint32_t mask = IN_MODIFY | IN_CREATE | IN_DELETE | IN_MOVED_FROM | IN_MOVED_TO;
    wd = inotify_add_watch(fd, path.c_str(), mask);
    if (wd < 0) {
        close(fd);
        fd = -1;
        running = false;
        return false;
    }
#endif

    worker = std::thread(&FileWatcher::workerThread, this);
    return true;
}

void FileWatcher::unwatch() {
    running = false;
    if (worker.joinable()) worker.join();

#ifdef _WIN32
    if (dirHandle) {
        CloseHandle(dirHandle);
        dirHandle = nullptr;
    }
#else
    if (wd >= 0) {
        inotify_rm_watch(fd, wd);
        wd = -1;
    }
    if (fd >= 0) {
        close(fd);
        fd = -1;
    }
#endif
}

bool FileWatcher::isWatching() const {
    return running.load();
}

void FileWatcher::setCallback(std::function<void(const FileEvent&)> cb) {
    callback = cb;
}

std::vector<FileEvent> FileWatcher::pollEvents() {
    std::vector<FileEvent> events;

#ifdef _WIN32
    if (!dirHandle) return events;

    char buffer[4096];
    DWORD bytesReturned;
    if (ReadDirectoryChangesW(dirHandle, buffer, sizeof(buffer), recursive,
                               FILE_NOTIFY_CHANGE_FILE_NAME |
                               FILE_NOTIFY_CHANGE_DIR_NAME |
                               FILE_NOTIFY_CHANGE_LAST_WRITE,
                               &bytesReturned, nullptr, nullptr)) {
        FILE_NOTIFY_INFORMATION* info = (FILE_NOTIFY_INFORMATION*)buffer;
        while (info) {
            FileEvent event;
            event.path = std::string((const char*)info->FileName,
                                     info->FileNameLength / sizeof(wchar_t));
            switch (info->Action) {
                case FILE_ACTION_ADDED:
                case FILE_ACTION_MODIFIED:
                    event.type = FileEvent::Modified;
                    break;
                case FILE_ACTION_REMOVED:
                    event.type = FileEvent::Deleted;
                    break;
                case FILE_ACTION_RENAMED_OLD_NAME:
                    event.type = FileEvent::Renamed;
                    break;
            }
            events.push_back(event);
            if (info->NextEntryOffset == 0) break;
            info = (FILE_NOTIFY_INFORMATION*)((char*)info + info->NextEntryOffset);
        }
    }
#else
    if (fd < 0) return events;

    struct pollfd pfd = {fd, POLLIN, 0};
    while (poll(&pfd, 1, 0) > 0) {
        char buffer[4096];
        int len = read(fd, buffer, sizeof(buffer));
        if (len <= 0) break;

        int offset = 0;
        while (offset < len) {
            struct inotify_event* ie = (struct inotify_event*)(buffer + offset);
            if (ie->wd == wd) {
                FileEvent event;
                event.path = watchPath + "/" + ie->name;
                if (ie->mask & IN_MODIFY) event.type = FileEvent::Modified;
                else if (ie->mask & IN_CREATE) event.type = FileEvent::Created;
                else if (ie->mask & IN_DELETE) event.type = FileEvent::Deleted;
                else if (ie->mask & IN_MOVED_FROM) event.type = FileEvent::Renamed;
                else if (ie->mask & IN_MOVED_TO) {
                    event.type = FileEvent::Created;
                    event.newPath = watchPath + "/" + ie->name;
                }
                events.push_back(event);
            }
            offset += sizeof(struct inotify_event) + ie->len;
        }
    }
#endif

    return events;
}

void FileWatcher::workerThread() {
    while (running.load()) {
        auto events = pollEvents();
        if (!events.empty() && callback) {
            for (const auto& e : events) {
                callback(e);
            }
        }
#ifdef _WIN32
        Sleep(100);
#else
        usleep(100000);
#endif
    }
}

}
}

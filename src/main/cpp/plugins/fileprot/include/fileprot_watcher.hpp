#pragma once
#include <string>
#include <vector>
#include <functional>
#include <thread>
#include <atomic>

namespace ravex {
namespace fileprot {

struct FileEvent {
    enum Type { Created, Modified, Deleted, Renamed };
    Type type;
    std::string path;
    std::string newPath;
};

class FileWatcher {
public:
    FileWatcher();
    ~FileWatcher();
    bool watch(const std::string& path, bool recursive);
    void unwatch();
    bool isWatching() const;
    void setCallback(std::function<void(const FileEvent&)> cb);
    std::vector<FileEvent> pollEvents();

private:
    void workerThread();
    std::thread worker;
    std::atomic<bool> running;
    std::string watchPath;
    bool recursive;
    std::function<void(const FileEvent&)> callback;
#ifdef _WIN32
    void* dirHandle;
#else
    int fd;
    int wd;
#endif
};

}
}

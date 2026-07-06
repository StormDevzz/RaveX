#include "include/benchmark.hpp"
#include "include/bench_common.hpp"
#include <thread>
#include <mutex>
#include <vector>
#include <atomic>

namespace ravex {
namespace benchmark {

class ThreadPool {
public:
    explicit ThreadPool(int numThreads) : threads(numThreads) {}

    template<typename F>
    void run(F&& task) {
        for (auto& t : threads) {
            t = std::thread(task);
        }
        for (auto& t : threads) {
            if (t.joinable()) t.join();
        }
    }

    int size() const { return static_cast<int>(threads.size()); }

private:
    std::vector<std::thread> threads;
};

class ParallelBench {
public:
    static double runParallel(int numThreads, std::function<double()> singleTask) {
        std::atomic<double> totalTime(0.0);
        std::atomic<int> done(0);
        std::mutex mtx;

        auto worker = [&]() {
            double t = singleTask();
            {
                std::lock_guard<std::mutex> lock(mtx);
                totalTime += t;
            }
            done++;
        };

        std::vector<std::thread> workers;
        for (int i = 0; i < numThreads; i++) {
            workers.emplace_back(worker);
        }
        for (auto& w : workers) {
            if (w.joinable()) w.join();
        }

        return totalTime.load() / numThreads;
    }
};

}
}

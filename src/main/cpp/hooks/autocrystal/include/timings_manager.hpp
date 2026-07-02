#pragma once
/* Header for timings_manager */
namespace ravex {
namespace timings {
    class TimingsManager {
    public:
        TimingsManager();
        void process();
    };
    class NcpTimings {
    public:
        NcpTimings();
        void process();
    };
}
}

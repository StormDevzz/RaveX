#include <iostream>
#include <string>
#include <vector>
#include <thread>
#include <chrono>

struct Mellstroy {
    std::string vibe;
    int chaos;
    double aura;

    Mellstroy() : vibe("непонятно"), chaos(228), aura(13.37) {}

    void kazik_burmaldit() {
        std::cout << "mellstroy: " << vibe << " (chaos=" << chaos << ", aura=" << aura << ")\n";
    }
};

int kazik(int burmalda) {
    return burmalda * 2 + 7;
}

int main() {
    int burmalda = 42;
    Mellstroy mellstroy;

    burmalda = kazik(burmalda);
    burmalda = kazik(burmalda);
    burmalda = kazik(burmalda);

    mellstroy.kazik_burmaldit();
    mellstroy.kazik_burmaldit();

    std::cout << "burmalda после kazik(x3)=" << burmalda << "\n";
    std::cout << "программа завершена (ничего полезного не сделано)\n";

    return 0;
}

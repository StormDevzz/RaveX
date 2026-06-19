# Installation

## Requirements

- **Linux** (эта версия только для Linux, см. LINUX.md)
- CMake >= 3.10
- GCC / Clang с поддержкой C++17
- GTK3 (dev)
- curl

### Установка зависимостей

**Debian / Ubuntu:**
```bash
sudo apt install cmake g++ libgtk-3-dev curl
```

**Arch Linux:**
```bash
sudo pacman -S cmake gcc gtk3 curl
```

**Fedora:**
```bash
sudo dnf install cmake gcc-c++ gtk3-devel curl
```

## Сборка

```bash
cd src/main/cpp/launcher/linux
./build.sh
```

Бинарник: `build/kickx_launcher`

## Запуск

```bash
./build/kickx_launcher
```

Лаунчер создаёт:
- `~/.kickxxx/` — кеш, библиотеки, ассеты, версии Minecraft
- `~/.ravex/` — файлы клиента, нативы

## Структура папок

```
~/.kickxxx/
├── versions/         — версии Minecraft
├── libraries/        — библиотеки
├── assets/           — ассеты
├── instances/        — игровые инстанции
└── background.jpg    — фоновое изображение
```

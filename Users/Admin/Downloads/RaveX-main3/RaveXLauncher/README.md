# RaveX Launcher

Отдельный лаунчер для чита RaveX с поддержкой Windows и Linux.

## Версии

### Java версия (Swing)
- Классический GUI интерфейс
- Простая установка
- Требует только Java

### C++ версия (ImGui + SDL2)
- Современный минималистичный интерфейс в стиле Discord
- Плавные анимации и красивый дизайн
- Требует установки дополнительных зависимостей

## Особенности C++ версии

- Минималистичный современный интерфейс в стиле Discord
- Плавные анимации
- Темная цветовая схема
- Выбор JAR файла чита
- Отображение логов запуска в реальном времени
- Поддержка Windows и Linux
- Автоматический поиск JAR файла

## Установка зависимостей C++ версии

### Windows
Требуется установить:
- CMake
- MinGW или Visual Studio
- SDL2
- OpenGL
- curl (для скриптов установки)

Запустите:
```bash
setup_deps.bat
```

### Linux
Требуется установить:
```bash
sudo apt-get install cmake build-essential libsdl2-dev libgl1-mesa-dev curl
```

Запустите:
```bash
chmod +x setup_deps.sh
./setup_deps.sh
```

## Сборка

### Java версия
```bash
compile.bat  # Windows
compile.sh   # Linux
```

### C++ версия
```bash
build_cpp.bat  # Windows
build_cpp.sh   # Linux
```

## Запуск

### Java версия

#### Windows
```bash
launch.bat
```

Или напрямую:
```bash
java -jar build/RaveXLauncher.jar
```

#### Linux
```bash
chmod +x launch.sh
./launch.sh
```

Или напрямую:
```bash
java -jar build/RaveXLauncher.jar
```

### C++ версия

#### Windows
```bash
launch_cpp.bat
```

Или напрямую:
```bash
build/RaveXLauncher.exe
```

#### Linux
```bash
chmod +x launch_cpp.sh
./launch_cpp.sh
```

Или напрямую:
```bash
./build/RaveXLauncher
```

## Использование

1. Запустите лаунчер
2. Выберите JAR файл чита RaveX (или он будет найден автоматически)
3. Нажмите кнопку "Launch RaveX"
4. Следите за логами в окне лаунчера

## Системные требования

### Java версия
- Java 8 или выше
- Windows 10+ или Linux
- 2GB RAM (рекомендуется)

### C++ версия
- C++17 компилятор
- CMake 3.10+
- SDL2
- OpenGL 3.0+
- Windows 10+ или Linux
- 2GB RAM (рекомендуется)

## Лицензия

GNU General Public License v3.0

# KickX Launcher

KickX Launcher is the official installer and launcher for RaveX on Windows.

It is built with native C++23, Win32 API, GDI+ and WinHTTP. The launcher manages Java, downloads Minecraft assets, installs Fabric, Forge or Quilt, and starts the game with your selected instance and account.

System requirements: Windows 10 x64. No additional dependencies are required, Java is handled automatically.

How to use:
1. Download KickXSetup.exe from Releases
2. Run it and follow the Setup wizard
3. Choose the install folder and shortcuts
4. Wait for Java and Minecraft files to download and pass verification
5. Use the shortcut KickX on your Desktop or Start Menu to run kickx_launcher

The launcher itself is not distributed separately. It is installed only through KickXSetup, which contains the launcher as an embedded resource and extracts it to the install folder.

Notes:
* Default install location is LocalAppData\KickX
* Instances are stored in .kickxxx\instances
* The launcher blocks running under Wine or Proton and shows a warning, please use the Linux build on Linux
* All icons are embedded, no extra download is needed

---

# KickX Launcher

KickX Launcher это официальный установщик и лаунчер для RaveX на Windows.

Он написан на нативном C++23, Win32 API, GDI+ и WinHTTP. Лаунчер настраивает Java, загружает ассеты Minecraft, устанавливает Fabric, Forge или Quilt и запускает игру с выбранным инстансом и аккаунтом.

Системные требования: Windows 10 x64. Дополнительные зависимости не нужны, Java устанавливается автоматически.

Как пользоваться:
1. Скачайте KickXSetup.exe из раздела Releases
2. Запустите его и следуйте шагам мастера установки
3. Выберите папку установки и ярлыки
4. Дождитесь загрузки Java и файлов Minecraft и прохождения проверки
5. Используйте ярлык KickX на рабочем столе или в меню Пуск чтобы запустить kickx_launcher

Сам лаунчер отдельно не поставляется. Он устанавливается только через KickXSetup, который содержит лаунчер как встроенный ресурс и извлекает его в папку установки.

Примечания:
* Папка установки по умолчанию это LocalAppData\KickX
* Инстансы хранятся в .kickxxx\instances
* Лаунчер блокирует запуск под Wine или Proton и показывает предупреждение, на Linux пожалуйста используйте Linux сборку
* Все иконки встроены, дополнительная загрузка не нужна

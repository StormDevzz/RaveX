# KickX Launcher

KickX Launcher is the official installer and launcher for RaveX on Windows.

It is built with native C++23, Win32 API, GDI+ and WinHTTP. The launcher manages Java, downloads Minecraft assets, installs Fabric, Forge or Quilt, and starts the game with your selected instance and account.

System requirements: Windows 10 / Windows 7 x64 (recommended). No additional dependencies are required, Java is handled automatically.

How to use:
1. Download KickXSetup.exe from Releases
2. Run it and follow the Setup wizard
3. Choose the install folder and shortcuts
4. Wait for Java and Minecraft files to download and pass verification
5. Use the shortcut KickX on your Desktop or Start Menu to run kickx_launcher

Verify your download:
* Every release contains a `SHA256SUMS.txt` asset and the release description has a table with SHA-256 hashes and direct VirusTotal links for KickXSetup.exe and kickx_launcher.exe
* Check the file: `Get-FileHash .\KickXSetup.exe -Algorithm SHA256` (PowerShell) or `sha256sum KickXSetup.exe` (Linux)
* Compare the result with `SHA256SUMS.txt` or open `https://www.virustotal.com/gui/file/<your-hash>` to scan it with 70+ antivirus engines

Notes:
* Default install location is LocalAppData\KickX
* Instances are stored in .kickxxx\instances
* The optional "Start with Windows" checkbox in Setup creates a visible shortcut in your Startup folder - you can remove it at any time (shell:startup)
* The launcher blocks running under Wine or Proton and shows a warning, please use the Linux build on Linux
* All icons are embedded, no extra download is needed

---

# KickX Launcher

KickX Launcher это официальный установщик и лаунчер для RaveX на Windows.

Он написан на нативном C++23, Win32 API, GDI+ и WinHTTP. Лаунчер настраивает Java, загружает ассеты Minecraft, устанавливает Fabric, Forge или Quilt и запускает игру с выбранным инстансом и аккаунтом.

Системные требования: Windows 10 / Windows 7 x64 (рекомендуется). Дополнительные зависимости не нужны, Java устанавливается автоматически.

Как пользоваться:
1. Скачайте KickXSetup.exe из раздела Releases
2. Запустите его и следуйте шагам мастера установки
3. Выберите папку установки и ярлыки
4. Дождитесь загрузки Java и файлов Minecraft и прохождения проверки
5. Используйте ярлык KickX на рабочем столе или в меню Пуск чтобы запустить kickx_launcher

Проверка загрузки:
* В каждом релизе есть ассет `SHA256SUMS.txt`, а в описании релиза — таблица с SHA-256 хешами и прямыми ссылками на VirusTotal для KickXSetup.exe и kickx_launcher.exe
* Проверь файл: `Get-FileHash .\KickXSetup.exe -Algorithm SHA256` (PowerShell) или `sha256sum KickXSetup.exe` (Linux)
* Сравни результат с `SHA256SUMS.txt` или открой `https://www.virustotal.com/gui/file/<твой-хеш>` — файл просканируют 70+ антивирусных движков

Примечания:
* Папка установки по умолчанию это LocalAppData\KickX
* Инстансы хранятся в .kickxxx\instances
* Необязательный чекбокс «Запуск с Windows» в установщике создаёт видимый ярлык в папке Автозагрузка - его всегда можно удалить (shell:startup)
* Лаунчер блокирует запуск под Wine или Proton и показывает предупреждение, на Linux пожалуйста используйте Linux сборку
* Все иконки встроены, дополнительная загрузка не нужна

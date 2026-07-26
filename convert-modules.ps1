$root = "C:\Users\nprevenant\RaveX\src\main\java\ravex\modules"
$files = Get-ChildItem $root -Recurse -Filter *.java | Where-Object {
    $_.FullName -notmatch 'Module\.java$|Category\.java$|\\annotations\\|NukerData\.java$|InvCleanData\.java$|AutoReGearData\.java$'
}

$convertedFields = @{}  # file -> [field names converted]
$total = 0

foreach ($file in $files) {
    $text = [System.IO.File]::ReadAllText($file.FullName)
    if ($text -notmatch '@ModuleInfo') { continue }
    $total++
    $orig = $text
    $cn = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
    $usesParam = $false
    $configureLines = @()
    $convertedFieldNames = @()

    # 1. extends Module
    $text = $text -replace '\s+extends\s+(ravex\.modules\.)?Module\b', ''

    # 2. @Override
    $text = $text -replace '(?<=\n)[ \t]*@Override\n', "`n"

    # 3. Конструктор — удаляем + извлекаем setVisible
    $text = [regex]::Replace($text, "(?<=\n)(private|public)\s+$cn\s*\(\s*\)\s*\{([^}]*)\}", {
        param($m)
        $body = $m.Groups[2].Value -replace 'super\("[^"]*"\)\s*;?\s*', ''
        $body = $body.Trim()
        foreach ($l in ($body -split ';' | ForEach-Object { $_.Trim() })) {
            if ($l -match '(\w+)\.setVisible\((.+)\)$') {
                $configureLines += "        self.getParameter(`"$($Matches[1])`").setVisible($($Matches[2]));"
            }
        }
        return "`n"
    })

    # 4. Парсинг и конвертация полей-параметров
    $lines = $text -split "`n"
    $out = @(); $i = 0
    while ($i -lt $lines.Count) {
        $line = $lines[$i]; $trimmed = $line.Trim()
        # Определяем тип: ищем "public final Type field = new Type(" или "public static final Type field = ..."
        $isStatic = $trimmed -match '^public\s+static\s+final\s+'
        $isRegular = $trimmed -match '^public\s+final\s+'
        if (-not ($isStatic -or $isRegular)) { $out += $line; $i++; continue }

        $paramType = $null; $fname = $null
        foreach ($t in @('BooleanParameter','ModeParameter','NumberParameter','ColorParameter','MultiSelectParameter','StringParameter')) {
            $pat = if ($isStatic) { "^public\s+static\s+final\s+$t\s+(\w+)\s*=" }
                    else { "^public\s+final\s+$t\s+(\w+)\s*=" }
            # Проверяем на new Type( с опциональным кастом
            $fullCheck = $trimmed -match "$pat\s*(?:\(\s*$t\s*\)\s*)?new\s+$t\("
            if ($fullCheck) { $paramType = $t; $fname = $Matches[1]; break }
        }

        if (-not $paramType) {
            # Проверяем Parameter<T> pattern (Parameter<String> lang = new ModeParameter(...))
            if ($trimmed -match '^public\s+(?:static\s+)?final\s+Parameter\s*<[^>]+>\s+(\w+)\s*=' -and $trimmed -match 'new\s+(BooleanParameter|ModeParameter|NumberParameter|ColorParameter|MultiSelectParameter)\(') {
                $paramType = $Matches[1]; $fname = $Matches[1]; $isWrappedParam = $true
            } else {
                $out += $line; $i++; continue
            }
        }

        # static final → не конвертируем
        if ($isStatic) { $out += $line; $i++; continue }

        # Собираем всё до matching ");"
        $accum = $trimmed; $depth = 0; $started = $false
        foreach ($c in $accum.ToCharArray()) {
            if ($c -eq '(') { $started = $true; $depth++ }
            elseif ($c -eq ')') { $depth-- }
        }
        while ($depth -gt 0 -or ($depth -eq 0 -and $started -and -not ($accum -match '\);\s*$' -or $accum -match '\)\.setVisible'))) {
            $i++; $next = $lines[$i].Trim(); $accum += " " + $next
            foreach ($c in $next.ToCharArray()) {
                if ($c -eq '(') { $depth++ }
                elseif ($c -eq ')') { $depth-- }
            }
        }

        # Извлекаем inline .setVisible(...)
        $visInline = $null
        if ($accum -match '\)\.setVisible\(([\s\S]+)\);\s*$') {
            $visInline = $Matches[1]
            $accum = $accum -replace '\)\.setVisible\([\s\S]+\);\s*$', ');'
        }

        # Парсим label и аргументы
        $label = $fname; $args = ''
        if ($accum -match '"([^"]*)"') {
            $label = $Matches[1]
            $args = $accum -replace '^.*?"[^"]*"\s*,\s*', '' -replace '\);?\s*$', ''
        }

        $annot = "name = `"$label`""
        $usesParam = $true
        $convertedFieldNames += $fname

        switch ($paramType) {
            'BooleanParameter' {
                $def = if ($args -match '(true|false)') { $Matches[1] } else { 'false' }
                $out += "    @Parameter($annot)"
                $out += "    public boolean $fname = $def;"
            }
            'ModeParameter' {
                $def = ''; $modes = ''
                if ($args -match '"([^"]*)"') { $def = $Matches[1] }
                if ($args -match '(?:java\.util\.)?List\.of\s*\(([^)]*)\)') { $modes = $Matches[1] }
                if ($modes) { $annot += ", modes = {$($modes -replace '"','"')}" }
                $out += "    @Parameter($annot)"
                $out += "    public String $fname = `"$def`";"
            }
            'StringParameter' {
                $def = ''
                if ($args -match '"([^"]*)"') { $def = $Matches[1] }
                $out += "    @Parameter($annot)"
                $out += "    public String $fname = `"$def`";"
            }
            'NumberParameter' {
                $parts = ($args -split ',' | ForEach-Object { $_.Trim() })
                $def = if ($parts[0]) { $parts[0] } else { '0.0' }
                $min = if ($parts[1]) { $parts[1] } else { '0' }
                $max = if ($parts[2]) { $parts[2] } else { '100' }
                $step = if ($parts[3]) { $parts[3] } else { '1' }
                $annot += ", min = $min, max = $max, step = $step"
                $out += "    @Parameter($annot)"
                $out += "    public double $fname = $def;"
            }
            'ColorParameter' {
                $cval = if ($args -match '(0x[0-9a-fA-F]+)') { $Matches[1] } else { '0xFFFFFFFF' }
                $annot += ", color = true"
                $out += "    @Parameter($annot)"
                $out += "    public int $fname = $cval;"
            }
            'MultiSelectParameter' {
                $defaults = ''; $options = ''
                if ($args -match 'List\.of\s*\(([^)]*)\)\s*,\s*(?:java\.util\.)?List\.of\s*\(([^)]*)\)') {
                    $defaults = $Matches[1]; $options = $Matches[2]
                }
                if ($options) { $annot += ", options = {$($options -replace '"','"')}" }
                $defList = if ($defaults) { "List.of($defaults)" } else { 'List.of()' }
                $out += "    @Parameter($annot)"
                $out += "    public java.util.List<String> $fname = $defList;"
            }
        }

        if ($visInline) {
            $configureLines += "        self.getParameter(`"$fname`").setVisible($visInline);"
        }
        $i++
    }
    $text = $out -join "`n"

    # 5. Замена getValue() на прямое обращение для сконвертированных полей
    foreach ($fn in $convertedFieldNames) {
        # field.getValue() → field
        $text = $text -replace "(?<![.\w])$fn\.getValue\(\)", $fn
        # field.getValue().intValue() → (int) field
        $text = $text -replace "(?<![.\w])$fn\.getValue\(\)\.intValue\(\)", "(int)($fn)"
        # field.getValue().doubleValue() → field
        $text = $text -replace "(?<![.\w])$fn\.getValue\(\)\.doubleValue\(\)", $fn
        # field.getValue().longValue() → (long) field
        $text = $text -replace "(?<![.\w])$fn\.getValue\(\)\.longValue\(\)", "(long)($fn)"
        # field.getValue().floatValue() → (float) field
        $text = $text -replace "(?<![.\w])$fn\.getValue\(\)\.floatValue\(\)", "(float)($fn)"
        # field.setValue(x) — такого в модулях обычно не бывает, но на всякий:
        $text = $text -replace "(?<![.\w])$fn\.setValue\(([^)]+)\)", "$fn = `$1"
    }

    # 6. getParameters() override
    $text = [regex]::Replace($text, '(?<=\n)\s*public\s+(java\.util\.)?List\s*<\s*(ravex\.parameter\.)?Parameter\s*<\s*\?\s*>\s*>\s+getParameters\s*\(\s*\)\s*\{[\s\S]*?return\s+\w+\s*;\s*\}', "`n")

    # 7. Импорты — удаляем только типы, которые больше не используются
    $importsToRemove = @('BooleanParameter','ModeParameter','NumberParameter','ColorParameter','MultiSelectParameter')
    $remainingText = $text -replace 'import\s+ravex\.parameter\.(\w+);', ''
    foreach ($it in $importsToRemove) {
        if ($text -match "import ravex\.parameter\.$it;" -and $remainingText -notmatch "(?<!\w)$it(?!\w)") {
            $text = $text -replace "import ravex\.parameter\.$it;\s*", ''
        }
    }

    # 8. Добавить import @Parameter
    if ($usesParam -and $text -notmatch 'import ravex\.modules\.annotations\.Parameter') {
        $text = $text -replace '(import ravex\.modules\.annotations\.ModuleInfo;)', "`$1`nimport ravex.modules.annotations.Parameter;"
    }

    # 9. configure() блок
    if ($configureLines.Count -gt 0) {
        $block = "`n    void configure(ravex.module.ModuleProxy self) {`n"
        $block += ($configureLines -join "`n") + "`n    }"
        $idx = $text.LastIndexOf('}')
        $text = $text.Substring(0, $idx) + $block + "`n" + $text.Substring($idx)
    }

    # 10. enabled =  → setEnabled(
    $text = $text -replace '(?<!\w)enabled\s*=\s*(false|true)', 'setEnabled($1)'

    if ($text -ne $orig) {
        [System.IO.File]::WriteAllText($file.FullName, $text)
    }
}

Write-Host "Processed $total modules"

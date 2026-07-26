$moduleRoot = "src/main/java/ravex/modules"
$files = Get-ChildItem -Path $moduleRoot -Recurse -Filter "*.java" | Where-Object {
    $_.FullName -notlike "*\annotations\*" -and
    $_.Name -ne "Module.java" -and
    $_.Name -ne "Category.java"
}

function Remove-Method {
    param([string]$content, [string]$pattern)
    $result = $content
    $count = 0
    # try up to 5 times (most files have 0-1 overrides)
    while ($count -lt 5) {
        $m = [regex]::Match($result, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
        if (-not $m.Success) { break }
        $result = $result.Remove($m.Index, $m.Length)
        $count++
    }
    return $result
}

$changed = 0
foreach ($file in $files) {
    $content = Get-Content -LiteralPath $file.FullName -Raw
    $original = $content

    # 1. extends ravex.modules.Module -> extends Module
    if ($content -match 'extends ravex\.modules\.Module\b') {
        if ($content -notmatch 'import ravex\.modules\.Module;') {
            $content = $content -replace '(package ravex(?:\.\w+)*;)', "`$1`nimport ravex.modules.Module;"
        }
        $content = $content -replace 'extends ravex\.modules\.Module\b', 'extends Module'
    }

    # 2. Remove getParameters() override (only if it uses java.util.List or fully qualified)
    $getParamsPat = 'public\s+(java\.util\.)?List\s*<\s*(ravex\.parameter\.)?Parameter\s*<\s*\?\s*>\s*>\s+getParameters\s*\(\s*\)\s*\{.*?^\s*\}'
    $content = Remove-Method -content $content -pattern $getParamsPat

    # 3. Remove itz() static method
    $itzPat = 'public\s+static\s+\w+\s+itz\s*\(\s*\)\s*\{.*?^\s*\}'
    $content = Remove-Method -content $content -pattern $itzPat

    # 4. Remove maybeEnabled() static method
    $maybePat = 'public\s+static\s+boolean\s+maybeEnabled\s*\(\s*\)\s*\{.*?^\s*\}'
    $content = Remove-Method -content $content -pattern $maybePat

    # 5. Ensure Category import
    if ($content -match 'Category\.(?:COMBAT|RENDER|PLAYER|MOVEMENT|MISC|WORLD|CLIENT|HUD)' -and $content -notmatch 'import ravex\.modules\.Category;') {
        $content = $content -replace '(package ravex(?:\.\w+)*;)', "`$1`nimport ravex.modules.Category;"
    }

    # 6. Remove unused ModuleManager import
    $content = $content -replace 'import ravex\.manager\.ModuleManager;\s*\n', ''

    # 7. Clean multiple blank lines
    $content = $content -replace '(\r?\n){3,}', "`r`n`r`n"

    if ($content -cne $original) {
        Set-Content -LiteralPath $file.FullName -Value $content -NoNewline
        $changed++
    }
}

Write-Host "Done. Cleaned $changed files."

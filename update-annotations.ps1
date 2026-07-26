$moduleRoot = "src/main/java/ravex/modules"
$files = Get-ChildItem -Path $moduleRoot -Recurse -Filter "*.java" | Where-Object { $_.FullName -notlike "*\annotations\*" }

$categoryMap = @{
    "Combat"                            = "Category.COMBAT"
    "Render"                            = "Category.RENDER"
    "Player"                            = "Category.PLAYER"
    "net.minecraft.world.entity.player.Player" = "Category.PLAYER"
    "Movement"                          = "Category.MOVEMENT"
    "Misc"                              = "Category.MISC"
    "World"                             = "Category.WORLD"
    "Client"                            = "Category.CLIENT"
    "HUD"                               = "Category.HUD"
}

function EnsureImport {
    param([string]$content)
    if ($content -notmatch "import ravex.modules.Category;") {
        $content = $content -replace "(package ravex\.modules\.\w+;|package ravex\.modules;)", "`$1`nimport ravex.modules.Category;"
    }
    return $content
}

$changed = 0
foreach ($file in $files) {
    $content = Get-Content -LiteralPath $file.FullName -Raw
    $original = $content
    $needsHud = $false

    # Match @ModuleInfo(name = "...", category = "...") or with extra attrs
    if ($content -match '@ModuleInfo\(name\s*=\s*"([^"]+)",\s*category\s*=\s*"([^"]+)"') {
        $name = $matches[1]
        $oldCategory = $matches[2]

        if ($categoryMap.ContainsKey($oldCategory)) {
            $newCategory = $categoryMap[$oldCategory]
            $oldStr = 'category = "' + $oldCategory + '"'
            $newStr = 'category = ' + $newCategory

            if ($oldCategory -eq "HUD") {
                $needsHud = $true
            }

            $content = $content -replace [regex]::Escape($oldStr), $newStr
        }
    }

    if ($needsHud -and $content -notmatch 'hud\s*=\s*true') {
        $content = $content -replace '(category = Category\.HUD[^)]*)', '$1, hud = true'
    }

    if ($content -cne $original) {
        $content = EnsureImport -content $content
        if (-not (Test-Path $file.Directory)) { New-Item -ItemType Directory -Path $file.Directory -Force | Out-Null }
        Set-Content -LiteralPath $file.FullName -Value $content -NoNewline
        Write-Host "  Updated: $($file.FullName)"
        $changed++
    }
}

Write-Host "Done. Updated $changed files."

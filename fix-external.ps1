$root = "C:\Users\nprevenant\RaveX\src\main\java\ravex"
$excludes = @("parameter\", "module\ModuleProxy.java")

$files = Get-ChildItem $root -Recurse -Filter *.java | Where-Object {
    $full = $_.FullName.Replace($root + "\", "")
    $skip = $false
    foreach ($e in $excludes) { if ($full.StartsWith($e)) { $skip = $true } }
    -not $skip
}

$count = 0
foreach ($file in $files) {
    $text = [System.IO.File]::ReadAllText($file.FullName)
    $orig = $text

    # Remove old Parameter imports if we have @Parameter annotation
    if ($file.FullName -match '\\modules\\.+\.java$' -and $text -match 'import ravex\.modules\.annotations\.Parameter;') {
        $text = $text -replace 'import ravex\.parameter\.Parameter;\s*', ''
    }

    # Handle .getValue().intValue() → nothing
    $text = $text -replace '\.getValue\(\)\.intValue\(\)', ''
    $text = $text -replace '\.getValue\(\)\.doubleValue\(\)', ''
    $text = $text -replace '\.getValue\(\)\.floatValue\(\)', ''
    $text = $text -replace '\.getValue\(\)\.longValue\(\)', ''
    $text = $text -replace '\.getValue\(\)', ''
    $text = $text -replace '\.setValue\(([^)]+)\)', ' = $1'

    if ($text -ne $orig) {
        [System.IO.File]::WriteAllText($file.FullName, $text)
        $count++
    }
}

Write-Host "Modified $count files"

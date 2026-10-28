$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskJar = Join-Path $taskRoot 'target\pos-accessibility-lab.jar'
if (-not (Test-Path -LiteralPath $taskJar)) { throw 'Compila primero: mvn clean package' }
$taskDist = Join-Path $taskRoot 'dist'
[System.IO.Directory]::CreateDirectory($taskDist) | Out-Null
$taskZip = Join-Path $taskDist 'pos-accessibility-lab.zip'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskStream = [System.IO.File]::Open($taskZip, [System.IO.FileMode]::Create)
$taskArchive = New-Object System.IO.Compression.ZipArchive($taskStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $taskFiles = @('pom.xml', 'README.md', 'THIRD-PARTY.txt', 'run.sh', '.gitignore', '.gitattributes') | ForEach-Object { Get-Item -LiteralPath (Join-Path $taskRoot $_) }
    foreach ($taskFolder in @('src', 'tests', 'examples', 'scripts')) {
        $taskFiles += Get-ChildItem -LiteralPath (Join-Path $taskRoot $taskFolder) -File -Recurse | Where-Object { $_.FullName -notmatch '[\\/]__pycache__[\\/]' -and $_.Extension -ne '.pyc' }
    }
    foreach ($taskFile in $taskFiles) {
        $taskRelative = $taskFile.FullName.Substring($taskRoot.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($taskArchive, $taskFile.FullName, 'pos-accessibility-lab/' + $taskRelative) | Out-Null
    }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($taskArchive, $taskJar, 'pos-accessibility-lab/pos-accessibility-lab.jar') | Out-Null
} finally { $taskArchive.Dispose(); $taskStream.Dispose() }
Get-Item -LiteralPath $taskZip | Select-Object FullName, Length

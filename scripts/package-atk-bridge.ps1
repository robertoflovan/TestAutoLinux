param([Parameter(Mandatory=$true)][string]$DeliveryDirectory)
$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskDelivery = (Resolve-Path -LiteralPath $DeliveryDirectory).Path
$taskResults = Get-Content -Raw -LiteralPath (Join-Path $taskDelivery 'TEST-RESULTS.txt')
if ($taskResults -notmatch 'Corrected: six value checks passed') { throw 'Missing successful integration test result' }
foreach ($taskLine in Get-Content -LiteralPath (Join-Path $taskDelivery 'SHA256SUMS')) {
    if ($taskLine -notmatch '^([a-f0-9]{64})  ([a-zA-Z0-9.-]+)$') { throw 'Invalid checksum entry' }
    $taskExpected = $Matches[1]
    $taskName = $Matches[2]
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $taskDelivery $taskName)).Hash -ne $taskExpected) { throw "Checksum mismatch: $taskName" }
}
$taskStage = Join-Path $taskRoot 'target/atk-package-v1'
if (Test-Path -LiteralPath $taskStage) { throw 'Staging directory already exists; preserve it and use a fresh workspace' }
$taskPackage = Join-Path $taskStage 'puente-corregido-v1'
New-Item -ItemType Directory -Path $taskPackage | Out-Null
Get-ChildItem -LiteralPath $taskDelivery -File | Copy-Item -Destination $taskPackage
Copy-Item -LiteralPath (Join-Path $taskRoot 'examples/PosSwingControl.java') -Destination $taskPackage
Copy-Item -LiteralPath (Join-Path $taskRoot 'examples/run-swing-atk.sh') -Destination $taskPackage
Copy-Item -LiteralPath (Join-Path $taskRoot 'scripts/run-fixed-atk-example.sh') -Destination (Join-Path $taskPackage 'probar.sh')
Copy-Item -LiteralPath (Join-Path $taskRoot 'patches/DELIVERY-README.txt') -Destination (Join-Path $taskPackage 'LEEME.txt')
$taskOut = Join-Path $taskRoot 'experimental'
New-Item -ItemType Directory -Force -Path $taskOut | Out-Null
$taskZip = Join-Path $taskOut 'puente-corregido-v1.zip'
if (Test-Path -LiteralPath $taskZip) { throw 'Versioned ZIP already exists; do not overwrite a published binary' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskStage, $taskZip)
$taskHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $taskZip).Hash.ToLowerInvariant()
[System.IO.File]::WriteAllText((Join-Path $taskOut 'SHA256SUMS'), "$taskHash  puente-corregido-v1.zip`n", [System.Text.UTF8Encoding]::new($false))
Get-Item -LiteralPath $taskZip | Select-Object FullName,Length

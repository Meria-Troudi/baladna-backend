# Downloads 20 different cover images (Picsum: one stable photo per seed; replace with real guest-house photos later).
# into uploads\accommodations with the same naming pattern as AccommodationService.uploadCover:
#   {accommodation-uuid}_{timestamp}_guesthouse_cover.jpg
#
# Matches the 20 UUIDs from scripts/seed-20-tunisia-accommodations.sql
# After running, execute scripts/update-seed-accommodation-covers.sql on MySQL.
#
# Usage (PowerShell):
#   cd ...\baladna-backend-develop\scripts
#   .\download-seed-cover-images.ps1

$ErrorActionPreference = "Stop"
$backendRoot = Split-Path $PSScriptRoot -Parent
$dest = Join-Path $backendRoot "uploads\accommodations"
New-Item -ItemType Directory -Force -Path $dest | Out-Null

# Base timestamp (fixed so it matches update-seed-accommodation-covers.sql)
$tsBase = [long]1744020000000

# Picsum Photos: stable image per seed (https://picsum.photos). Good for dev/demo; replace with your own photos for production.
$urls = @()
for ($k = 1; $k -le 20; $k++) {
    $urls += "https://picsum.photos/seed/baladna-tn-guest-$k/1200/800.jpg"
}

$uuids = @(
    "a1000001-0001-4001-8001-000000000001",
    "a1000001-0001-4001-8001-000000000002",
    "a1000001-0001-4001-8001-000000000003",
    "a1000001-0001-4001-8001-000000000004",
    "a1000001-0001-4001-8001-000000000005",
    "a1000001-0001-4001-8001-000000000006",
    "a1000001-0001-4001-8001-000000000007",
    "a1000001-0001-4001-8002-000000000008",
    "a1000001-0001-4001-8002-000000000009",
    "a1000001-0001-4001-8002-00000000000a",
    "a1000001-0001-4001-8002-00000000000b",
    "a1000001-0001-4001-8002-00000000000c",
    "a1000001-0001-4001-8002-00000000000d",
    "a1000001-0001-4001-8002-00000000000e",
    "a1000001-0001-4001-8003-00000000000f",
    "a1000001-0001-4001-8003-000000000010",
    "a1000001-0001-4001-8003-000000000011",
    "a1000001-0001-4001-8003-000000000012",
    "a1000001-0001-4001-8003-000000000013",
    "a1000001-0001-4001-8003-000000000014"
)

if ($urls.Count -ne $uuids.Count) { throw "URL count must match UUID count" }

for ($i = 0; $i -lt $uuids.Count; $i++) {
    $u = $uuids[$i]
    $ts = $tsBase + $i + 1
    $name = "${u}_${ts}_guesthouse_cover.jpg"
    $out = Join-Path $dest $name
    Write-Host "Downloading -> $name"
    Invoke-WebRequest -Uri $urls[$i] -OutFile $out -UseBasicParsing
}

Write-Host ""
Write-Host "Done. Files are in: $dest"
Write-Host "Run on MySQL: scripts/update-seed-accommodation-covers.sql"

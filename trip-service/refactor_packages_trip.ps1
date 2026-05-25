$path = "src/main/java/com/miniuber/trip"
Get-ChildItem -Path $path -Recurse -Filter "*.java" | ForEach-Object {
    $c = Get-Content $_.FullName -Raw
    $c = $c -replace "package com.miniuber.ride", "package com.miniuber.trip.ride"
    $c = $c -replace "package com.miniuber.location", "package com.miniuber.trip.location"
    $c = $c -replace "import com.miniuber.ride", "import com.miniuber.trip.ride"
    $c = $c -replace "import com.miniuber.location", "import com.miniuber.trip.location"
    $c = $c -replace "com.miniuber.ride.", "com.miniuber.trip.ride."
    $c = $c -replace "com.miniuber.location.", "com.miniuber.trip.location."
    Set-Content -Path $_.FullName -Value $c -NoNewline
}
Write-Host "Refactoring complete."

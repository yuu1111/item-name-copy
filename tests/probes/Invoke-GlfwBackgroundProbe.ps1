[CmdletBinding()]
param(
    [string]$LwjglVersion = '3.3.3',
    [string]$Driver = "$env:LOCALAPPDATA\Programs\Cua\cua-driver\bin\cua-driver.exe",
    [string]$GradleCache = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"
)

$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$output = Join-Path $root "build/driver-probe/$LwjglVersion"
New-Item -ItemType Directory -Force $output | Out-Null
$jars = foreach ($module in @('lwjgl', 'lwjgl-glfw')) {
    foreach ($name in @("$module-$LwjglVersion.jar", "$module-$LwjglVersion-natives-windows.jar")) {
        $matches = @(Get-ChildItem (Join-Path $GradleCache "org.lwjgl/$module/$LwjglVersion") -Recurse -File -Filter $name)
        if ($matches.Count -ne 1) { throw "Expected one cached dependency: $name" }
        $matches[0].FullName
    }
}
& javac -cp ($jars -join ';') -d $output (Join-Path $PSScriptRoot 'GlfwBackgroundProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
$classPath = $output + ';' + ($jars -join ';')
$log = Join-Path $output 'glfw.log'
$process = Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-cp', ('"' + $classPath + '"'), 'GlfwBackgroundProbe') -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError (Join-Path $output 'glfw.err.log') -PassThru
try {
    $deadline = [DateTime]::UtcNow.AddSeconds(10)
    do {
        Start-Sleep -Milliseconds 100
        $ready = Get-Content $log | Select-String '^READY pid=(\d+) window_id=(\d+)'
    } until ($ready -or $process.HasExited -or [DateTime]::UtcNow -ge $deadline)
    if (!$ready) { throw "Probe did not become ready: $log" }
    $target = @{ kind = 'window'; pid = [int]$ready.Matches[0].Groups[1].Value; window_id = [long]$ready.Matches[0].Groups[2].Value }
    $evidence = [ordered]@{ lwjgl = $LwjglVersion; target = $target }
    $evidence.cursorBefore = & $Driver call get_cursor_position '{}'
    $evidence.hotkey = & $Driver call hotkey (@{ target = $target; keys = @('ctrl', 'c'); delivery_mode = 'background'; session = 'minecraft-background-probe' } | ConvertTo-Json -Compress)
    $evidence.pointer = & $Driver call move_cursor (@{ target = $target; scope = 'window'; x = 120; y = 100; session = 'minecraft-background-probe' } | ConvertTo-Json -Compress)
    $evidence.cursorAfter = & $Driver call get_cursor_position '{}'
    $evidence | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $output 'driver.json')
    if (!$process.WaitForExit(35000)) { throw 'Probe did not exit before its deadline' }
    Get-Content $log
    Get-Content (Join-Path $output 'glfw.err.log')
    Write-Output "Driver evidence: $output/driver.json"
    $result = Get-Content $log | Select-String '^RESULT keyEvents=(\d+) controlC=(\d+) pointerEvents=(\d+) focusGains=(\d+)$'
    if (!$result -or [int]$result.Matches[0].Groups[2].Value -eq 0 -or [int]$result.Matches[0].Groups[3].Value -eq 0 -or [int]$result.Matches[0].Groups[4].Value -ne 0) {
        throw 'Background input acceptance failed; inspect the GLFW and driver evidence'
    }
} finally {
    if (!$process.HasExited) { Stop-Process -Id $process.Id }
}

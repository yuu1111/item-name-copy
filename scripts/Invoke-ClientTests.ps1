param(
    [string[]]$Target,
    [ValidateSet('emi', 'rei')]
    [string]$RecipeViewer,
    [switch]$Resume
)

$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$matrix = (& (Join-Path $PSScriptRoot 'Get-BuildMatrix.ps1') | ConvertFrom-Json).include
if ($RecipeViewer) {
    if (-not $Target) { $Target = @('1.21.1-fabric') }
    if (@($Target).Count -ne 1 -or $Target[0] -ne '1.21.1-fabric') {
        throw 'Recipe viewer tests currently use the 1.21.1-fabric compatibility target'
    }
}
if ($Target) {
    $unknown = @($Target | Where-Object { $_ -notin $matrix.node })
    if ($unknown.Count -gt 0) { throw "Unknown targets: $($unknown -join ', ')" }
    $matrix = @($matrix | Where-Object { $_.node -in $Target })
}

$reportDirectory = Join-Path $repository 'build/runtime-verification'
$manifestPath = Join-Path $reportDirectory 'matrix.json'
$logDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ('itemnamecopy-client-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $reportDirectory, $logDirectory -Force | Out-Null
$wrapper = if ($IsWindows -or $env:OS -eq 'Windows_NT') { Join-Path $repository 'gradlew.bat' } else { Join-Path $repository 'gradlew' }
$launcher = if ($IsWindows -or $env:OS -eq 'Windows_NT') { $wrapper } else { (Get-Command bash).Source }
$wrapperArguments = if ($IsWindows -or $env:OS -eq 'Windows_NT') { @() } else { @($wrapper) }

Push-Location $repository
try {
    $sourceFiles = @(git ls-files --cached --others --exclude-standard | Sort-Object -Unique | Where-Object {
        $_ -match '^(src/|core/src/|gradle/|gradle\.properties$|versions/.+/gradle.properties$|build.+\.gradle\.kts$|settings\.gradle\.kts$|stonecutter\.gradle\.kts$|minecraft-client-testkit/|tests/client/)'
    })
    if ($LASTEXITCODE -ne 0) { throw 'Could not enumerate test inputs' }
    $digest = [System.Security.Cryptography.IncrementalHash]::CreateHash([System.Security.Cryptography.HashAlgorithmName]::SHA256)
    foreach ($sourceFile in $sourceFiles) {
        $digest.AppendData([System.Text.Encoding]::UTF8.GetBytes($sourceFile + "`n"))
        $digest.AppendData([System.IO.File]::ReadAllBytes((Join-Path $repository $sourceFile)))
    }
    $digest.AppendData([System.Text.Encoding]::UTF8.GetBytes("recipeViewer=$RecipeViewer`n"))
    $sourceHash = [Convert]::ToHexString($digest.GetHashAndReset()).ToLowerInvariant()
    $digest.Dispose()

    $entries = @{}
    if (Test-Path -LiteralPath $manifestPath) {
        $existing = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
        if ($existing.source -eq $sourceHash) {
            foreach ($entry in $existing.targets) { $entries[$entry.target] = $entry }
        }
    }

    $buildLog = Join-Path $logDirectory 'harness-build.log'
    & $launcher @wrapperArguments :client-tests:assemble --console=plain *> $buildLog
    $buildExit = $LASTEXITCODE
    if ($buildExit -ne 0) {
        Get-Content -LiteralPath $buildLog -Tail 40
        throw "Client test bootstrap build failed ($buildExit)"
    }

    $failed = 0
    foreach ($node in $matrix) {
        if ($Resume -and $entries.ContainsKey($node.node) -and $entries[$node.node].status -eq 'passed') {
            Write-Output "$($node.node): already passed for the same inputs"
            continue
        }
        $logPath = Join-Path $logDirectory ($node.node + '.log')
        $reportPath = Join-Path $repository "versions/$($node.node)/build/reports/client-test/results.json"
        if (Test-Path -LiteralPath $reportPath) { Remove-Item -LiteralPath $reportPath }
        $timer = [System.Diagnostics.Stopwatch]::StartNew()
        Write-Output "$($node.node): starting client verification"
        $viewerArgument = if ($RecipeViewer) { @("-PrecipeViewerTest=$RecipeViewer") } else { @() }
        & $launcher @wrapperArguments "-Ptarget=$($node.node)" "-PclientTestSource=$sourceHash" @viewerArgument ":$($node.node):runClient" --console=plain *> $logPath
        $runExit = $LASTEXITCODE
        $timer.Stop()
        $result = if (Test-Path -LiteralPath $reportPath) { Get-Content -Raw -LiteralPath $reportPath | ConvertFrom-Json } else { $null }
        $passed = $runExit -eq 0 -and $null -ne $result -and $result.target -eq $node.node -and
            $result.source -eq $sourceHash -and $result.failed -eq 0 -and $result.passed -gt 0 -and $result.passed -eq $result.expectedTests
        $status = if ($passed) { 'passed' } else { 'failed' }
        if (-not $passed) { $failed++ }
        $entries[$node.node] = [ordered]@{
            target = $node.node
            status = $status
            exitCode = $runExit
            durationSeconds = [math]::Round($timer.Elapsed.TotalSeconds, 1)
            passed = if ($null -ne $result) { $result.passed } else { 0 }
            failed = if ($null -ne $result) { $result.failed } else { $null }
            report = if ($null -ne $result) { $reportPath } else { $null }
            log = $logPath
        }
        [ordered]@{
            schemaVersion = 1
            source = $sourceHash
            targets = @($entries.Values | Sort-Object target)
        } | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding utf8
        Write-Output "$($node.node): $status ($([math]::Round($timer.Elapsed.TotalSeconds, 1)) s)"
        if (-not $passed) {
            if ($null -ne $result) { $result.tests | Where-Object status -eq 'failed' | ForEach-Object { Write-Output "  $($_.name): $($_.message)" } }
            else { Get-Content -LiteralPath $logPath -Tail 20 }
        }
    }
    Write-Output "Client verification finished: $failed failed; report: $manifestPath"
    if ($failed -ne 0) { exit 1 }
} finally {
    Pop-Location
}

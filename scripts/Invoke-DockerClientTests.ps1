param(
    [string[]]$Target,
    [ValidateSet('all', 'synthetic', 'external')]
    [string]$Suite = 'all',
    [switch]$Resume,
    [switch]$FailedOnly,
    [switch]$List
)

$ErrorActionPreference = 'Stop'
function Get-TestImageFingerprint {
    $contents = & docker image inspect item-name-copy-e2e:local --format '{{json .RootFS.Layers}}|{{json .Config}}|{{.Os}}|{{.Architecture}}'
    if ($LASTEXITCODE -ne 0 -or -not $contents) { throw 'Could not identify Docker image contents' }
    $digest = [System.Security.Cryptography.SHA256]::Create()
    try {
        return [Convert]::ToHexString($digest.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($contents))).ToLowerInvariant()
    } finally {
        $digest.Dispose()
    }
}

$repository = Split-Path -Parent $PSScriptRoot
$directory = Join-Path $repository 'build/docker-e2e'
$manifest = Join-Path $directory 'matrix.json'
$entries = @{}
if (Test-Path -LiteralPath $manifest) {
    $previous = Get-Content -Raw -LiteralPath $manifest | ConvertFrom-Json
    foreach ($entry in $previous.targets) {
        if (-not $entry.image) { $entry | Add-Member -NotePropertyName image -NotePropertyValue $previous.image -Force }
        $entries["$($entry.target)/$($entry.suite)"] = $entry
    }
} elseif ($FailedOnly) {
    throw "No previous results: $manifest"
}
$matrix = @((& (Join-Path $PSScriptRoot 'Get-BuildMatrix.ps1') | ConvertFrom-Json).include)
if ($Target) {
    $unknown = @($Target | Where-Object { $_ -notin $matrix.node })
    if ($unknown.Count) { throw "Unknown targets: $($unknown -join ', ')" }
    $matrix = @($matrix | Where-Object { $_.node -in $Target })
}
$suites = if ($Suite -eq 'all') { @('synthetic', 'external') } else { @($Suite) }
$selected = @{}
foreach ($node in $matrix) {
    foreach ($inputSuite in $suites) {
        $key = "$($node.node)/$inputSuite"
        if (-not $FailedOnly -or ($entries.ContainsKey($key) -and $entries[$key].status -eq 'failed')) {
            $selected[$key] = $true
        }
    }
}
if ($List) {
    foreach ($node in $matrix) {
        foreach ($inputSuite in $suites) {
            if ($selected.ContainsKey("$($node.node)/$inputSuite")) { Write-Output "$($node.node) $inputSuite" }
        }
    }
    return
}
if ($selected.Count -eq 0) {
    Write-Output 'No matching failed runs to retry'
    return
}

Push-Location $repository
try {
    $compose = @('compose', '-f', 'tests/e2e/compose.yaml')
    & docker @compose build
    if ($LASTEXITCODE -ne 0) { throw 'Docker image build failed' }
    $image = Get-TestImageFingerprint
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $failures = 0
    foreach ($node in $matrix) {
        foreach ($inputSuite in $suites) {
            $key = "$($node.node)/$inputSuite"
            if (-not $selected.ContainsKey($key)) { continue }
            if ($Resume -and $entries.ContainsKey($key) -and $entries[$key].status -eq 'passed' -and $entries[$key].image -eq $image) {
                Write-Output "$key already passed for this image"
                continue
            }
            $currentImage = Get-TestImageFingerprint
            if ($currentImage -ne $image) { throw 'Docker image changed during the batch; rerun with a stable image' }
            $runId = "$($node.node.Replace('.', '_'))-$inputSuite-$([guid]::NewGuid().ToString('N'))"
            Write-Output "$key starting"
            & docker @compose run --rm --no-deps -e "E2E_RUN_ID=$runId" -e E2E_TIMEOUT_SECONDS=1800 client bash tests/e2e/minecraft/run.sh $node.node $inputSuite
            $runExit = $LASTEXITCODE
            $reportPath = Join-Path $directory "$runId/client-report/results.json"
            $result = if (Test-Path -LiteralPath $reportPath) { Get-Content -Raw -LiteralPath $reportPath | ConvertFrom-Json } else { $null }
            $expected = if ($inputSuite -eq 'external') { 3 } else { 13 }
            $passed = $runExit -eq 0 -and $null -ne $result -and $result.target -eq $node.node -and
                $result.failed -eq 0 -and $result.passed -eq $expected -and $result.expectedTests -eq $expected
            $status = if ($passed) { 'passed' } else { 'failed' }
            if (-not $passed) { $failures++ }
            $entries[$key] = [ordered]@{
                target = $node.node
                suite = $inputSuite
                status = $status
                exitCode = $runExit
                report = $reportPath
                image = $image
            }
            [ordered]@{
                schemaVersion = 2
                image = $image
                targets = @($entries.Values | Sort-Object target, suite)
            } | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$manifest.tmp" -Encoding utf8
            Move-Item -LiteralPath "$manifest.tmp" -Destination $manifest -Force
            Write-Output "$key $status"
        }
    }
    Write-Output "Finished: $failures failed; report: $manifest"
    if ($failures) { exit 1 }
} finally {
    Pop-Location
}

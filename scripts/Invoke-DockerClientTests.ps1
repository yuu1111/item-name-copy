param(
    [string[]]$Target,
    [ValidateSet('all', 'synthetic', 'external')]
    [string]$Suite = 'all',
    [switch]$Resume,
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
$matrix = @((& (Join-Path $PSScriptRoot 'Get-BuildMatrix.ps1') | ConvertFrom-Json).include)
if ($Target) {
    $unknown = @($Target | Where-Object { $_ -notin $matrix.node })
    if ($unknown.Count) { throw "Unknown targets: $($unknown -join ', ')" }
    $matrix = @($matrix | Where-Object { $_.node -in $Target })
}
$suites = if ($Suite -eq 'all') { @('synthetic', 'external') } else { @($Suite) }
if ($List) {
    foreach ($node in $matrix) {
        foreach ($inputSuite in $suites) { Write-Output "$($node.node) $inputSuite" }
    }
    return
}

Push-Location $repository
try {
    $compose = @('compose', '-f', 'tests/e2e/compose.yaml')
    & docker @compose build
    if ($LASTEXITCODE -ne 0) { throw 'Docker image build failed' }
    $image = Get-TestImageFingerprint
    $directory = Join-Path $repository 'build/docker-e2e'
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $manifest = Join-Path $directory 'matrix.json'
    $entries = @{}
    if ($Resume -and (Test-Path -LiteralPath $manifest)) {
        $previous = Get-Content -Raw -LiteralPath $manifest | ConvertFrom-Json
        if ($previous.image -eq $image) {
            foreach ($entry in $previous.targets) { $entries["$($entry.target)/$($entry.suite)"] = $entry }
        }
    }
    $failures = 0
    foreach ($node in $matrix) {
        foreach ($inputSuite in $suites) {
            $key = "$($node.node)/$inputSuite"
            if ($Resume -and $entries.ContainsKey($key) -and $entries[$key].status -eq 'passed') {
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
            }
            [ordered]@{
                schemaVersion = 1
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

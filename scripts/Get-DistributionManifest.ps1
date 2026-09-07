param(
    [string] $OutputPath = (Join-Path $PSScriptRoot '../build/distribution-manifest.json')
)

$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$project = Get-Content -LiteralPath (Join-Path $repository 'distribution/project.json') -Raw | ConvertFrom-Json
$properties = Get-Content -LiteralPath (Join-Path $repository 'gradle.properties') -Raw | ConvertFrom-StringData
$matrix = & (Join-Path $PSScriptRoot 'Get-BuildMatrix.ps1') | ConvertFrom-Json
$version = $properties.'mod.version'
if (-not $version -or $project.status -ne 'draft') {
    throw 'A mod version and draft project metadata are required'
}

foreach ($resource in @($project.descriptionFile, $project.iconFile, 'LICENSE')) {
    if (-not $resource -or -not (Test-Path -LiteralPath (Join-Path $repository $resource) -PathType Leaf)) {
        throw "Missing distribution resource: $resource"
    }
}
if (@($project.platforms).Count -ne 2 -or 'modrinth' -notin $project.platforms -or 'curseforge' -notin $project.platforms) {
    throw 'Expected Modrinth and CurseForge distribution targets'
}

$artifacts = @($matrix.include | ForEach-Object {
    $node = $_
    $nodeProperties = Get-Content -LiteralPath (Join-Path $repository "versions/$($node.node)/gradle.properties") -Raw |
        ConvertFrom-StringData
    [ordered]@{
        target = $node.node
        file = "item-name-copy-$version+$($node.loader)-mc$($node.minecraft).jar"
        minecraftVersions = @($node.minecraft)
        loader = $node.loader
        loaderVersion = $nodeProperties.loader_version
        java = [int]$node.java
        runtimeVerification = 'pending'
    }
})
if (@($artifacts.file | Select-Object -Unique).Count -ne $matrix.include.Count) {
    throw 'Distribution artifact names are not unique'
}

$manifest = [ordered]@{
    schemaVersion = 1
    status = 'draft'
    project = $project
    version = $version
    artifacts = $artifacts
}
$resolvedOutput = [IO.Path]::GetFullPath($OutputPath)
New-Item -ItemType Directory -Path (Split-Path -Parent $resolvedOutput) -Force | Out-Null
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $resolvedOutput -Encoding utf8
Write-Output "Generated $($artifacts.Count) draft distribution targets: $resolvedOutput"

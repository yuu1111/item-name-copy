$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$nodes = @(Get-ChildItem -LiteralPath (Join-Path $repository 'versions') -Directory | Sort-Object Name | ForEach-Object {
    $propertiesPath = Join-Path $_.FullName 'gradle.properties'
    if (Test-Path -LiteralPath $propertiesPath) {
        $properties = Get-Content -LiteralPath $propertiesPath -Raw | ConvertFrom-StringData
        if (-not $properties.build_script -or -not $properties.java_version -or -not $properties.loader_version) {
            throw "Incomplete target properties: $($_.Name)"
        }
        $minecraft, $loader = $_.Name -split '-', 2
        if ($minecraft -ne $properties.minecraft_version -or $loader -notin @('fabric', 'forge', 'neoforge')) {
            throw "Invalid target name: $($_.Name)"
        }
        [ordered]@{
            node = $_.Name
            minecraft = $minecraft
            loader = $loader
            java = $properties.java_version
        }
    }
})
if ($nodes.Count -eq 0) { throw 'No build targets configured' }
@{ include = $nodes } | ConvertTo-Json -Depth 3 -Compress

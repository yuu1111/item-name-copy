param(
    [string] $OutputPath = (Join-Path $PSScriptRoot '../build/available-targets.json')
)

$ErrorActionPreference = 'Stop'

function Get-LatestLoaderVersion {
    param([string[]] $Versions)

    if (-not $Versions -or $Versions.Count -eq 0) {
        return $null
    }

    $stable = @($Versions | Where-Object { $_ -notmatch '-' })
    $candidates = if ($stable.Count -gt 0) { $stable } else { $Versions }
    return $candidates | Sort-Object { [version](($_ -split '-')[0]) } -Descending | Select-Object -First 1
}

$minecraftSource = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
$fabricGameSource = 'https://meta.fabricmc.net/v2/versions/game'
$fabricLoaderSource = 'https://meta.fabricmc.net/v2/versions/loader'
$forgeSource = 'https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml'
$neoforgeSource = 'https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml'
$legacyNeoforgeSource = 'https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml'

$minecraftManifest = Invoke-RestMethod $minecraftSource
$fabricGames = Invoke-RestMethod $fabricGameSource
$fabricLoaders = Invoke-RestMethod $fabricLoaderSource
$forgeMetadata = Invoke-RestMethod $forgeSource
$neoforgeMetadata = Invoke-RestMethod $neoforgeSource
$legacyNeoforgeMetadata = Invoke-RestMethod $legacyNeoforgeSource

$releases = @($minecraftManifest.versions | Where-Object {
    $_.type -eq 'release' -and [version]$_.id -ge [version]'1.14'
})
$fabricLoader = $fabricLoaders | Where-Object stable | Select-Object -First 1 -ExpandProperty version
$targets = foreach ($release in $releases) {
    $minecraft = $release.id
    $forgeVersions = @($forgeMetadata.metadata.versioning.versions.version | Where-Object {
        $_.StartsWith("$minecraft-")
    } | ForEach-Object { ($_ -split '-', 2)[1] })

    if ($minecraft -eq '1.20.1') {
        $neoVersions = @($legacyNeoforgeMetadata.metadata.versioning.versions.version | Where-Object {
            $_.StartsWith('1.20.1-')
        } | ForEach-Object { ($_ -split '-', 2)[1] })
    } else {
        $version = [version]$minecraft
        $neoPrefix = if ($version.Major -ge 26) {
            $patch = [Math]::Max(0, $version.Build)
            "$($version.Major).$($version.Minor).$patch."
        } else {
            $patch = [Math]::Max(0, $version.Build)
            "$($version.Minor).$patch."
        }
        $neoVersions = @($neoforgeMetadata.metadata.versioning.versions.version | Where-Object {
            $_.StartsWith($neoPrefix)
        })
    }

    $loaders = [ordered]@{
        fabric = if ($fabricGames.version -contains $minecraft) { $fabricLoader } else { $null }
        forge = Get-LatestLoaderVersion $forgeVersions
        neoforge = Get-LatestLoaderVersion $neoVersions
    }

    [ordered]@{
        minecraft = $minecraft
        manifest = $release.url
        loaders = $loaders
    }
}

$report = [ordered]@{
    sources = @($minecraftSource, $fabricGameSource, $fabricLoaderSource, $forgeSource, $neoforgeSource, $legacyNeoforgeSource)
    releases = @($targets)
}
$resolvedOutput = [IO.Path]::GetFullPath($OutputPath)
New-Item -ItemType Directory -Path (Split-Path -Parent $resolvedOutput) -Force | Out-Null
$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $resolvedOutput -Encoding utf8
Write-Output "Discovered $($targets.Count) formal Minecraft releases: $resolvedOutput"

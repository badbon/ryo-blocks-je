[CmdletBinding()]
param(
 [Parameter(Mandatory)][string]$BaselineJar,[Parameter(Mandatory)][string]$CurrentJar,[Parameter(Mandatory)][string]$FabricApiJar,[Parameter(Mandatory)][string]$HarnessJar,[Parameter(Mandatory)][string]$WorldSource,[Parameter(Mandatory)][string]$OutputRoot,
 [Parameter(Mandatory)][string]$BaselineIris,[Parameter(Mandatory)][string]$BaselineSodium,[Parameter(Mandatory)][string]$BaselineIndium,[Parameter(Mandatory)][string]$BaselineShaderpack,[Parameter(Mandatory)][string]$BaselineConfig,
 [int]$WarmupSeconds=20,[int]$MeasurementSeconds=60,[int]$Pairs=3,
 [ValidateSet('paired','single','aggregate')][string]$Mode='paired',[ValidateSet('baseline','current')][string]$Variant,[int]$RunIndex=1,[string[]]$RunDirectory
)
$ErrorActionPreference='Stop'
if($WarmupSeconds -lt 1 -or $MeasurementSeconds -lt 1 -or $Pairs -lt 1){throw 'Durations and pair count must be positive.'}
$root=Split-Path -Parent $PSScriptRoot; $runner=Join-Path $root 'performance/runner'; $currentCommit=(git -C $root rev-parse HEAD).Trim()
function Get-ArtifactHash([string]$Path){(Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()}
function Require-InputPath([string]$Path){if(!(Test-Path -LiteralPath $Path)){throw "Missing required input: $Path"}}
function Get-Percentile([double[]]$Values,[double]$P){if($Values.Count -eq 0){return 0};$s=@($Values|Sort-Object);return $s[[math]::Ceiling($P*$s.Count)-1]}
function Get-Median([double[]]$Values){Get-Percentile $Values .5}
function Assert-ModProfileNames([string]$Variant,[string[]]$Names){
 $actual=@($Names|Sort-Object)
 if($Variant -eq 'current'){
  $expected=@('ryo-performance-harness.jar','ryo-product.jar')
  if($actual.Count -ne 2 -or (($actual -join '|') -ne ($expected -join '|'))){throw 'Current profile contamination: expected only product and harness JARs'}
 } else {
  if($actual.Count -ne 6){throw 'Baseline profile contamination: expected six standalone mod JARs'}
  foreach($required in @('fabric-api.jar','ryo-performance-harness.jar','ryo-product.jar')){if($actual -notcontains $required){throw "Baseline profile missing: $required"}}
 }
}
function Assert-SelfTest {
 if((Get-Median @(10,30,20)) -ne 20){throw 'Median self-test failed'}
 if((Get-Percentile @(10,20,30,40) .95) -ne 40){throw 'Percentile self-test failed'}
 $improvement=((Get-Median @(120,120))/(Get-Median @(100,100))-1)*100
 if([math]::Abs($improvement-20) -gt .0001){throw 'Improvement self-test failed'}
 Assert-ModProfileNames current @('ryo-product.jar','ryo-performance-harness.jar')
 Assert-ModProfileNames baseline @('fabric-api.jar','ryo-product.jar','ryo-performance-harness.jar','iris.jar','sodium.jar','indium.jar')
 $rejected=$false;try{Assert-ModProfileNames current @('ryo-product.jar','ryo-performance-harness.jar','fabric-api.jar')}catch{$rejected=$true}
 if(!$rejected){throw 'Current external Fabric API contamination self-test failed'}
}
function Write-HostSnapshot([string]$Path,[string]$Phase){
 $gpu=$null; $smi=Get-Command nvidia-smi -ErrorAction SilentlyContinue
 if($smi){try{$gpu=& $smi.Source '--query-gpu=name,utilization.gpu,utilization.memory,memory.used,memory.total,temperature.gpu' '--format=csv,noheader,nounits' 2>$null}catch{$gpu="query failed: $($_.Exception.Message)"}}
 $java=@(Get-Process javaw,java -ErrorAction SilentlyContinue|ForEach-Object {[ordered]@{name=$_.ProcessName;id=$_.Id;cpuSeconds=$_.CPU;workingSetBytes=$_.WorkingSet64;privateBytes=$_.PrivateMemorySize64}})
 $os=Get-CimInstance Win32_OperatingSystem
 [ordered]@{phase=$Phase;capturedAt=[DateTime]::UtcNow.ToString('o');host=[ordered]@{freePhysicalMemoryKB=$os.FreePhysicalMemory;totalVisibleMemoryKB=$os.TotalVisibleMemorySize};javaProcesses=$java;nvidiaSmi=$gpu}|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $Path
}
function Assert-ProfileHash([string]$Path,[string]$Expected,[string]$Label){if((Get-ArtifactHash $Path) -ne $Expected.ToLowerInvariant()){throw "$Label SHA-256 mismatch"}}
function Assert-RunLog([string]$Log,[string]$Variant){
 if(!(Test-Path -LiteralPath $Log)){throw "Missing latest.log for $Variant"}; $text=Get-Content -LiteralPath $Log -Raw
 if($Variant -eq 'baseline'){
  foreach($required in @('iris 1.7.6+mc1.20.1','sodium 0.5.13+mc1.20.1','indium 1.0.36+mc1.20.1','Using shaderpack: Ryo-Vanilla-Tint.zip','ryo-blocks:ryo_blocks')){if($text -notmatch [regex]::Escape($required)){throw "Baseline log missing: $required"}}
 } else {
  foreach($forbidden in @('iris ','sodium ','indium ')){if($text -match "(?im)^.*\b$forbidden"){throw "Current log contains forbidden runtime: $forbidden"}}
  if($text -notmatch 'ryo-blocks:ryo_blocks'){throw 'Current log missing ryo-blocks resource identity'}
 }
}
Assert-SelfTest
@($BaselineJar,$CurrentJar,$FabricApiJar,$HarnessJar,$WorldSource,$BaselineIris,$BaselineSodium,$BaselineIndium,$BaselineShaderpack,$BaselineConfig)|ForEach-Object {Require-InputPath $_}
Assert-ProfileHash $CurrentJar 'F57933EA99FB451DB15EFB08C099ADCEEDF2ABFA02874F2DF428E52244557A9B' 'Current product JAR'
Assert-ProfileHash $BaselineJar '57DCA917D83627F3DDB8AF8D0B6B6C3321B6105D6DFAC8251F05C46B3E3BF11E' 'Baseline product JAR'
New-Item -ItemType Directory -Force $OutputRoot|Out-Null
$worldLines=@(Get-ChildItem -LiteralPath $WorldSource -Recurse -File|Sort-Object FullName|ForEach-Object {"$($_.FullName.Substring($WorldSource.Length)):$((Get-ArtifactHash $_.FullName))"})
$worldHash=([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes(($worldLines -join "`n"))))).Replace('-','').ToLowerInvariant()
function Stage([string]$Variant,[int]$Index){
 $id="$Variant-$Index-$(Get-Date -Format yyyyMMdd-HHmmss-fff)";$path=Join-Path $root "performance-runs/$id";New-Item -ItemType Directory -Force "$path/mods","$path/config","$path/shaderpacks","$path/saves"|Out-Null
 Copy-Item -LiteralPath $WorldSource -Destination "$path/saves/Hardcore" -Recurse;Copy-Item -LiteralPath $HarnessJar -Destination "$path/mods/ryo-performance-harness.jar"
 if($Variant -eq 'baseline'){Copy-Item -LiteralPath $FabricApiJar -Destination "$path/mods/fabric-api.jar";Copy-Item -LiteralPath $BaselineJar -Destination "$path/mods/ryo-product.jar";Copy-Item -LiteralPath @($BaselineIris,$BaselineSodium,$BaselineIndium) -Destination "$path/mods/";Copy-Item -LiteralPath $BaselineShaderpack -Destination "$path/shaderpacks/Ryo-Vanilla-Tint.zip";Copy-Item -LiteralPath $BaselineConfig -Destination "$path/config/iris.properties"}else{Copy-Item -LiteralPath $CurrentJar -Destination "$path/mods/ryo-product.jar"}
 $mods=@(Get-ChildItem "$path/mods" -File);Assert-ModProfileNames $Variant @($mods.Name)
 $meta=[ordered]@{variant=$Variant;runIndex=$Index;runId=$id;createdAt=[DateTime]::UtcNow.ToString('o');productSha256=Get-ArtifactHash "$path/mods/ryo-product.jar";harnessSha256=Get-ArtifactHash "$path/mods/ryo-performance-harness.jar";externalFabricApiStaged=($Variant -eq 'baseline');mods=@($mods|Sort-Object Name|ForEach-Object {[ordered]@{name=$_.Name;sha256=Get-ArtifactHash $_.FullName}});worldTreeSha256=$worldHash;baselineCommit='2979e2a80aa07f37090d0db49b3d91dd5eabfc59';currentCommit=$currentCommit;expectedRuntimeContract=[ordered]@{framebuffer='1280x720';fov=70;renderDistance=12;simulationDistance=12;vsync=$false;fpsCap=10000}}
 $json=$meta|ConvertTo-Json -Depth 6 -Compress;Set-Content -LiteralPath "$path/profile-manifest.json" -Value $json;return @($path,$json,$id)
}
function Accept-StableRun([string]$Dest,[string]$ExpectedVariant,[string]$Profile){
 if(!(Test-Path -LiteralPath "$Dest/report.json") -or !(Test-Path -LiteralPath "$Dest/host-pre.json") -or !(Test-Path -LiteralPath "$Dest/host-post.json")){throw "Incomplete stable run: $Dest"}
 if($Profile){Copy-Item -LiteralPath "$Profile/profile-manifest.json" -Destination "$Dest/profile-manifest.json" -Force;Copy-Item -LiteralPath "$Profile/logs/latest.log" -Destination "$Dest/latest.log" -Force}
 Assert-RunLog "$Dest/latest.log" $ExpectedVariant
 $report=Get-Content "$Dest/report.json" -Raw|ConvertFrom-Json;$contract=$report.runtimeContract
 if($report.variant -ne $ExpectedVariant -or $report.completionState -ne 'complete' -or $contract.framebuffer -ne '1280x720' -or $contract.fov -ne 70 -or $contract.renderDistance -ne 12 -or $contract.simulationDistance -ne 12 -or $contract.vsync -ne $false -or $contract.fpsCap -ne 10000){throw "Observed runtime contract failed $Dest"}
 if(!(Get-ChildItem -LiteralPath "$Dest/screenshots" -Filter 'native-framebuffer.png' -ErrorAction SilentlyContinue)){throw "Missing completed native framebuffer screenshot for $Dest"};return $report
}
if($Mode -eq 'single'){
 if(!$Variant){throw 'Single mode requires -Variant'};$stage=Stage $Variant $RunIndex;$dest=Join-Path $OutputRoot $stage[2];New-Item -ItemType Directory -Force $dest|Out-Null;Write-HostSnapshot "$dest/host-pre.json" 'pre';$relative=[IO.Path]::GetRelativePath($runner,$stage[0]);& "$root/gradlew.bat" --no-daemon -p $runner runBenchmarkClient "-PryoPerformance.profile=$relative" "-PryoPerformance.output=$dest" "-PryoPerformance.variant=$Variant" "-PryoPerformance.runId=$($stage[2])" "-PryoPerformance.warmupSeconds=$WarmupSeconds" "-PryoPerformance.measurementSeconds=$MeasurementSeconds" "-PryoPerformance.metadata=$($stage[1])" --args='--quickPlaySingleplayer Hardcore';Write-HostSnapshot "$dest/host-post.json" 'post';Accept-StableRun $dest $Variant $stage[0]|Out-Null;Write-Output $dest;return
}
$all=if($Mode -eq 'aggregate'){@($RunDirectory|ForEach-Object {Accept-StableRun $_ ((Get-Content "$_/report.json" -Raw|ConvertFrom-Json).variant) $null})}else{@()}
if($Mode -eq 'paired'){foreach($index in 1..$Pairs){foreach($variant in 'baseline','current'){
 $stage=Stage $variant $index;$dest=Join-Path $OutputRoot $stage[2];New-Item -ItemType Directory -Force $dest|Out-Null;Write-HostSnapshot "$dest/host-pre.json" 'pre'
 $relative=[IO.Path]::GetRelativePath($runner,$stage[0]);& "$root/gradlew.bat" --no-daemon -p $runner runBenchmarkClient "-PryoPerformance.profile=$relative" "-PryoPerformance.output=$dest" "-PryoPerformance.variant=$variant" "-PryoPerformance.runId=$($stage[2])" "-PryoPerformance.warmupSeconds=$WarmupSeconds" "-PryoPerformance.measurementSeconds=$MeasurementSeconds" "-PryoPerformance.metadata=$($stage[1])" --args='--quickPlaySingleplayer Hardcore'
 Write-HostSnapshot "$dest/host-post.json" 'post'
 if($LASTEXITCODE -ne 0 -or !(Test-Path "$dest/report.json")){throw "Incomplete $($stage[2])"}
 Copy-Item -LiteralPath "$stage[0]/profile-manifest.json" -Destination "$dest/profile-manifest.json";Copy-Item -LiteralPath "$stage[0]/logs/latest.log" -Destination "$dest/latest.log";Assert-RunLog "$dest/latest.log" $variant
 $all += Accept-StableRun $dest $variant $stage[0]
}}}
$baseline=@($all|Where-Object variant -eq 'baseline'|Sort-Object runId);$current=@($all|Where-Object variant -eq 'current'|Sort-Object runId)
$baselineFps=@($baseline|ForEach-Object {[double]$_.submittedFps});$currentFps=@($current|ForEach-Object {[double]$_.submittedFps});$baselineMedian=Get-Median $baselineFps;$currentMedian=Get-Median $currentFps;$improvement=if($baselineMedian -gt 0){(($currentMedian/$baselineMedian)-1)*100}else{0}
$contractsPass=($baseline.Count -eq $Pairs -and $current.Count -eq $Pairs);$acceptancePass=($contractsPass -and $Pairs -ge 3 -and $improvement -ge 20)
$summary=[ordered]@{schema='ryo-performance-comparison/v2';createdAt=[DateTime]::UtcNow.ToString('o');pairs=$Pairs;diagnosticOnly=($Pairs -eq 1);acceptanceFormula='((currentMedianSubmittedFps / baselineMedianSubmittedFps) - 1) * 100; accepts only with Pairs >= 3, improvement >= 20, and all run contracts';contractsPass=$contractsPass;baseline=[ordered]@{submittedFpsPerRun=$baselineFps;medianSubmittedFps=$baselineMedian};current=[ordered]@{submittedFpsPerRun=$currentFps;medianSubmittedFps=$currentMedian};improvementPercent=$improvement;acceptancePass=$acceptancePass;runs=$all}
$summary|ConvertTo-Json -Depth 12|Set-Content -LiteralPath (Join-Path $OutputRoot 'comparison.json')
$md=@("# Ryo Blocks hidden-window diagnostic comparison",'',"Pairs: $Pairs",("Diagnostic only: {0}" -f ($Pairs -eq 1)),"Contracts passed: $contractsPass",'', '| Variant | Submitted FPS per run | Median submitted FPS |','| --- | --- | ---: |',("| Baseline | {0} | {1:N2} |" -f ($baselineFps -join ', '),$baselineMedian),("| Current | {0} | {1:N2} |" -f ($currentFps -join ', '),$currentMedian),'',("Calculated improvement: {0:N2}%" -f $improvement),("Acceptance pass: $acceptancePass"),'', 'This is hidden-window render-submission throughput, not displayed or presented FPS. GPU query results are non-blocking and reported separately per run.')
Set-Content -LiteralPath (Join-Path $OutputRoot 'comparison.md') -Value ($md -join "`n")

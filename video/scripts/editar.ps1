# The demo's edit from a recording of the person's voice reading the script (docs/GUION.md), in seven steps. The
# recipe (receta.json, in video/trabajo/<name>/) is decided once in step 4 and every later step only reads it.
# The original recording is only ever read. If a tool fails, it stops and says which, in which step and why.
#   powershell -File video/scripts/editar.ps1 -Video <path> [-Velocidad 1.1] [-SinRender]
param(
    [Parameter(Mandatory)][string]$Video,
    [double]$Velocidad = 1.1,
    [switch]$SinRender
)
$ErrorActionPreference = "Stop"
$env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
$models = Join-Path $HOME "modelos-video"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$voice = Join-Path $root "public\voice"
$inv = [Globalization.CultureInfo]::InvariantCulture

if (-not (Test-Path -LiteralPath $Video)) { throw "No encuentro el video: $Video" }
$Video = (Resolve-Path -LiteralPath $Video).Path
$name = ([IO.Path]::GetFileNameWithoutExtension($Video) -replace "[^\w-]", "_")
$work = Join-Path $root "trabajo\$name"
New-Item -ItemType Directory -Force $work, (Join-Path $root "out") | Out-Null

# Runs a tool; if it fails, says which, in which step and the last thing it said.
function Step($n, $tool, $what, [scriptblock]$run) {
    Write-Host "Paso $n · $what"
    $ErrorActionPreference = "Continue"  # the tools talk on stderr; only their exit code says if they failed
    $out = & $run 2>&1 | ForEach-Object { "$_" }
    if ($LASTEXITCODE -ne 0) {
        $why = ($out | Where-Object { $_ -match "\S" } | Select-Object -Last 4) -join " | "
        throw "FALLO en el paso $n ($tool): $why"
    }
    $out
}

# 1. The voice, alone: mono 16 kHz for Whisper and Silero, and mono 48 kHz to cut from.
Step 1 "FFmpeg" "saco el audio" {
    ffmpeg -hide_banner -loglevel error -y -i $Video -vn -ac 1 -ar 16000 (Join-Path $work "audio16k.wav")
    if ($LASTEXITCODE -eq 0) { ffmpeg -hide_banner -loglevel error -y -i $Video -vn -ac 1 -ar 48000 (Join-Path $work "voz48k.wav") }
} | Out-Null

# 2. Every word, with when it starts and ends. Whisper only listens where Silero hears a voice and starts each stretch
# afresh (-mc 0): over long silences it otherwise makes up sentences and repeats them.
Step 2 "whisper-cli" "transcribo palabra por palabra" {
    whisper-cli -m (Join-Path $models "ggml-large-v3.bin") -f (Join-Path $work "audio16k.wav") -l en -ml 1 -sow -oj -mc 0 `
        --vad -vm (Join-Path $models "ggml-silero-v5.1.2.bin") `
        -of (Join-Path $work "transcripcion") -t ([Environment]::ProcessorCount) -np
} | Out-Null

# 3. Where there is a voice (Silero says it in hundredths of a second).
$vad = Step 3 "whisper-vad-speech-segments" "busco los tramos con voz" {
    whisper-vad-speech-segments -vm (Join-Path $models "ggml-silero-v5.1.2.bin") -f (Join-Path $work "audio16k.wav") -np
}
$tramos = @($vad | Select-String "start = ([\d.]+), end = ([\d.]+)" | ForEach-Object {
    [ordered]@{ from = [double]::Parse($_.Matches[0].Groups[1].Value, $inv) / 100; to = [double]::Parse($_.Matches[0].Groups[2].Value, $inv) / 100 }
})
if ($tramos.Count -eq 0) { throw "FALLO en el paso 3 (whisper-vad-speech-segments): no encontró ningún tramo con voz" }
ConvertTo-Json @($tramos) -Compress | Set-Content (Join-Path $work "tramos.json") -Encoding ASCII

# 4. The recipe: which take is each line, which silences go, when each word is heard.
Step 4 "node (receta.ts)" "decido los cortes y escribo la receta" {
    node --no-warnings (Join-Path $PSScriptRoot "receta.ts") $work $Velocidad.ToString($inv)
}
$recipe = Get-Content (Join-Path $work "receta.json") -Raw -Encoding UTF8 | ConvertFrom-Json
if ($recipe.scenes.Count -eq 0) { throw "FALLO en el paso 4 (receta): no encontré ninguna frase del guion en la grabación" }

# 5. The face: this recording is only a voice.
Write-Host "Paso 5 · sin cara: es solo tu voz, me lo salto"

# 6. Each scene's voice, cut and sped up as the recipe says; then the demo drawn with it.
$draft = Join-Path $voice "borrador"
if (-not (Test-Path $draft)) {
    New-Item -ItemType Directory $draft | Out-Null
    Copy-Item (Join-Path $voice "vo_*.wav"), (Join-Path $voice "durations.json") $draft
}
foreach ($scene in $recipe.scenes) {
    $parts = for ($i = 0; $i -lt $scene.cuts.Count; $i++) {
        $c = $scene.cuts[$i]
        $len = $c.to - $c.from
        "[0:a]atrim=start=$($c.from.ToString($inv)):end=$($c.to.ToString($inv)),asetpts=PTS-STARTPTS,afade=t=in:d=0.01,afade=t=out:st=$([math]::Max(0, $len - 0.01).ToString($inv)):d=0.01[p$i]"
    }
    $joined = ($parts -join ";") + ";" + ((0..($scene.cuts.Count - 1) | ForEach-Object { "[p$_]" }) -join "") +
        "concat=n=$($scene.cuts.Count):v=0:a=1,atempo=$($Velocidad.ToString($inv))[out]"
    Step 6 "FFmpeg" "monto tu voz de la escena $($scene.n)" {
        ffmpeg -hide_banner -loglevel error -y -i (Join-Path $work "voz48k.wav") -filter_complex $joined -map "[out]" (Join-Path $voice "vo_$($scene.n).wav")
    } | Out-Null
}
$lengths = [ordered]@{}
Get-ChildItem (Join-Path $voice "vo_*.wav") | Sort-Object { [int]($_.BaseName -replace "\D") } | ForEach-Object {
    $lengths[($_.BaseName -replace "\D")] = [math]::Round([double]::Parse((ffprobe -v error -show_entries format=duration -of csv=p=0 $_.FullName), $inv), 2)
}
$lengths | ConvertTo-Json -Compress | Set-Content (Join-Path $voice "durations.json") -Encoding ASCII
$words = [ordered]@{}
foreach ($scene in $recipe.scenes) { $words["$($scene.n)"] = $scene.words }
[IO.File]::WriteAllText((Join-Path $voice "edicion.json"), ([ordered]@{ headlines = $recipe.headlines; words = $words } | ConvertTo-Json -Depth 6 -Compress))

if ($SinRender) { Write-Host "Listo sin renderizar: la receta está en $work"; return }
$render = Join-Path $root "out\$name`_render.mp4"
Push-Location $root
try {
    Step 6 "Remotion" "dibujo la demo con titulares y subtítulos (tarda varios minutos)" {
        npx remotion render Demo $render --log=error
    } | Out-Null
} finally { Pop-Location }

# 7. The file to hand in: H.264, the sound at -14 LUFS measured first and then set (two passes), ready to stream.
$norm = "loudnorm=I=-14:TP=-1.5:LRA=11"
$measure = Step 7 "FFmpeg" "mido el volumen (pasada 1 de 2)" {
    ffmpeg -hide_banner -y -i $render -af "$norm`:print_format=json" -f null NUL
}
$m = (($measure -join "`n") -replace "(?s)^.*(\{[^{}]*""input_i""[^{}]*\}).*$", '$1') | ConvertFrom-Json
$final = Join-Path $root "out\$name`_final.mp4"
if ($final -eq $Video) { throw "La salida pisaría el original; no sigo." }
Step 7 "FFmpeg" "entrego el video final (pasada 2 de 2)" {
    ffmpeg -hide_banner -loglevel error -y -i $render `
        -af "$norm`:measured_I=$($m.input_i):measured_TP=$($m.input_tp):measured_LRA=$($m.input_lra):measured_thresh=$($m.input_thresh):offset=$($m.target_offset):linear=true,aresample=48000" `
        -c:v libx264 -preset slow -crf 18 -profile:v high -pix_fmt yuv420p -c:a aac -b:a 192k -movflags +faststart $final
} | Out-Null
Remove-Item $render -Confirm:$false

$seconds = [double]::Parse((ffprobe -v error -show_entries format=duration -of csv=p=0 $final), $inv)
Write-Host ("Listo: {0} ({1:N1} s{2})" -f $final, $seconds, $(if ($seconds -gt 180.5) { ", PASA de los 3:00 que pide el hackatón" } else { "" }))

# The demo's draft voice: each scene's line (video/public/voice/lines.json) spoken by a Windows voice into vo_<n>.wav,
# and how long each lasts into durations.json, which the demo reads. For the final voice, record vo_1.wav ... vo_8.wav
# with the same words, put them in video/public/voice/, and run this with -MeasureOnly to update the lengths.
#   powershell -File video/scripts/voice.ps1 [-MeasureOnly]
param([switch]$MeasureOnly)
Add-Type -AssemblyName System.Speech
$dir = Join-Path $PSScriptRoot "..\public\voice"
$lines = Get-Content (Join-Path $dir "lines.json") -Raw -Encoding UTF8 | ConvertFrom-Json
$lengths = [ordered]@{}
foreach ($p in $lines.PSObject.Properties) {
    $file = Join-Path $dir "vo_$($p.Name).wav"
    if (-not $MeasureOnly) {
        $s = New-Object System.Speech.Synthesis.SpeechSynthesizer
        $s.SelectVoice("Microsoft Zira Desktop")
        $s.Rate = -1
        $s.SetOutputToWaveFile($file)
        $s.Speak($p.Value)
        $s.Dispose()
    }
    # A WAV's length: its sound's bytes (the "data" chunk, wherever a recorder put it) over its bytes per second.
    $bytes = [System.IO.File]::ReadAllBytes($file)
    $data = [System.Text.Encoding]::ASCII.GetString($bytes).IndexOf("data")
    $lengths[$p.Name] = [math]::Round([BitConverter]::ToInt32($bytes, $data + 4) / [BitConverter]::ToInt32($bytes, 28), 2)
}
$lengths | ConvertTo-Json -Compress | Set-Content (Join-Path $dir "durations.json") -Encoding ASCII
$lengths

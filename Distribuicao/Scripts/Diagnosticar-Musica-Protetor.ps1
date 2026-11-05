param([string]$Adb = '.\adb.exe', [string]$Dispositivo = '')
$ErrorActionPreference = 'Stop'
if (-not $Dispositivo) {
    $Conectados = @(& $Adb devices | ForEach-Object { if ($_ -match '^(\S+)\s+device\b') { $Matches[1] } })
    if ($Conectados.Count -ne 1) { throw 'Conecte uma TV ou informe -Dispositivo com o serial atual de adb devices.' }
    $Dispositivo = $Conectados[0]
}
function NaTv([string[]]$Argumentos) {
    $Saida = & $Adb -s $Dispositivo shell @Argumentos
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao consultar a TV. Confira a conexão ADB.' }
    $Saida
}
function ResumoMidia {
    foreach ($Linha in (NaTv @('dumpsys','media_session'))) {
        if ($Linha -match 'state=PlaybackState.*state=(-?\d+)') {
            $Estado = $Matches[1]
            $Acoes = '?'
            if ($Linha -match 'actions=(\d+)') { $Acoes = $Matches[1] }
            "state=$Estado; actions=$Acoes"
        } elseif ($Linha -match '^\s*package=([A-Za-z0-9_.]+)') {
            "package=$($Matches[1])"
        } elseif ($Linha -match '^\s*active=(true|false)') {
            "active=$($Matches[1])"
        } elseif ($Linha -match 'Last MediaButtonReceiver|Restored MediaButtonReceiver|Media button session is') {
            if ($Linha -match 'packageName=([A-Za-z0-9_.]+)') { "receiver-package=$($Matches[1])" }
            elseif ($Linha -match '(com\.[A-Za-z0-9_.]+)/') { "receiver-package=$($Matches[1])" }
            elseif ($Linha -match 'null') { 'receiver=null' }
            else { 'receiver=presente (dados omitidos)' }
        }
    }
}
$Relatorio = @(
    'LAUNCHERIN - DIAGNÓSTICO SOMENTE LEITURA'
    'Execute após reproduzir uma música, fechar o app, voltar à launcher e tentar Play uma vez.'
    'Nenhum título de música, álbum, URL, conta ou imagem será incluído.'
    ''
    'VERSÃO DA TV'
    (NaTv @('getprop','ro.build.version.sdk'))
    (NaTv @('getprop','ro.product.model'))
    ''
    'PROTETOR SELECIONADO'
    (NaTv @('dumpsys','dreams') | Select-String 'mCurrentDream|mDreamsEnabled|mDreamsActivate|mDreaming|mSystemDreamComponent|mCurrentDreamComponent')
    (NaTv @('settings','get','secure','screensaver_components'))
    (NaTv @('settings','get','secure','screensaver_default_component'))
    'screensaver_enabled:'
    (NaTv @('settings','get','secure','screensaver_enabled'))
    'screensaver_activate_on_sleep:'
    (NaTv @('settings','get','secure','screensaver_activate_on_sleep'))
    ''
    'ENERGIA E INATIVIDADE'
    (NaTv @('dumpsys','power') | Select-String 'mDreamsSupportedConfig|mDreamsEnabledSetting|mDreamsActivateOnSleepSetting|mScreenOffTimeoutSetting|mSleepTimeoutSetting|mAttentiveTimeoutSetting|mUserActivityTimeoutOverrideFromWindowManager|mStayOnWhilePluggedInSetting|mWakefulness=|mIsPowered=')
    ''
    'RECEPTORES E ESTADOS DE MÍDIA - SEM METADADOS'
    (ResumoMidia)
    ''
    'APPS DE MÚSICA INSTALADOS'
    (NaTv @('pm','list','packages') | Select-String 'spotify|youtube.*music|youtube\.tv$')
    ''
    'PROTETORES DESATIVADOS'
    (NaTv @('pm','list','packages','-d') | Select-String 'dream|backdrop|screensaver|ambient')
)
$Destino = Join-Path (Get-Location) 'diagnostico-launcherin.txt'
$Relatorio | Out-File -FilePath $Destino -Encoding utf8
Write-Host "Diagnóstico salvo em $Destino"

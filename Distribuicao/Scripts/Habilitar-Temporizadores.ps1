param(
    [string]$Adb = '.\adb.exe',
    [string]$Dispositivo = '',
    [switch]$PermitirAvisos
)
$ErrorActionPreference = 'Stop'
if (-not $Dispositivo) {
    $Conectados = @(& $Adb devices | ForEach-Object {
        if ($_ -match '^(\S+)\s+device\b') { $Matches[1] }
    })
    if ($Conectados.Count -ne 1) {
        throw 'Conecte a TV e execute novamente. Se houver mais de uma conexão, informe -Dispositivo seguido do serial mostrado por adb devices.'
    }
    $Dispositivo = $Conectados[0]
}
function NaTv([string[]]$Argumentos) {
    & $Adb -s $Dispositivo shell @Argumentos
    if ($LASTEXITCODE -ne 0) { throw 'A TV recusou o comando. Confira a autorização ADB.' }
}
# Somente permissões da Launcherin; nenhum aplicativo da TV é desativado.
NaTv @('pm', 'grant', 'app.launcherin.tv', 'android.permission.WRITE_SECURE_SETTINGS')
NaTv @('appops', 'set', 'app.launcherin.tv', 'WRITE_SETTINGS', 'allow')
if ($PermitirAvisos) {
    NaTv @('appops', 'set', 'app.launcherin.tv', 'SYSTEM_ALERT_WINDOW', 'allow')
}
NaTv @('am', 'start', '-n', 'app.launcherin.tv/.MainActivity')
Write-Host 'Permissões aplicadas. Configure os tempos na Launcherin. Para Google Fotos, escolha o álbum em Sistema > Modo ambiente na TV.'

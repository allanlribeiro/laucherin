# Launcherin preview11 — fotos com música, desligamento protegido

TV: versão0.1.0-preview11/código19. Celular: versão0.1.0-preview11/código11. Instale como atualização, sem desinstalar ou limpar dados.

## Comportamento solicitado

Configure o protetor em5 minutos e o desligamento em10 minutos. As fotos do Google Fotos podem aparecer enquanto a música continua. A proteção suspende apenas o desligamento automático durante uma música reconhecida; não mantém a launcher visível nem bloqueia o protetor. Ao pausar/encerrar a música, inicia um novo período de10 minutos. Uma nova reprodução suspende esse período novamente. Usar o controle também pode adiar o desligamento pelo temporizador nativo.

A proteção depende das permissões dos temporizadores e do acesso a mídias da Launcherin. Se o estado de reprodução ficar indisponível, o desligamento é mantido suspenso até que possa ser verificado, evitando cortar um áudio desconhecido. A tecla física de desligar continua sob controle do usuário.

## Instalar pela conexão ADB já autorizada

Abra o PowerShell na pasta platform-tools. Informe o seletor atual mostrado em adb devices-l; a porta pode mudar.

```powershell
$launcherinTv = Read-Host 'Seletor da TV (IP:porta ou serial de adb devices -l)'
$launcherinApk = Read-Host 'Caminho completo do APK Launcherin-TV-0.1.0-preview11.apk'
.\adb.exe -s $launcherinTv install -r $launcherinApk
.\adb.exe -s $launcherinTv shell pm grant app.launcherin.tv android.permission.WRITE_SECURE_SETTINGS
.\adb.exe -s $launcherinTv shell appops set app.launcherin.tv WRITE_SETTINGS allow
.\adb.exe -s $launcherinTv shell am start -n app.launcherin.tv/.MainActivity -a app.launcherin.tv.action.LAUNCHER_SETTINGS
```

A última linha abre as configurações internas da Launcherin. Selecione **TV aberta, gols e inatividade**, escolha **Protetor após:5min** e **Desligar após:10min**. Mantenha o acesso a mídias autorizado. O álbum já selecionado no Google Fotos é preservado.

Alternativa: atualize o app do celular, conecte à TV, e na aba **TV** configure **Protetor de tela** e **Desligamento automático**. As duas páginas compartilham o rascunho; aplique as alterações na TV. O app do celular inclui o mesmo APK TV19, com verificação de versão, tamanho, hash e assinatura.

Os comandos nativos usados durante o diagnóstico deixaram o protetor em5min e o desligamento desativado. A aplicação dos valores5/10 nas configurações da Launcherin, depois desta atualização, é necessária para ativar o novo desligamento protegido. Não basta alterar sleep_timeout para600000 pelo ADB na versão antiga.

## Conferir na TV

1. Sem música: deixe a launcher parada. O protetor deve usar o álbum escolhido pelo sistema, e o desligamento segue o período configurado de inatividade.
2. Com Spotify tocando pelo widget: o protetor continua permitido. Verifique que o áudio continua e que a TV não entra em standby após10min sem usar o controle.
3. Pause a música: o prazo de10min começa novamente. Durante esse período a proteção ainda mantém sleep_timeout em-1; isso é esperado. Ao terminar, o valor configurado de600000 é restaurado, e o Android considera também a atividade recente do controle.
4. Segure OK no Play para abrir o diagnóstico. O campo Proteção da música informa se há reprodução ativa, acesso/estado indisponível ou o período de proteção após pausar.

Consultas opcionais, somente leitura:

```powershell
.\adb.exe -s $launcherinTv shell dumpsys power | Select-String 'mScreenOffTimeoutSetting=|mSleepTimeoutSetting=|mAttentiveTimeoutSetting='
.\adb.exe -s $launcherinTv shell dumpsys dreams | Select-String 'mCurrentDream|mDreamsEnabledSetting'
```

Com música ativa, espera-se screen_off_timeout300000, sleep_timeout-1 e attentive_timeout-1. O álbum não foi trocado nem baixado pela Launcherin.

## Correção no Android14

No Android14, a atividade SystemUI.Somnambulator usada anteriormente tornou-se um atalho sem ação. A resposta Starting/Status:ok não comprovava a abertura das fotos. A Home agora usa o temporizador nativo, evitando abrir esse atalho e reiniciar a inatividade. Os botões de configuração abrem as configurações do protetor, com rótulo correspondente. Em versões anteriores, a rota legada permanece disponível.

## Validação e limites

Os testes incluem a política do prazo, configurações e preferências Android em Robolectric/API35, retomada/pausa, permissões, reinício e restauração sem alterar a aparência. Eles não representam reprodução real de Spotify nem medição do hardware.

Na TCL física, o usuário já confirmou o álbum Google Fotos funcionando ao ajustar o temporizador nativo. A proteção implementada nesta nova versão ainda precisa do teste após instalar o APK. Não confundir o resultado anterior com a validação desta nova proteção.

Nesta TCL, o YouTube Music reproduz pelo aplicativo principal do YouTube, que remove sua sessão ao sair e não publica os serviços/receptores consultados para retomada. O usuário confirmou o Spotify funcionando no widget e aceitou usar o YouTube Music no próprio app. Esta atualização não promete resolver o segundo caso.

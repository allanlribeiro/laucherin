# Fixtures Android de música e seleção de TV

Exclusivas de um emulador isolado de desenvolvimento: usam identificadores reconhecidos dos apps para testar comunicação entre processos, sem instalar os clientes reais nem reproduzir áudio. Não instale essas fixtures em uma TV ou celular de uso pessoal. Não são incluídas no APK da launcher.

Defina `ANDROID_SDK_ROOT` (plataforma 36, build-tools 35.0.0), `JAVA_HOME` e `LAUNCHERIN_TEST_KEYSTORE` (chave de desenvolvimento, alias launcherin, senha android). Execute `python3 build.py`. A saída padrão fica em `/tmp/launcherin-music-fixture`; altere com `LAUNCHERIN_FIXTURE_OUTPUT`.

As fixtures publicam sessões e notificações de mídia com faixa, artista, capa 64×64, progresso e callbacks de reprodução. Os comandos só alteram metadados da sessão de teste. O terceiro pacote registra um handler de canais para comprovar que um streaming não substitui o sintonizador.

No emulador, instale os três APKs gerados e os builds `assembleDebug` e `assembleDebugAndroidTest` da launcher. Autorize o listener de mídia com `adb shell cmd notification allow_listener app.launcherin.tv/.LauncherinMediaService`. Execute `adb shell am instrument -r -w -e dashboard true app.launcherin.tv.test/app.launcherin.tv.PlaybackInstrumentation`. O resultado deve informar 29 verificações e `INSTRUMENTATION_CODE: -1`. A captura fica no armazenamento externo privado da launcher como `dashboard-preview6.png`. O teste usa modo de navegação por controle; o emulador deve estar sem diálogos de sistema sobre a aplicação.

Sem `-e dashboard true`, o runner executa apenas os 14 testes do parser e despacho de conteúdo, sem fixtures. Esses testes não confirmam integração com os apps comerciais nem teclas de um controle físico.

Os modos button_only, ignore_transport e id_only cobrem controles de TV diferentes. O MediaBrowserService e o MediaReceiver permitem testar recuperação da sessão encerrada sem abrir uma Activity; reject_browser força a rota do receptor. Essas fixtures sinalizam estados de reprodução e não decodificam faixas reais. O teste também verifica limite de dez, exclusão de YouTube e remoção persistida pela UI.

# Launcherin

Fork independente do CleanYourTV 3.3.0, com aplicativo Android de configuração baseado no Clean Your TV App 0.4.7. As licenças MIT e os créditos dos projetos de origem foram preservados.

- TV: `app.launcherin.tv`, projeto em `launcher/`.
- Celular: `app.launcherin.mobile`, projeto ao lado em `../launcherin-mobile`.
- Fonte original da TV: https://github.com/Gathin23/cleanyourtv — base `e62aa10a033f715a8ac7116efcf91e01b145adbd`.
- Fonte original do celular: https://github.com/udhaykumarbala/cleanyourtv-app — base `v0.4.7`.

A interface mantém os menus, o adaptador de aplicativos, as animações de foco, a configuração de aparência, os atalhos e os módulos do projeto de origem. A composição acrescenta data e hora, previsão do tempo, controles de música e Continuar assistindo. A fileira de apps usa blocos horizontais arredondados com as cores dos aplicativos. Os widgets têm transparência ajustável.

## Dados dos widgets

O histórico combina sessões de mídia autorizadas e a lista pública Watch Next do Android TV, mediante permissão. Títulos, imagens, progresso e links dependem dos dados compartilhados pelo aplicativo de streaming. A Launcherin não acessa a conta do streaming nem inventa episódios assistidos. Os dados são guardados no armazenamento privado do aplicativo.

Na preview4, a abertura preserva o intent completo publicado pelo streaming, incluindo ação, componente, tipo, categorias e parâmetros tipados de conteúdo/reprodução. Aceita tanto URLs simples quanto `Intent.toUri(0)`, `intent:` e `android-app:`, inclusive intents sem URL que identificam o episódio nos extras. O pacote e o componente precisam pertencer ao publicador; seletores, permissões de URI e flags de tarefa não necessárias são removidos. A launcher não inventa parâmetros específicos de Netflix/Prime/Disney+/Max nem força um seek sobre outra sessão.

Quando não há um link utilizável, a sessão ativa só é aberta se o identificador de mídia coincidir (ou, para registros sem identificador, se o título coincidir). Sem correspondência, abre o aplicativo e avisa que a retomada direta não está disponível para aquele item. A posição efetivamente retomada é controlada pelo streaming. O diagnóstico local guarda apenas o nome do provedor e a rota da última abertura, sem URLs, títulos ou identificadores do conteúdo. A equivalência com a Monet nos quatro serviços ainda precisa de validação na TCL física.

A previsão usa Open-Meteo, com pesquisa de cidade, cache e atualização limitada ao período em que a launcher está visível. Não requer GPS. O nome da cidade é enviado ao serviço de previsão. Créditos: https://open-meteo.com/ — CC BY 4.0.

Ao clicar na previsão, um card ancorado mostra os próximos cinco dias, com mínima, máxima e probabilidade de chuva. O cenário usa a condição atual e o nascer/pôr do sol no fuso da cidade: sol, estrelas, nuvens em movimento, chuva, neblina ou neve. A animação fica restrita ao card e para ao fechá-lo ou sair da launcher; respeita a redução de movimentos e a desativação de animações. Não há vídeo nem animação de fundo em serviço.

O quarto widget é **Seus times**. A chave `sports_favorite_teams` contém até dois identificadores escolhidos no mesmo catálogo da TV e do celular. Ao abrir, mostra até dois próximos jogos e o último resultado encerrado de cada time, considerando todas as competições retornadas pelo provedor. A lista inicial contém clubes das séries A/B brasileiras e de ligas europeias. Dados públicos de agenda e placares: ESPN. O acesso não requer conta nem chave de API; a disponibilidade depende do serviço. São feitas consultas com cache por time, somente com a launcher visível, sem serviço de segundo plano. Jogos cancelados/adiados não são apresentados como resultados e placares ausentes não viram 0 a 0.

As duas expansões mantêm as demais fileiras e o papel de parede visíveis. Voltar ou Fechar recolhe o card e devolve o foco ao widget. Não alteram o funcionamento do auxiliar Home.

No widget recolhido, os favoritos aparecem por seus escudos, preservando cores e proporções. No expandido, o cabeçalho de cada time combina escudo e nome. Apenas os escudos escolhidos são baixados do CDN público da ESPN, com tamanho/decodificação limitados e cache local; não se baixa o catálogo inteiro de imagens. Sem imagem disponível, um símbolo neutro de futebol ocupa o lugar até uma nova consulta, sem substituir o escudo por nome.

Imagens de demonstração e registros de exemplo existem somente no build debug. O APK release não inclui esse histórico de exemplo.

## Temas decorativos

A escolha `seasonal_theme` aceita `none` (Padrão) ou `christmas` (Natal). Padrão é a opção inicial. Natal acrescenta pequenos gorrinhos, luzinhas, bonecos de neve e presentes presos às bordas de widgets, cards de Continuar assistindo e apps. A posição e a escala acompanham os elementos reais durante foco e rolagem; os apps e enfeites permanecem recortados dentro do dock. O overlay não recebe toque nem foco e redesenha somente quando a geometria muda. Não há animação de tela inteira, vídeo, blur ou download de enfeites.

O tema é independente do papel de parede e da transparência. Pode ser escolhido nas configurações da launcher na TV ou no editor Avançado do app Android. Padrão remove os enfeites. As fotos pessoais das gatas usadas nas prévias não fazem parte dos APKs; cada pessoa escolhe a foto pelo app.

## Desenvolvimento

O namespace Java fica em `launcher/app/src/main/java/app/launcherin/tv`. Para compilar e verificar:

```sh
cd launcher
bash gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

Use JDK 17 ou mais recente, SDK 36 e build-tools 35.0.0. O fork possui acesso à internet para imagens de mídia e previsão; essa é uma diferença intencional em relação à launcher original.

Os testes de regressão do parser precisam de um Android real ou emulado, pois o `android.jar` da JVM contém stubs. Compile `assembleDebug assembleDebugAndroidTest`, instale os dois APKs no dispositivo de desenvolvimento e execute `adb shell am instrument -w app.launcherin.tv.test/app.launcherin.tv.PlaybackInstrumentation`. A instrumentação é exclusiva do APK de testes; não inclui apps reais de streaming e não comprova retomada em contas de produção.

A chave local é mantida em `.keys/` e não entra no controle de versão. Mantenha a mesma chave e o mesmo identificador para atualizar a instalação. O telefone verifica hash, tamanho e certificado do APK da TV antes de incluí-lo no instalador. Depois de alterar o APK da TV, execute `scripts/bundle-launcherin-tv.py` no projeto do celular, antes de recompilar o celular.

## Home no Google TV

A alternativa preservada do projeto original é o auxiliar do botão Home. Ele mantém o Google TV ativo e redireciona a tela inicial para a Launcherin. O modo padrão não filtra teclas, preservando a repetição de volume. Esse redirecionamento pode permitir uma breve aparição da tela original; não há garantia de eliminar o efeito em todo firmware TCL.

Os scripts de proteção, as verificações de origem dos módulos e as confirmações de instalação do projeto original permanecem presentes. Nenhuma alteração foi aplicada à TV física durante o desenvolvimento deste fork.


## Ajustes da preview5

Os apps usam uma geometria única durante criação, associação e anexação ao dock. Uma medida ainda indisponível usa o mesmo tamanho proporcional de referência; a anexação corrige a medida real antes da exibição. Os banners têm somente margens externas transparentes recortadas, mantendo cores, proporção e formato de cápsula. O foco continua dentro dos limites da barra.

O foco de Continuar assistindo cresceu mais 7% em relação à preview4: escala final `1.035 × 1.07 = 1.10745`, com contorno branco de 2dp desenhado acima da imagem e elevação de desenho. A área de rolagem reserva espaço lateral para esse realce. O tratamento dos intents completos da preview4 foi preservado.

O atalho de TV aberta aceita somente os pacotes nativos conhecidos e instalados como apps do sistema. Tenta o URI de canais restrito ao pacote e depois a entrada do próprio app de TV; não usa um resolvedor implícito nem aceita Globoplay como sintonizador. Na tela da Launcherin, reconhece a tecla TV/terrestre e as teclas de configurações padrão/TCL 83, 4020 e 4077, além das configuradas no perfil. Não habilita o filtro global de teclas. Fora da launcher, o tratamento continua a cargo do firmware ou dos auxiliares que já estavam configurados.

Continuar ouvindo mostra faixa, artista e capa, com fonte/estado de acesso explícitos quando ainda não há conteúdo. Aceita capas de música a partir de 48px e mantém a regra anterior de imagens de vídeo. Atualiza as sessões ao voltar para Home e pede religação do listener autorizado quando necessário. Títulos/artistas ausentes podem vir da notificação de mídia dos apps de música reconhecidos, somente quando o token pertence ao mesmo pacote; notificações comuns e conteúdo de contas não são lidos. Dados são locais. YouTube TV só é tratado como música com indicação positiva (URI music.youtube.com ou álbum e artista), preservando vídeos comuns em Continuar assistindo.

Os controles e o card usam a mesma sessão preferida: reprodução antes de pausa e eventos reais de reprodução para desempatar. Não usam o horário retornado por getPlaybackState() para ordenar sessões, pois o Android pode atualizá-lo ao estimar a posição. Uma sessão pausada não substitui a música ativa; novas interações e retorno ao outro reprodutor são acompanhados. Sem sessão ativa, o conteúdo anterior fica no cache e o app pode ser aberto novamente.

A instrumentação com `-e dashboard true` requer [fixtures isoladas de música/handler IPTV](launcher/app/src/androidTest/fixtures/music/README.md) e acesso de mídia no emulador. As fixtures não são Spotify/YouTube Music/Globoplay reais e não fazem parte dos APKs distribuídos. Botões físicos e integração com os apps oficiais precisam de novo teste na TCL.


## Ajustes da preview6

TV15/preview6 compartilha uma única resolução entre a opção **TV ao vivo** do menu de entradas e o ícone/tecla TV. Seleciona o mesmo tuner do sistema e envia o URI de fonte com o MIME de canal correspondente; não usa mais o URI genérico sem tipo que o app TCL podia tratar como hub de streaming. Perfis com tuner específico e root-channel explícito mantêm sua semântica anterior.

O Play da música permanece na launcher. Atualiza as sessões antes do clique, usa PLAY/PAUSE, MEDIA_PLAY_PAUSE ou PLAY_FROM_MEDIA_ID conforme os controles disponíveis e tenta uma tecla explícita de mídia se o comando direto for ignorado. Sem sessão, tenta o MediaBrowserService público do mesmo publicador e depois seu receptor MEDIA_BUTTON exportado, respeitando permissões e sem lançar atividade do cliente. Não envia comandos globais nem usa busca de música por título. O YouTube compartilhado exige identificação da faixa para não controlar um vídeo comum. Uma geração de comando cancela tentativas anteriores ao sair da launcher ou clicar novamente. A interface só indica reprodução quando o publicador informa esse estado; se não houver resposta, informa o erro e conserva a música no widget. Isso depende dos APIs e restrições do cliente oficial, cuja integração final precisa ser conferida na TCL.

Continuar assistindo contém no máximo dez itens, excluindo YouTube tanto do cache antigo quanto dos programas nativos e das capturas de vídeo. O YouTube Music do widget usa um armazenamento separado. Segurar OK ou pressionar Menu sobre um card abre **Remover da lista**. A remoção é local, persistida, não altera a conta do streaming e resiste a uma reimportação igual ou atualização de metadados pausados. Novo início real de reprodução ou engajamento mais recente informado pelo provedor permite que o conteúdo reapareça. A UI atualiza imediatamente e mantém foco em um card vizinho.

Os testes Android incluem o despacho de D-pad pela Activity até o botão real do widget, sessões que só aceitam Play/Pause por tecla, comando direto ignorado, fila parada por ID, sessão encerrada recuperada via browser e via receptor, e remoção pela UI com cache antigo de mais de dez itens/YouTube. As fixtures são reprodutores de teste, sem acesso aos apps oficiais ou contas.

## Preview7: TV aberta, avisos de gols e inatividade

- A tecla de TV usa a mesma rota nativa confirmada no ícone/entradas. Adicionado o código TCL 4001 documentado em `examples/tcl-65c8l-google-tv/README.md` e GUIDE 172 nas TCL. Como o código desta P7K não foi medido, **Configurações da Launcherin → TV aberta, gols e inatividade → Aprender tecla de TV aberta** permite gravar o KeyEvent real. Não confundir TV abaixo do Home com TCL Channels ou Entrada.
- Aprender a tecla não habilita filtragem global. A opção separada **Tecla de TV em outros apps** exige consentimento na TV e o HomeKeyService ativo; avisa sobre a perda de repetição do volume reportada nas TCL. Desativá-la mantém as teclas fora da launcher com o firmware.
- **Avisos de gols** usa os até dois favoritos já escolhidos. O cartão aparece no canto superior direito durante 7 s, sem receber foco/toques, inclusive sobre outros apps que permitam sobreposição. Requer o acesso de mídia já usado no widget e a permissão de exibir sobre outros apps. A opção começa desativada.
- Consultas públicas ESPN: 30 s durante jogo, 60 s próximo do início/erro, 15 min longe de jogos; sem consulta com tela apagada ou avisos desligados. A primeira observação, placares ausentes e reconexões longas não anunciam gols antigos. A comparação de placares usa o maior valor observado para evitar repetição após VAR; não detecta novamente um gol que só recompõe esse mesmo placar. Pode haver atraso ou indisponibilidade do provedor.
- **Protetor após** e **Desligar após** são minutos contados desde a última atividade, em todos os apps. 0 desativa; o desligamento precisa ocorrer depois do protetor. Usa `screen_off_timeout`, `sleep_timeout` e `attentive_timeout`, sem capturar cada tecla globalmente. Apps que mantêm a tela acordada podem suspender a contagem. Desligar significa standby.
- Primeira configuração dos tempos: conceder WRITE_SECURE_SETTINGS por ADB e WRITE_SETTINGS por acesso especial/appops. `Habilitar-Temporizadores.ps1` na entrega faz apenas essas concessões; `-PermitirAvisos` também autoriza a sobreposição. Desativar os dois tempos restaura as configurações anteriores ainda pertencentes à Launcherin, preservando alterações externas posteriores.
- Google Fotos usa o modo ambiente **nativo**: em Sistema → Modo ambiente → Google Fotos, selecionar/autorizar o álbum na conta. O atalho da Launcherin abre as configurações públicas de protetor. Não há scraping, download de álbum privado, senha nem OAuth Google incorporado na launcher/celular. Selecionar o álbum permanece no fluxo do Google na TV/Google Home.
- O app Android em português salva/envia avisos e os dois tempos pelo transporte de aparência original. As permissões iniciais e a seleção do álbum precisam ser feitas na TV/ADB; a entrega não as autoriza silenciosamente.

Referências: [Google TV: protetor/álbum](https://support.google.com/googletv/answer/10070821?hl=pt-BR), [Android 14: timers nativos](https://android.googlesource.com/platform/frameworks/base/+/android-14.0.0_r1/services/core/java/com/android/server/power/PowerManagerService.java).

As escolhas de gols e tempos permanecem na TV ao receber uma alteração de aparência que não inclua essas chaves. Enviar 0 explicitamente desativa a opção. Redefinir só a aparência no celular também preserva esses controles.

## Preview8 — candidato para validar a TCL

O usuário confirmou: o álbum Google Fotos já está selecionado, mas a launcher permanece visível até o desligamento; a música funciona enquanto há sessão e falha após parar/fechar os dois apps. Nenhum diagnóstico da TCL está disponível neste momento; o usuário poderá testar à tarde.

- A inatividade da Home agora aciona o atalho público SystemUI/Somnambulator para iniciar o DreamService escolhido no Android. Há **Visualizar protetor agora** em TV aberta, gols e inatividade. A seleção do álbum permanece no Google TV; nenhum álbum/foto foi incorporado ou baixado pela launcher.
- A entrada manual no Dream reinicia a atividade do PowerManager (comportamento AOSP Android 14). O auxiliar Home, quando ativo, preserva o prazo original de standby durante esse Dream por GLOBAL_ACTION_LOCK_SCREEN. As configurações nativas de desligamento que já funcionavam não foram alteradas. Sem auxiliar ativo, continua valendo o temporizador nativo, que pode contar novamente desde a entrada no Dream. Fora da Home, o protetor continua dependendo da política nativa da TCL e precisa ser verificado no aparelho.
- Play e Pausa agora obedecem ao botão que o usuário viu; BUFFERING/CONNECTING não transforma Play em Pausa. Retomadas em conexão não são encerradas tão cedo. A conexão de MediaBrowser permanece enquanto o controle está sendo usado.
- Em Android 13+, a retomada consulta getMediaKeyEventSessionPackageName: o Play do sistema só é enviado quando o receptor mantido pelo Android pertence ao mesmo app dedicado de música. Sem correspondência, não há disparo global. A permissão é o acesso de mídia existente. Não foi acrescentado MEDIA_CONTENT_CONTROL nem acesso privado.
- Android 12+: outro caminho usa a ação Play da notificação de mídia, criada pelo próprio app e pelo mesmo UID, somente para serviço/broadcast. PendingIntents de Activity são recusados. Os controles são mantidos apenas em memória e não são serializados. Em versões anteriores, conserva browser/receptor explícito.
- YouTube compartilhado com vídeos não usa retomada baseada somente no pacote: poderia abrir um vídeo comum. Continua exigindo identificação positiva da música via sessão/browser e ID. O diagnóstico da TCL deverá mostrar qual pacote o YouTube Music realmente usa.
- Segurar OK nos botões do widget abre o diagnóstico; também está disponível em TV aberta, gols e inatividade. Registra rota e estado, sem título/álbum/conta/URL. O script Diagnosticar-Musica-Protetor.ps1 é somente leitura.

As novas rotas Android 12/13+ ainda precisam de confirmação na TCL Android 14 com Spotify/YouTube Music reais. Os testes Android disponíveis usam API 28 e clientes sintéticos; não comprovam áudio dos serviços oficiais. Esta versão é um candidato de teste, não uma confirmação de compatibilidade dos dois aplicativos fechados.

Referências: https://developer.android.com/reference/android/media/session/MediaSessionManager#getMediaKeyEventSessionPackageName() ; https://developer.android.com/reference/android/app/PendingIntent#isActivity() ; https://android.googlesource.com/platform/frameworks/base/+/android-14.0.0_r1/services/core/java/com/android/server/dreams/DreamManagerService.java


## Preview10 — widgets e Continuar assistindo abaixo dos apps

TV18 e companion10 compartilham a versão `0.1.0-preview10`. O companion embute exatamente o APK TV18 verificado; mesma assinatura das entregas anteriores. Atualização inicial de ambos é necessária; compromissos posteriores não reinstalam/reconfiguram a TV.

- Grade de 12 colunas por duas linhas, com visibilidade, tamanho P/M/G, arraste e botões de posição no celular. Colisões são recusadas no editor; payloads malformados são limitados na TV. Ocultar remove o card, sem placeholder, e os vizinhos de foco são recalculados. Os quatro widgets existentes ficam ativos por padrão; novos widgets começam ocultos.
- Agenda manual na Launcherin, até40 eventos com data e horário opcional. Documento próprio `agenda.json`, guardado localmente no celular e enviado pela sessão pareada após o desbloqueio original. Alterações offline persistem. O transporte envia um arquivo pendente, usa `ThemeProvider.call(commitAgenda)` com autorização explícita de WRITE_SECURE_SETTINGS, faz commit atômico e verifica SHA256. Não chama PlanRequest/ApplyTheme, não muda config.json e não inicia/reinicia Home. Um teste cobre edição enquanto o envio anterior termina sua última leitura.
- Porta-retratos com até10 fotos escolhidas pelo Photo Picker, reencodificadas em JPEG limitado, até1600px/280kB. Sem permissão ampla de arquivos. O plano envia fotos imutáveis antes do config; a TV usa cópias locais e troca a cada15s. Não lê biblioteca privada do Google Fotos nem altera o protetor nativo.
- RAM global real via ActivityManager. CPU global quando /proc/stat permite; caso contrário CPU do próprio processo, com rótulo explícito. Atualização em5s somente com card visível e Home ativa.
- `watch_next_mode`: `above`, `below`, `hidden`. Em abaixo, Down dos apps revela o histórico e move a linha de apps junto; Up da lista/Back recolhe. A camada cresce para comportar os filhos revelados, evitando clipping. Apps permanecem visíveis. Oculto remove título e lista e evita suas consultas. Chaves ausentes mantêm a apresentação anterior.
- Nenhuma nova permissão Android; prioridade HOME, Google TV, filtro global de teclas e temporizadores de standby não foram alterados. Sem claims de desempenho ou compatibilidade física. Música após fechar apps e Google Fotos em inatividade continuam pendentes na TCL.

Evidências: `/workspace/output/launcherin/validation-preview10.json`, hashes, APKs e imagens `preview10/`. Imagens da TV são do emulador API28 com dados sintéticos; telas do celular renderizadas em Robolectric API35. Sem acesso à TCL física, commit, push ou publicação externa.

Validação concluída da preview10: 165 testes JUnit e5 verificações Android de widgets/navegação, sem falhas. Recursos pt-BR1494/0 problemas; estabilidade Compose e limite de APK passaram. Lint TV/launcher/ui:0 erros; respectivamente135/8/11 avisos. Assinaturas, manifestos e permissões conferidos, sem novas permissões, phone não depurável e apenas1 componente exportado. SHA do TV embarcado idêntico ao separado. Celular:7929938bytes, SHA256`37901199e4592d891977beeb625f24ed5da80b1dd9997a9d062a2b485d7381aa`; TV:724147bytes, SHA256`aab1444a2cc5a28f6db6e2335d6c223038f2a67683f06d1d81075757ebce8dbc`. Instaladores e capturas em `/workspace/output/launcherin`; evidências do companion em `work/preview10/`.


## Preview11 — protetor nativo no Android14 e proteção do standby durante música

O usuário validou na TCL que Google Fotos aparece usando o temporizador nativo. O antigo SystemUI.Somnambulator não executa dream() no AOSP Android14: apenas finish(). A versão anterior confundia abertura da atividade com início do Dream. A Home agora não agenda esse atalho no Android14; usa o temporizador nativo. A configuração/teste do celular abre android.settings.DREAM_SETTINGS, com rótulo explícito. A rota legada permanece em API33 e anteriores.

O usuário escolheu protetor5min e standby10min, permitindo fotos durante música mas exigindo não desligar enquanto ela toca. O listener de mídia passa a manter uma proteção apenas sobre sleep_timeout: -1 durante música reconhecida (incluindo buffering/conexão) e por um prazo novo completo depois da pausa. screen_off_timeout e o álbum permanecem independentes. Ao expirar o prazo, restaura o tempo configurado e o temporizador nativo também considera entradas recentes do controle. Metadata/broadcasts não reescrevem config.json nem reiniciam a Home. Não foi acrescentada permissão Android, serviço novo ou wake lock de tela.

A proteção é persistida com o contador de boot; não usa um prazo antigo após reiniciar. Se a sessão não puder ser consultada, mantém o desligamento suspenso até obter um estado confiável. Acesso a mídias e permissões de temporizadores são necessários. Há observação do timeout enquanto protegido, reconciliação por callbacks e verificação periódica durante reprodução; sem proteção ativa, alterações externas permanecem preservadas. O deadline legado não força lockScreen durante música/período de proteção. O diagnóstico informa a fase e o prazo após pausar.

Os testes novos cobrem política e SettingsProvider/SharedPreferences Android sob Robolectric/API35, fotos independentes da proteção, prazo após música longa, retomada, perda de acesso, restauração, alterações externas, reinício e arquivo de aparência intacto. Os goldens do companion foram portados por substituição explícita dos identificadores antigos com.gathin.tvhome para app.launcherin.tv, incluindo o hash do plano já adaptado ao fork; sem mudança nos algoritmos de shell/watchdog. As verificações completas passaram após a correção dessas referências antigas.

A proteção nova ainda precisa de teste físico na TCL após instalar TV19. O teste de protetor confirmado pelo usuário foi do mecanismo nativo com a instalação anterior e não valida o código novo. YouTube Music nesta TCL usa a sessão compartilhada do YouTube e encerra ao sair; nenhum MediaBrowserService/receptor MEDIA_BUTTON foi encontrado. Spotify funcionou no teste do widget. Essa limitação do YouTube Music não foi prometida como corrigida.

## Preview12 — tecla de configurações TCL

A tecla de configurações usa o painel lateral nativo nas TCL compatíveis: serviço de sistema `com.android.tv.settings/com.tcl.settings.ShowWindowService`, ação `com.tcl.settings.SHOW_WINDOW`, extras `Type=Settings` e `Catagory=picture`. A chamada foi confirmada na TV real; `Type=quicksettings` abriu uma barra inferior diferente e não é usado. Nenhum APK da TCL é incluído ou redistribuído.

Em Configurações da Launcherin → TV aberta, gols e inatividade, ative **Tecla de configurações em outros apps** e mantenha o auxiliar de acessibilidade autorizado. A opção é independente da tecla de TV e inicia desativada. O menu também oferece **Testar painel de configurações da TV**. As configurações completas continuam disponíveis pelos seus atalhos. A escolha explícita pelo painel da própria Launcherin (`settings_key_panel`) é respeitada.

O auxiliar abre o painel ao soltar a tecla, ignora cancelamentos e não leva a Home para a frente. O código TCL83 é tratado globalmente com essa opção; botões TCL4020, volume e Home não ganham novos mapeamentos. A leitura global pode afetar a repetição do volume em algumas TCL; para desligá-la, desative ambas as opções de teclas em outros apps quando não houver outro filtro configurado. Se o serviço nativo não estiver disponível ou negar acesso à Launcherin, usa-se o caminho anterior de configurações.

Inclui também as correções da preview11 para protetor nativo Android14 e proteção do desligamento durante música. Fotos podem aparecer enquanto toca música; apenas o standby fica suspenso. O APK da TV e o teste da tecla física ainda precisam ser confirmados após instalar esta candidata.

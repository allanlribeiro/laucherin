# Launcherin para Android

Fork independente do aplicativo completo Clean Your TV App v0.4.7, baseado no commit `2aaf02228b4f130f1853ae5ba054eb90a66ab3fe`. A licença MIT e os créditos originais foram mantidos. A launcher correspondente está em `../launcherin`, baseada no CleanYourTV 3.3.0.

O identificador de instalação é `app.launcherin.mobile`. Os namespaces Kotlin originais foram preservados. O app requer Android 9 ou mais recente.

## O que foi adaptado

- Interface em português brasileiro, com 1.332 recursos traduzidos e verificação dos argumentos ICU/printf.
- APK da Launcherin TV incluído no instalador, mantendo as verificações de hash, tamanho, versão e certificado.
- Editor original de aparência preservado, com controles adicionais de transparência dos widgets e cidade da previsão do tempo.
- Escolha de até dois times favoritos com busca por nome. O mesmo catálogo de clubes é usado na TV e no celular. A seleção viaja em `sports_favorite_teams` pelo transporte original de configuração, incluindo remoção explícita e desfazer antes de enviar.
- Tema decorativo Padrão/Natal nos controles avançados de aparência. A escolha `seasonal_theme` é enviada pelo mesmo transporte original e sobrevive ao salvamento, reabertura e troca de estilo. O tema preserva o papel de parede e as escolhas dos outros widgets; Padrão retira os enfeites.
- Auxiliar original do botão Home disponível por escolha explícita do usuário. Essa opção mantém a launcher original ativa. Pode haver uma breve aparição dela antes do redirecionamento, dependendo do firmware.
- Descoberta, pareamento ADB, análise, limpeza, backups, restauração, fotos e módulos do app original preservados.
- Atualizações do aplicativo original não são oferecidas por este fork. As verificações de assinatura dos módulos continuam presentes.

O APK auxiliar de progresso (`tv.cleanyour.curtain`) continua sendo o artefato original, com certificado e hash próprios, verificados separadamente. Não é uma recompilação da Launcherin.

## Compilação e verificação

Use JDK 21 e o SDK Android 37.0. Mantenha a mesma chave local em `.keys/` para futuras atualizações; a chave não faz parte do Git.

Após compilar a launcher TV, atualize o artefato incluído:

```sh
python3 scripts/bundle-launcherin-tv.py
python3 scripts/check-launcherin-portuguese.py
```

O empacotador recusa uma troca de certificado ou redução do código de versão. Os testes de integridade continuam no módulo `core:trust` e na etapa anterior à compilação Android.

Para gerar o APK, configure `CYT_RELEASE_KEYSTORE`, `CYT_RELEASE_KEY_ALIAS`, `CYT_RELEASE_KEYSTORE_PASSWORD` e `CYT_RELEASE_KEY_PASSWORD`, então execute:

```sh
bash gradlew :app:assembleFullRelease :core:trust:test
```

A configuração de proxy e a localização dos diretórios de compilação dependem do ambiente de desenvolvimento. Não distribua chaves privadas junto com os APKs.

## Estado de validação

Versão `0.1.0-preview6`, destinada a testes. Inclui o APK TV de código 15, com preservação dos intents completos de conteúdo, padronização dos apps, foco ampliado, atalhos de TV/configurações na tela inicial e informações de música visíveis. O Play da música usa APIs de segundo plano, mantendo a launcher visível. Continuar assistindo limita dez itens, permite remoção local e exclui YouTube. A inicialização em português foi conferida em emulador Android na preview3; o fluxo visual do celular não mudou na preview4. Pareamento, aplicação das escolhas e retomada dos streamings na TCL física ainda precisam ser validados. O histórico depende das sessões de mídia e dos dados Watch Next compartilhados pelos apps; não há acesso às contas dos serviços de streaming. O widget de esportes consulta a agenda pública da ESPN na TV; o app do celular envia apenas a escolha dos times.

Os links e instruções restantes no README abaixo pertencem ao projeto de origem. Não instale o APK de origem esperando atualizar esta versão da Launcherin.

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

## Companion preview9 — navegação por assunto

Somente o celular avança para código 9 / `0.1.0-preview9`; o APK embarcado continua sendo TV17 / preview8, com a mesma assinatura e pin. Não é preciso atualizar a TV para reorganizar o celular.

O destino inicial de gerenciamento agora abre o hub com quatro abas: Início, Personalizar, Widgets e TV. Busca sem diferenciação de maiúsculas e acentos encontra cidade, times/gols, transparência, álbum, temporizadores e ferramentas. O gerenciamento original continua acessível em Ferramentas avançadas, incluindo conexão, limpeza, relatórios e restauração. Seleção de TV e credenciais continuam no transporte original.

Os editores por assunto reutilizam LookDrafts e o envio tipado existente. Trocar abas ou fechar um editor mantém o rascunho; sair, trocar a TV ou abrir operações independentes pede para manter ou descartar alterações pendentes. Aplicar na TV é fixo quando há alterações. Envios revertidos, abandonados ou parcialmente concluídos não confirmam sucesso nem substituem a configuração salva; o rascunho continua disponível. Ao voltar de outra tela, os dados são recarregados sem apagar as alterações do editor.

Testar protetor agora abre somente o Somnambulator do Android; Ver na TV abre somente a atividade da Launcherin. Não alteram timers, não forçam parada de apps e não simulam teclas. Exigem conexão existente, não aceitam argumentos de shell da interface e não executam com alterações pendentes. A resposta confirma o pedido de abertura, não o funcionamento físico de um álbum ou de um streaming. Google Fotos permanece configurado no modo ambiente nativo/Google Home, sem leitura da biblioteca privada pelo companion.

Spotify e YouTube Music, bem como a entrada automática do protetor na TCL, continuam pendentes do teste físico de preview8. Este trabalho não modifica o APK da TV nem declara esses problemas resolvidos.

A interface permanece em português. O verificador de recursos agora inclui todos os arquivos de strings, inclusive as telas novas e a prévia da televisão. BackHandler usa a mesma dependência AndroidX Activity já presente no app; quatro checksums transitivos antes ausentes foram conferidos contra o Google Maven oficial, sem desligar a verificação de dependências.

Validação final da preview9: 99 testes passaram (flows83, navegação6, busca3, interface7), incluindo renderização nativa Robolectric/API35 e fonte200%. Contratos pt-BR:1431/0 problemas. Estabilidade Compose e limite de APK passaram. Lint dos módulos launcher/manage:0 erros (28 avisos, em sua maioria tipografia; também dimensão do painel e plural de contagem). O lint global do app foi interrompido por pressão de memória; os módulos alterados foram verificados separadamente. Release compilado com um worker, assinatura verificada e permissões iguais às da preview8; apenas uma atividade exportada. TV17 embarcada idêntica ao APK entregue anteriormente. Sem instalação física, commit ou publicação.

Saída: `/workspace/output/launcherin/Launcherin-Android-0.1.0-preview9.apk` (7846230bytes), SHA256 `c0cbf74efe6b651fd28a42f31f1ea6ba2bd30c11328dbdfb6cd28958e9d7b263`. Imagens das quatro abas e fonte200% em `mobile-preview9/`; notas, hashes e `validation-preview9.json` ao lado do APK. Evidências locais em `work/preview9/`.


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

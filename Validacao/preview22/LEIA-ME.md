# Launcherin Música — preview22 (teste)

A apresentação acompanha a nova referência: menu lateral sem a marca no canto,
player principal com capa circular, abas de conteúdo, histórico, mixes, gêneros e
player inferior persistente. A Home, seus widgets, apps, Continuar assistindo,
atal/temas, atalhos, Home/acessibilidade e configurações rápidas não são redesenhados.

## Instalar

- TV: `Launcherin-TV-0.1.0-preview22.apk`, pacote `app.launcherin.tv`, código30.
- Celular: `Launcherin-Mobile-0.1.0-preview22.apk`, pacote `app.launcherin.mobile`, código22.

Os dois usam o certificado já existente. Instale por cima, sem desinstalar ou apagar
os dados. Para esta candidata, **instale o APK da TV diretamente**: o instalador
embutido no Mobile continua sendo TV preview18. O limite existente de8 MiB do
celular foi preservado; o player maior não foi incluído nesse instalador antigo.
O Mobile22 configura a TV22 depois da instalação direta.

Na Home, segure OK em um controle do widget Continuar ouvindo ou selecione seu título
para abrir a música. Instalar apenas o celular não redesenha nem atualiza automaticamente
esta tela na TV.

## Perfis locais

O botão circular do cabeçalho abre **Perfil1** e **Perfil2**, com opção de renomear
pelo teclado próprio e adicionar mais perfis (até8). Renomeie para vocês dois.

O primeiro perfil usa a biblioteca antiga: músicas, artistas/bandas, fila, posição
e preferências anteriores continuam nele. Os demais começam com biblioteca própria.
Favoritos, artistas/bandas, histórico, playlists, fonte preferida, aleatório,
repetição e opções do visualizador ficam separados por perfil.

Trocar de perfil pausa o player próprio, salva a fila/posição anterior e carrega o
perfil escolhido. Não inicia outra música sozinho. São perfis locais da Launcherin,
não contas ou logins de YouTube Music, SoundCloud ou Spotify. Não mudam contas de
outros aplicativos nem o perfil da Home inteira.

## Teclado de PC

A busca e os nomes usam um teclado QWERTY próprio, dentro da música. Números ficam
na fileira superior; QWERTYUIOP, ASDFGHJKLÇ e ZXCVBNM seguem as posições usuais,
com Shift, Enter/Buscar à direita, Apagar e Espaço largo.

Setas movem o foco e OK digita. **Segure OK em uma letra** para escolher seus
acentos, por exemplo A→á/à/â/ã. Não existe uma fileira de letras acentuadas. Shift
alterna maiúsculas. O teclado Android/IME do sistema não é instalado, trocado nem
desativado globalmente; somente os campos da música evitam abrir o teclado padrão.

## Menus e controles

- **Início:** Tocando agora, histórico real do perfil, mixes locais e gêneros.
- **Buscar:** tipos Músicas, Artistas, Álbuns e Playlists no catálogo público selecionado.
- **Sua Biblioteca / Favoritos:** músicas, artistas e bandas já salvos; os filtros anteriores continuam disponíveis.
- **Playlists:** criar playlist local, adicionar músicas pelas opções de uma faixa,
  consultar, reproduzir e remover uma playlist. Não altera playlists da conta do serviço.
- **Rádios:** seleções da Launcherin por artista, construídas pela busca pública;
  não são estações ao vivo nem a rádio personalizada oficial de uma conta.
- **Explorar:** buscas por gêneros musicais.
- **Ajustes:** existe apenas no menu lateral. A engrenagem duplicada foi removida.

As abas são Tocando agora, Letra, Artista, Álbum, Fila e Relacionadas. As letras usam
o mesmo LRCLIB, cache, correspondência por música/artista/duração e sincronização
anterior. Artista oferece busca, favorito e álbuns; Fila mantém suas ações. Relacionadas
mostra os itens efetivamente compartilhados pela fonte, ou avisa a ausência.

Play/Pausa, anterior, próxima, aleatório, repetição e favorito funcionam no player
principal. O player inferior continua acessível durante a navegação e oferece
controles, progresso, letra, protetor e volume real do fluxo de mídia da TV.
Aleatório visita os itens restantes sem repetir dentro do ciclo; repetição alterna
Desativada / Uma música / Toda a fila. A fila salva não é embaralhada destrutivamente.

A notificação do cabeçalho mostra avisos locais do player. Não representa notificações
privadas de uma conta musical. Álbum/ano são exibidos somente quando os metadados
existem; ano de upload não é usado como ano do álbum. O catálogo atual normalmente
não informa ano. Coleções do SoundCloud não são rotuladas como álbuns confirmados.
Não há indicadores inventados de FLAC, Dolby, Lossless ou conta conectada.

## Fotos de artistas e bandas

A aplicação consulta a busca específica de artistas da fonte e utiliza uma foto
somente quando encontra uma correspondência única. O cadastro manual não fica
limitado às iniciais. A imagem é guardada nos favoritos e reutiliza o cache limitado.
Também há consulta para o retrato do artista atual, sem favoritar automaticamente.

Se o nome for ambíguo ou a fonte não retornar foto, as iniciais permanecem como
alternativa. No artista/banda, segure OK ou abra suas opções e escolha **Escolher foto
do artista/banda**; confira a prévia antes de Usar foto. A atualização não reordena os
favoritos nem refaz a Home.

## Visualizador circular e acesso ao áudio

Ative em Música → **Ajustes → Visualizador circular** ou pelo Mobile22 em
**Widgets → Visualizador e proteção musical**. A opção começa desativada para
preservar o comportamento da instalação anterior.

Na TV, selecione **Permitir análise do áudio** e autorize a permissão de áudio.
A API Android Visualizer exige `RECORD_AUDIO` mesmo para o efeito ligado ao player
próprio. O código não cria AudioRecord, não grava o microfone e não captura o mix
global: liga o efeito somente a um identificador positivo da sessão MediaPlayer
própria. Não muda o Assistente ou o funcionamento de “Ok Google”.

O anel usa48 faixas logarítmicas calculadas a partir do FFT recebido, com ataque
suavizado. Não há amplitudes aleatórias ou batidas inventadas. Azul/ciano/violeta,
intensidade e movimento são configuráveis. O limite é até60 FPS; a cadência reduz
para30/15 quando há atrasos e respeita a redução de efeitos. O modo minimalista
reduz para4 FPS e captura em5 Hz. A captura e os callbacks são liberados na pausa,
saída ou ocultação da visualização.

A permissão ou o firmware/serviço podem impedir o acesso. Nesse caso o player continua
funcionando e informa a limitação. **Ambiente sem espectro** é opcional: anel/capa em
movimento lento, sem fingir uma reação à música. A compatibilidade real da captura
na TCL P7K ainda precisa ser confirmada. As capturas de demonstração do espectro
usam um FFT sintético conhecido de teste, não áudio capturado de um streaming real.

## Proteção inteligente e modos anteriores

Com visualizador e **Proteção inteligente** ativados, enquanto o player próprio toca:

| Inatividade | Resultado padrão |
|---|---|
|5 minutos | Diminui a intensidade da apresentação |
|15 minutos | Oculta a interface existente e conserva somente o visualizador móvel |
|30 minutos | Fundo preto, visual menor/discreto, menos barras e atualização de4 FPS |

Os conjuntos5/15/30,10/20/45 e15/30/60 podem ser escolhidos na TV e no celular.
Qualquer tecla restaura a interface. A primeira seta/OK apenas retorna, sem trocar
música acidentalmente; volume e teclas de mídia preservam suas funções. Pausar a
música encerra a apresentação automática. A tela de letras não é reconstruída nem
substituída pelas novas etapas.

Desative **Proteção inteligente** para continuar usando Capa flutuante, Fotos em
sequência, Modo escuro ou **Google Fotos**, mantendo o anel na capa. Selecionar um
desses modos nos Ajustes da TV também desativa a prioridade inteligente. Google
Fotos conserva o álbum e o temporizador nativos; não há novo acesso à conta privada.
Na TCL Android14, o álbum abre pelo tempo do modo ambiente, não por uma promessa de
início imediato via Somnambulator. Os modos locais anteriores mantêm suas opções.

O controle **Tela preta** mantém o áudio e não desliga a TV. **O painel LED continua
iluminado.** Não foi usada uma API privada/factory para desligar fisicamente só o
painel. Na Home e em outros apps, os protetores e regras anteriores continuam
valendo. A regra de não desligar durante música permanece preservada.

## Aplicação pelo celular

O Mobile lê o perfil ativo e suas opções pelo canal pareado existente, verifica o
hash do arquivo enviado e solicita uma gravação atômica protegida na TV. Não envia
comandos livres, não reabre a Home e não reenvia apps, widgets, wallpapers ou agenda.
Se o perfil na TV mudar durante a edição, a aplicação é recusada; use **Atualizar
perfil e opções da TV** para evitar aplicar uma configuração ao perfil errado.

## Validação e limites

Veja RESULTADOS.md e resultados.json distribuídos com os APKs para contagens,
hashes, certificado e testes. As renderizações são componentes Android reais em
Robolectric, com dados e artes de exemplo. As imagens de gêneros são ilustrações
vetoriais originais, não fotos de álbuns fictícios.

Testes locais não comprovam FPS, consumo de CPU/GPU/RAM, captura de áudio ou
instalação sobre a versão anterior na TCL física. Catálogo, disponibilidade de
músicas, imagens, álbuns/ano e relacionadas dependem da fonte. Os mixes são locais,
montados dos favoritos/histórico ou buscas de gêneros, não recomendações privadas
oficiais dos serviços. Não foi incorporado login de contas musicais.

## Compilar

TV: JDK21, SDK36, build-tools35.0.0, na pasta `launcher/`:

```bash
./gradlew --no-daemon --max-workers=1 -I tools/native-tests.gradle testReleaseUnitTest lintRelease assembleRelease
```

Mobile: siga o Gradle wrapper do projeto. Foram usados os testes de core:flows,
core:ops, core:trust e feature:launcher, lint de feature:launcher/app fullRelease,
assembleFullRelease e o gate de8 MiB. As dependências permanecem verificadas por
checksum; nenhuma dependência nova foi adicionada. A chave privada não é publicada.
A variante TV com NewPipe continua GPL-3.0-ou-posterior; avisos MIT da base preservados.

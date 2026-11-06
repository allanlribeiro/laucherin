# Launcherin · YouTube Music · protótipo Preview 15

**Estado: código experimental na branch feature/ytmusic-preview15, não é APK compilado.**

## Referência externa

O [Streamer Launcher v3.6.2](https://github.com/raviprakashji/Streamer-launcher-release/releases/tag/v3.6.2) descreve player próprio, Quick Mixes, letras sincronizadas, reprodução em segundo plano e indicador *Now Playing* na Home. Seu repositório público de *releases* contém documentação e APK; o fonte do motor usado nesses recursos não estava publicamente disponível na inspeção realizada. **Não foi incorporado nem decompilado código do Streamer.** A implementação abaixo usa somente APIs Android e componentes já existentes da Launcherin.

## O que foi implementado nesta primeira etapa

- Tela opcional **Launcherin · YouTube Music**, navegável com o controle remoto.
- Nome da faixa, artista, progresso temporal, capa (quando disponível no cache local ou em metadados HTTPS), Play/Pause, próxima faixa, anterior e avanço quando o aplicativo anuncia suporte.
- Seleção da sessão de mídia **específica do YouTube Music** (não envia teclas de mídia globais que poderiam controlar Netflix ou Spotify por engano).
- Acesso ao aplicativo instalado do YouTube Music; em dispositivos Google TV que usam o aplicativo YouTube como entrada musical, tenta o YouTube TV como alternativa.
- Não solicita login do Google, não armazena token do YouTube Music, não extrai nem redistribui fluxos de música.
- Reaproveita a permissão **Acesso às notificações** que a Launcherin já precisa para sua integração de mídias.
- Entrada no menu **TV aberta, gols e inatividade** → **YouTube Music · player da Launcherin**; também pode ser aberta mantendo **Play** pressionado no widget de música.
- Não adiciona serviços que rodam constantemente em segundo plano; a tela atualiza a cada 1,5 s somente enquanto aberta.
- Testes unitários de identificação dos pacotes e do cálculo de progresso.

## O que NÃO está implementado

- Reprodução interna do catálogo YouTube Music dentro da Launcherin (Streamer usa seu próprio motor).
- Pesquisa de catálogo e Quick Mixes nativos.
- Letras sincronizadas, playlists pessoais, autenticação da conta e cache/offline.
- Reprodução em background independente do aplicativo oficial. A reprodução em segundo plano depende do aplicativo e do firmware da TV.
- APK assinado da Preview 15 ou validação física na TCL P7K.

## Base e proteção contra regressões

Esta branch foi criada de **backup/pre-ytmusic-preview13-2026-11-06**, que está no commit `15bef143306ad94deef965ed2f4b1e4117ebbe9a`. **Não contém, necessariamente, todas as mudanças binárias da Preview 14** recebida como APK do celular. O APK da Preview 14 e o APK da TV embutido nele foram preservados fora desta branch. Não elevar `versionCode` nem publicar a Preview 15 até recuperar/reconciliar o código-fonte correspondente à Preview 14, compilar e testar.

## Roteiro mínimo de testes na TCL P7K

1. Verifique que a Preview 14 atual continua funcionando, com botão de Configurações e barra de apps sem regressão.
2. Depois de **reconciliar o fonte e compilar** a Preview 15, atualize por cima, sem desinstalar, com a mesma assinatura.
3. Autorize o acesso às notificações da Launcherin, se ainda não estiver ativo.
4. Abra o YouTube Music na TV, reproduza uma faixa e volte à Launcherin.
5. Abra o menu de player ou segure Play no widget. Verifique título, artista, capa e progresso.
6. Teste pausar, retomar e passar de faixa; um aplicativo sem suporte declarado deve manter seus controles desabilitados ou informar indisponibilidade, sem atingir outro app.
7. Abra Spotify e Netflix em seguida. Confirme que o painel YouTube Music não controla acidentalmente esses apps.
8. Observe navegação D-pad, uso de RAM, protetor de tela durante música e comportamento quando o app perde sua sessão.

**Critério para declarar integração concluída:** Preview 15 compilada da base correta, assinada, sem regressão de TV/celular e validada na TCL física. A presença do código nesta branch não equivale à publicação de APK.

# Validação — Launcherin Música preview22

Base: `32f99f9db28134d2638d2b13732eb36b38e5dd84` (TV preview21 e Mobile18).
Data:2026-11-12, referência de calendário America/Sao_Paulo.

## Resultado

**1390 testes passaram, sem falhas.**

| Escopo | Testes |
|---|---:|
|TV: regressões e música |558|
|Mobile core:flows |443|
|Mobile core:ops |58|
|Mobile core:trust |51|
|Mobile feature:launcher |280|

Lint da TV, Mobile fullRelease e feature:launcher: **zero erros**. Os relatórios
registram respectivamente160,7 e9 avisos, principalmente verificações já existentes.
`testReleaseUnitTest lintRelease assembleRelease` da TV e os testes/lints/assemble/
gate do Mobile terminaram com sucesso. Nenhuma dependência nova; verificação estrita
por checksum preservada. Os APKs têm o mesmo certificado existente:
`874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`.

- TV: versão0.1.0-preview22, código30, 1649838 bytes.
- Mobile: versão0.1.0-preview22, código22, 8078751 bytes.
- Gate Mobile:8.388.608 bytes, passou sem aumentar o limite.
- O APK TV18 embutido no celular e seu pin são idênticos aos anteriores. **Instale
  a TV22 diretamente com o APK separado.** O celular22 configura a TV22 depois disso.

## Preservação verificada

17 arquivos de Home, widgets, apps, foco, atalhos, teclas, Watch Next, media listener,
protetor nativo e ociosidade são idênticos ao commit-base por hash Git. Os corpos de
openLyrics/loadLyrics/renderLyrics/closeLyrics são idênticos. O resolver de áudio
e o downloader HTTP anteriores também são idênticos. O serviço recebeu os ajustes
necessários a perfis, histórico e ordem de reprodução, mantendo MediaPlayer,
MediaSession, foco de áudio, retomada, cancelamento e reprodução em segundo plano.

## Casos exercitados

- Dados legados no primeiro perfil; favoritos, histórico, filas, progresso, artistas,
  playlists e opções separados entre perfis. Troca pausa/salva o perfil anterior.
- Teclado QWERTY em fileiras de PC, navegação D-pad, texto sem IME padrão e acentos
  via pressão longa na letra; sem fileira de letras acentuadas.
- Ações reais de músicas/favoritos/fila, controle persistente, seleção de categorias,
  entrada e retorno da letra, e layout/foco nas renderizações nativas.
- FFT conhecido gera energia nas faixas esperadas; silêncio fica em zero. Captura
  restringida a sessão própria positiva, permissão negada, falha do backend, pausa,
  mudança de taxa e liberação. Não há captura do mix global nem batidas aleatórias.
- Etapas5/15/30, ocultação sem perder os controles, modo mínimo de4 FPS e retorno pelo
  controle consumindo a primeira tecla. Modos anteriores e Google Fotos continuam
  disponíveis pela opção de prioridade inteligente, sem mudar o álbum nativo.
- Envio Mobile por canal pareado, SHA-256 antes do commit, allowlist de arquivo,
  rejeição de opções desconhecidas e de perfil divergente; falha conserva o estado
  anterior e não marca o rascunho como aplicado. Sem reinício da Home.
- Aleatório visita cada item restante uma vez e para sem repetição; retorno anterior,
  novo ciclo com repetição e invalidação de índices após mudar a fila.

## Checagem externa limitada

Uma consulta real de artistas ao SoundCloud retornou8 resultados e uma
correspondência exata de Skank com URL de imagem. Isso valida o caminho de consulta,
não todos os artistas nem todos os downloads de imagem. A consulta real ao YouTube
Music recebeu bloqueio do proxy Cloud: CONNECT HTTP403. Não houve tentativa de
contornar esse bloqueio; a disponibilidade na rede da TV ainda precisa ser testada.

## Capturas e limites

As capturas são componentes Android reais renderizados localmente em Robolectric
Native Graphics/API35. Testes de espectro/opções/perfis incluem API34 e35. **Não são
capturas de uma TCL nem de um emulador de sistema.** Músicas, fotos e letras nas
fixtures são exemplos. O espectro das imagens com sufixo fixture é **FFT sintético
de teste**, explicitamente identificado; não comprova captura nativa de um streaming.
O Vsync é controlado nos testes para tornar a apresentação reprodutível.

Não foram medidos CPU/GPU/RAM/FPS na TCL física. Foram testados limites de cache e
fechamento dos recursos, mas isso não substitui análise de memória e desempenho no
hardware. A API Visualizer pode ser bloqueada pelo firmware/serviço. O fallback
ambiente não simula reação musical. Tela preta mantém o áudio e a iluminação LED;
nenhuma API privada de desligar painel/factory foi acionada.

Perfis são locais, sem login de conta musical. Mixes e rádios são seleções da
Launcherin, não recomendações privadas oficiais. O ano do álbum **não é obtido pela
integração pública atual** e não é inventado; álbum aparece quando há identificação
real, como ao carregar um álbum do catálogo. SoundCloud fornece coleções que não
são tratadas como álbuns confirmados. Relacionadas e fotos dependem da fonte.

## Conferir na TCL

1. Instalar por cima e confirmar os dados antigos no Perfil1.
2. Testar QWERTY, acentos ao segurar OK e navegação física nas páginas.
3. Renomear os dois perfis e conferir que filas/favoritos/progresso não se misturam.
4. Testar busca, fotos de artistas, playlists, aleatório, repetição e letra.
5. Permitir análise do áudio, ativar o visualizador e verificar a reação à música
   real, pausa, troca de faixa, saída da tela e retorno.
6. Validar etapas de proteção, tecla de retorno, tela preta, Google Fotos e a regra
   existente de não desligar durante música. Medir fluidez/memória no aparelho.

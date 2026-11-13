# Validação preview26

Data: 2026-11-13 (America/Sao_Paulo). Base: `c4d4a452db979f0c3e3948c745da1a197c3757ef` (preview25).

## Verificações concluídas

- `testReleaseUnitTest lintRelease assembleRelease`: compilação concluída. **646 testes passaram**, sem falhas, erros ou testes pulados; lint **0 erros / 163 avisos**. Nenhum baseline novo.
- Componentes Android renderizados por Robolectric API34/Android14 e API35/Android15. Não se trata de emulador completo nem da TCL física.
- Seis novas execuções de armazenamento de pesquisas: isolamento por perfil, limite de12, ordem, duplicatas, normalização, tipo salvo, entradas inválidas, remoção e limpeza sem apagar favoritos/progresso.
- Seis novos testes de UI: identidade e controles do player ao trocar seções; Pagode sem registrar busca automática; histórico selecionável/removível; volume real do stream; pesquisa manual salva e reabertura sem autoplay; letras abaixo do player sem alterar reprodução.
- Testes anteriores de perfis, foto circular, qualidade de imagens, teclado, foco, fila, recomendações, cache, letras e visualizador/proteção continuam passando.
- Capturas nativas conferidas: Home sem as duas linhas removidas, cartões maiores com Pagode, busca com histórico e letras abaixo do player. Músicas e artes são exemplos identificados na captura.
- APK `app.launcherin.tv`, code34, `0.1.0-preview26`, 1663142 bytes. SHA256 `6268995453fd030f0fe2be97269debea881237309450eccdc762f3f4b5b07249`. Assinatura v2 válida, mesmo certificado `874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`.
- 284 arquivos existentes de código/recursos main idênticos à preview25; somente PlayerActivity e MusicArtwork existentes alterados, mais SearchHistory nova. Home, widgets, Continuar Assistindo, remotos, acessibilidade, inatividade, protocolo/fotos dos perfis, serviço e resolução/HTTP de áudio não foram alterados.
- Métodos `openLyrics`, `loadLyrics`, `renderLyrics` e `closeLyrics` idênticos à base; ajuste somente no posicionamento externo do painel e guardas de foco.
- Sem novas permissões ou versões/dependências. Verificação estrita de dependências preservada. Quatro hashes de metadados POM/module já utilizados foram acrescentados após comparação com SHA256 oficial do Maven Central; nenhum checksum de biblioteca existente foi removido ou flexibilizado.
- Cache de imagens limitado como antes: RAM2MiB, avatares512KiB, disco8MB, uma thread/fila limitada. A navegação retém as imagens do player e libera vínculos de páginas antigas. Isso é validação do código/testes, não medição física de consumo.
- Mobile preview24 não modificado/recompilado; mesmo contrato de perfis/comunicação.

## Limites e teste físico pendente

Não houve TCL conectada, emulador Android completo nem coleta de desempenho ou vazamentos na TV. Reprodução real, catálogo online, foto já salva, controles do remoto e fluidez precisam ser conferidos no aparelho. Capturas e FFT de teste usam dados de exemplo; não comprovam espectro real no hardware. A proteção e o visualizador em tela cheia existentes permanecem disponíveis como modos específicos.

## Compilação reproduzível

JDK21, Android SDK36/build-tools35.0.0, Gradle8.9/AGP8.5.2; minSdk26 e targetSdk34 preservados. Com SDK/JDK configurados, execute em `launcher`: `./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease`. Os caminhos de saída foram redirecionados para `/tmp` neste ambiente por espaço disponível. Não são requisitos do aplicativo. Assinatura existente usada localmente; chave privada e credenciais não são publicadas.

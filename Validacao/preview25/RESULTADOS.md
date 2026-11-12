# Validação preview25

- Compilação `testReleaseUnitTest lintRelease assembleRelease` concluída. **634 testes passaram**, nenhuma falha; lint0 erros/164 avisos, sem baseline novo.
- Android14/API34 e Android15/API35 por Robolectric; 18 execuções novas sobre seleção de resolução, CDNs, fallback, cache antigo e amostragem. Teste da foto verifica pixels efetivamente desenhados no cabeçalho, mantendo abertura do perfil e foco do player.
- APK code33, `1663046` bytes, SHA256 `26d69f6b50bf0041ad027f2b0c18468dbc809153fdc176e4078c9187aee7ba19`. Mesmo pacote e certificado `874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`.
- 281 arquivos existentes do código/recursos main mantidos idênticos à preview24. Quatro classes existentes alteradas: renderização do perfil, dimensões do drawable, seleção de miniatura e carregador de imagens; uma classe auxiliar nova.
- Métodos de letras, resolvedor/HTTP de áudio e protocolo/dados salvos dos perfis preservados por comparação. Nenhuma nova permissão/dependência. Mobile não foi recompilado nem modificado: manter preview24.
- RAM de imagens2MiB, avatares512KiB, disco8MB, download2MB e trabalho em fila limitada preservados. Nova chave do cache invalida apenas miniaturas antigas; fotos pessoais e biblioteca não são apagadas.

## Casos verificados

Escolha da maior imagem independentemente da ordem e da qualidade estimada com dimensões desconhecidas; variantes Google/YouTube Music e SoundCloud; host semelhante malicioso não reescrito; fallback de vídeo; JPEG1280×720 preservado para recorte480px; limite de decodificação; cache pequeno antigo não reaproveitado; placeholder HD pequeno descartado em favor da imagem real disponível. Fotos locais do protetor mantêm a resolução anterior (caso JPEG1600×900). Cabeçalho com ImageView circular dedicada, foto presente no resultado gráfico e imagem antiga salva preservada.

## Limites

Não houve TCL conectada, emulador completo ou medição física de CPU/GPU/RAM/FPS. O proxy deste ambiente negou a consulta ao CDN do YouTube com CONNECT403; isso não demonstra indisponibilidade na sua TV, mas impede validar aqui a resolução de rede real. Capturas são dos componentes Android, com artes de exemplo. A melhoria depende da versão maior disponibilizada pela fonte; não é uma ampliação generativa ou substituição por capa de outro conteúdo.

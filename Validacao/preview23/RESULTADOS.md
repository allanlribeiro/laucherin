# Validação da preview23

- Base imutável: `f6450f3b60853ccf89f492e6189fc58a67a1107f`; mudanças limitadas ao módulo musical.
- Compilação final: `testReleaseUnitTest lintRelease assembleRelease` concluída com sucesso, sem relaxar a verificação de dependências.
- **591 testes passaram**, nenhuma falha. Incluem 26 execuções dos testes de aprendizado em API34/35, três novos testes de eventos reais do serviço e quatro testes de interface de recomendações. Os demais testes da TV também foram executados.
- Lint: **0 erros**, 164 avisos; nenhum baseline ou supressão foi acrescentado.
- APK `Launcherin-TV-0.1.0-preview23.apk`: 1,654,782 bytes, pacote `app.launcherin.tv`, versionCode 31.
- Certificado SHA-256 igual ao da preview22: `874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`.
- APK SHA-256: `47fb17a16a91129fbed49c1a4446c1f5f17e9b461f34f343b478834d362db4ba`.
- 278 arquivos existentes de recursos/código principal comparados com a preview22 permaneceram idênticos. Apenas Activity/Service de música existentes mudaram, com duas classes novas. Manifesto, dependências, resolvedor de áudio, Home, widgets, atalhos, Continue Assistindo e os quatro métodos de letras permanecem idênticos.
- Mobile não foi modificado nem recompilado nesta tarefa. Os testes anteriores do Mobile não são somados a estes 591 testes.

## O que os testes cobrem

Separação entre perfis; favoritos legados como ponto de partida; reforço de reproduções repetidas; limiares de conclusão e pulo rápido manual; pausas, buffering e buscas na barra não contados como escuta; limitação por tempo real; bloqueio e desfazer avaliações; favoritos bloqueados ainda preservados; limites de armazenamento e cache; expiração e relógio regressivo; deduplicação, diversidade de artistas e filtragem por fonte; prioridade a descobertas; reset sem apagar biblioteca; atribuição ao perfil anterior ao trocar; opção de feedback na tela; resultado atrasado após sair da página ignorado; ausência de reprodução automática; fallback local com catálogo indisponível; letras, fila e navegação continuam cobertas pela suíte completa.

## Capturas

Capturas locais dos componentes Android, com músicas/artes de exemplo identificadas na imagem. Não são imagens da TCL física nem provas de acesso ao catálogo ao vivo.

- `music-now-playing.png`: seção intitulada apenas **Recomendações**.
- `music-recommendations.png`: lista com feedback aplicado.
- `music-taste-settings.png`: explicação e controles do aprendizado por perfil.

## Limites

Robolectric executa os componentes Android nas APIs indicadas; não foi usado um emulador completo. Não há TCL conectada neste ambiente: atualização instalada sem perda de dados, desempenho, qualidade das sugestões e consumo de CPU/GPU/RAM no aparelho precisam de validação física. Não houve teste novo de reprodução de rede ou captura FFT real. A disponibilidade dos catálogos não é garantida por testes com fixtures.

Não há login ou acesso ao histórico privado de contas externas. Aprendizado usa o player da Launcherin e os favoritos locais. Novas sugestões dependem do catálogo público existente. O Mobile22 permanece compatível, mas seu atualizador embutido ainda contém a TV18; instale a TV23 diretamente.

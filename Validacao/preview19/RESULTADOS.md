# Validação — preview19 (teste)

Base: preview18, commit c9531a200ca525ea79f67fee7d56009a009f2713.

- TV: código 27, mesma identidade, minSdk 26 e targetSdk 34; assinatura existente conferida.
- 487 testes locais passaram (461 anteriores + 26 do player, interface e ícone).
- Lint release: zero erros. Compilação release passou com verificação estrita dos checksums das dependências.
- Casos do player: fila/favoritos persistentes e limitados, posição, cancelamento, pausa, troca para controles externos, preparação/reprodução simulada, manutenção do serviço em interrupção transitória de áudio, avanço ao completar, fim de fila, proteção do standby sem acesso a notificações.
- Interface: foco em modo de controle, telas de biblioteca/artes/letras, Voltar, interrupção das atualizações ao sair, entrada pelo widget, execução do interpretador e pixels visíveis do ícone de TV após layout.
- Os nove arquivos de Home, teclas, configurações nativas e política de ociosidade ficaram idênticos à preview18. MainActivity, AppAdapter, Config e Watch Next também ficaram idênticos.
- Nenhuma alteração de produção no aplicativo do celular.

## Fontes ao vivo

| Fonte | Resultado neste ambiente | Limite da evidência |
|---|---|---|
| SoundCloud | 10 resultados, manifest HLS HTTP 200, segmento audio/mp4 HTTP 206, 4096 bytes recebidos | Não executou o decoder real da TCL |
| LRCLIB | HTTP 200, registro de letra retornado | Cobertura depende de cada gravação |
| YouTube | CONNECT HTTP 403 no proxy do ambiente | Busca/reprodução ao vivo não validadas |

Não foram usados TCL física nem emulador de sistema. Os gráficos são capturas locais de componentes Android via Robolectric API 35, com música, capa e letra de exemplo. Não representam uma conta ou reprodução reais. Os testes de MediaPlayer usam o modelo de áudio do Robolectric, não um decoder de hardware.

## O que testar na TCL P7K / Android 14

- Instalação sobre preview18, preservação da configuração e favoritos após reiniciar.
- Busca e áudio das duas fontes, Play/Pausa/próxima/anterior e seek pelo widget/player.
- Perda e recuperação de foco de áudio ao abrir outros aplicativos.
- Artes, fotos do porta-retratos e letras disponíveis/indisponíveis.
- Protetor ativo com música e suspensão apenas do desligamento.
- RAM/PSS e tempos de quadro da Home antes e durante reprodução, com navegação pelos apps.
- Teclas físicas e ícone de TV aberta.

O tamanho do APK é separado do uso de memória. Não há promessa de fluidez ou RAM medida em hardware. A fonte YouTube pode impor verificação, login ou restrições; falhas são exibidas e não contornadas.

O APK do celular preview18 continua contendo o APK de TV preview18. Esta versão experimental exige atualizar a TV diretamente com o APK publicado aqui.

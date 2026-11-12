# Validação — preview20

Base: preview19, commit a9b6a4dbea588e52035e57768b4f8091e275ca3a.

- 514 testes locais passaram, zero falhas. Lint release com zero erros.
- Compilação release com verificação estrita dos checksums das dependências.
- Pacote app.launcherin.tv, código 28, minSdk 26/targetSdk 34, mesma assinatura.
- Serviço de áudio idêntico ao da preview19 que o usuário confirmou funcionando.
- 18 arquivos de áudio, Home, acessibilidade, atalhos, navegação da Home, Watch Next,
  temporizadores e Dreams permaneceram idênticos à base.
- Nenhuma alteração de produção no celular. A TV deve ser atualizada diretamente.

## Testes do novo comportamento

- Persistência e limites de artistas, atualização sem duplicatas, remoção, nome/fonte,
  migração de registros sem avatar, preservação de músicas, fila e posição.
- Foco da navegação lateral, campo de busca, controles e transição entre fileiras de
  resultados; ações visíveis para tocar/adicionar/favoritar artista ou música.
- Cadastros pelo diálogo, acesso direto às músicas do artista e remoção do favorito.
- Movimento em oito posições com limites; tempo de inatividade; não iniciar durante
  pausa, diálogos ou modo manual; Google Fotos não forçado por temporizador local.
- Saída do protetor consumindo a primeira tecla sem iniciar outro comando de áudio.
- Fotos sem álbum com alternativa de capa; bitmap de fundo realmente recebido quando
  há foto; quatro escolhas visíveis dentro da tela; modo escuro.
- Liberação da condição de tela ativa na pausa/segundo plano e em páginas estáticas
  de preferências/espera; referência de bitmap fora da área visível liberada.
- Encaminhamento do Google Fotos para ACTION_DREAM_SETTINGS, proteção do áudio e
  elegibilidade do modo ambiente nativo, sem usar Somnambulator como atalho imediato.
- Capturas locais de biblioteca, busca, fila, preferências e três modos próprios.

## Limites e teste físico

Não houve TCL física ou emulador de sistema nesta execução. Os testes gráficos são
Robolectric API 35 com dados e artes ilustrativos. O teste de áudio usa o modelo de
MediaPlayer; o funcionamento físico da preview19 foi informado pelo usuário, não
medido novamente neste ambiente. Busca e decodificação ao vivo das fontes não foram
retestadas nesta atualização visual.

A integração Google Fotos foi validada pelo encaminhamento às configurações e pela
liberação da tela para o temporizador nativo. O álbum privado e o Dream do Google não
foram executados aqui. Na TCL/Android 14, a entrada depende do modo ambiente e de seu
tempo de inatividade; a Launcherin não promete um comando de início imediato.

Ainda conferir na TCL: atualização preservando dados, busca e fotos reais dos
artistas, navegação física, fonte de áudio, letras, movimentos/alternância, retorno
à Home, protetor nativo com Google Fotos, regra de não desligar durante música e os
temporizadores após pausar. RAM/PSS e tempos de quadro da TV ainda não foram medidos.

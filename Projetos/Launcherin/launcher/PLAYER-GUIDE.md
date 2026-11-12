# Música na Launcherin — preview21 (teste)

Esta atualização aproxima a tela de música da referência visual escolhida: vidro
azul profundo, bordas ciano/violeta, player grande, trecho da letra ao lado e atalhos
para favoritos. A Home da launcher não é redesenhada nesta versão. O serviço de áudio
é idêntico ao da preview19 confirmada pelo usuário; fila, progresso, músicas e
artistas favoritos são preservados.

## Instalar e abrir

Instale `Launcherin-TV-0.1.0-preview21.apk` diretamente na TV, por cima da versão
anterior. Pacote `app.launcherin.tv`, código 29, com a mesma assinatura existente.
Não é necessário configurar tudo novamente. O aplicativo do celular permanece na
preview18 nesta entrega: ele continua configurando a TV, mas seu botão de atualizar
a tela inicial contém a TV preview18 e não instala esta nova versão experimental.

Na Home, segure OK sobre um dos controles do widget **Continuar ouvindo**, ou suba
até o título “Continuar ouvindo ›” e aperte OK.

Se instalar pelo ADB no Windows, na pasta platform-tools, consulte o endereço atual:

```powershell
.\adb.exe devices -l
$tv = "IP:PORTA"
.\adb.exe -s $tv install -r "$env:USERPROFILE\Downloads\Launcherin-TV-0.1.0-preview21.apk"
```

A atualização real por cima da instalação física ainda precisa ser verificada na
TCL; identidade, versão e assinatura do pacote foram conferidas localmente.

## Navegação e novo acabamento

- **Início / Tocando agora:** capa, título, artista, origem real do áudio, progresso,
  fila, anterior, Play/Pausa, próxima, protetor, favorito e opções da faixa atual.
- **Letra:** trecho sincronizado no painel lateral e letra ampliada ao selecionar
  a aba ou Abrir letra. Sem letra disponível, a interface informa isso.
- **Artistas / Bandas / Sua coleção:** favoritos separados por categoria e músicas
  favoritas. Os três atalhos inferiores abrem as mesmas coleções.
- **Buscar:** pesquisa músicas, artistas ou bandas na fonte escolhida: YouTube ou
  SoundCloud. As músicas salvas conservam sua própria origem.
- **Fila:** mostra a ordem de reprodução e abre ações de cada faixa.
- **Ajustes / engrenagem:** protetores, temporizadores, controles externos e licenças.

A barra compacta aparece nas páginas secundárias, sem duplicar os controles no
Início. Ícones de letra e protetor têm descrições acessíveis. O D-pad permite navegar
pela barra lateral, abas, player, busca e coleções. Voltar retorna ao Início; nele,
Voltar fecha a tela mantendo o áudio. As teclas de mídia continuam disponíveis.

Os gradientes, reflexos e contornos são desenhos nativos estáticos. Não há blur em
tempo real, visualizador a 60 fps ou vídeo decorativo. O cache de miniaturas continua
limitado a 2 MiB e descarta referências das imagens ocultas. As atualizações param ao
sair da tela. Não foi medida a fluidez desta atualização na TCL física.

A referência visual contém indicadores FLAC, Dolby e Lossless, rádio e recomendações.
Eles não são exibidos como recursos inexistentes: esta versão mostra a origem real
da música e oferece suas coleções salvas, busca e fila. Não há conta de perfil fictícia.

## Músicas e artistas favoritos

OK sobre uma música abre ações visíveis: **Tocar agora**, **Adicionar à fila**,
**Favoritar música** e **Favoritar artista ou banda**. Na Fila, também há remoção de
faixas que não sejam a música atual. Segurar OK continua abrindo as mesmas ações.

**Adicionar artista** ou **Adicionar banda** permite cadastrar nome, categoria e
fonte preferida. Você pode editar a categoria entre Artista e Banda ao salvar ou
posteriormente nas opções. Favoritos antigos sem categoria permanecem em Artistas;
eles não são inferidos, removidos nem duplicados. Mover de categoria não altera a fila. Ao selecionar um artista, a Launcherin busca
as músicas disponíveis naquela fonte, sem abrir outro aplicativo. **Artista
favorito · opções** permite editar ou remover. As imagens de artistas dependem do
que a fonte fornece; quando não há imagem, o cartão usa iniciais.

Limites: 50 artistas/bandas, 100 músicas favoritas e 100 itens na fila. Esses dados
ficam separados das configurações da Home e dos widgets. Salvar ou editar um artista
não altera o progresso ou a fila.

## Protetores durante o áudio

Em **Ajustes**, escolha:

| Opção | Comportamento |
|---|---|
| Capa flutuante | Capa e informações mudam de posição a cada 20 segundos |
| Fotos em sequência | Alterna as fotos enviadas ao Porta-retratos; informações mudam de posição |
| Modo escuro | Fundo preto com pequenas informações e capa em movimento por intervalos |
| Google Fotos | Usa o álbum e o temporizador do modo ambiente da própria TV |

Para os três modos próprios, escolha 30 segundos, 1, 2 ou 5 minutos de inatividade,
ou **Somente manual**. Eles entram a partir da biblioteca de música enquanto o
player próprio está tocando; **Protetor** também permite iniciar manualmente.
OK, Voltar ou uma seta retorna à biblioteca e consome essa primeira tecla, evitando
pular uma faixa ou acionar um botão sem querer. Volume e teclas de mídia continuam
com suas funções.

Os modos próprios não exibem a barra de controles ou logotipos fixos. Os elementos
mudam em intervalos, sem visualizador contínuo, vídeo ou desfoque em tempo real.
Sem fotos no Porta-retratos, Fotos em sequência usa a capa flutuante como alternativa.
O modo escuro é uma apresentação com fundo preto; não é o desligamento físico da TV.

A manutenção da tela ativa fica limitada à biblioteca/protetor próprio durante
reprodução e com temporizador local ativo. A pausa ou saída libera essa condição.
Letras, preferências e esperas de pesquisa continuam permitindo a proteção nativa.
Na Home e em outros aplicativos, vale o protetor configurado na própria TV. O
serviço de áudio e a política anterior de não desligar enquanto toca foram mantidos.

## Google Fotos: escolher o álbum

1. Abra Música → **Ajustes**.
2. Selecione **Google Fotos**.
3. Abra **Configurar álbum do Google Fotos**.
4. No modo ambiente da TCL, escolha Google Fotos e seu álbum. A configuração também
   costuma estar em Configurações → Sistema → Modo ambiente.

O álbum que você já selecionou no modo ambiente continua sendo usado. Conta,
autorização e acesso ao álbum privado ficam no sistema Google/TCL. A Launcherin
não recebe senha nem usa uma API para baixar esse álbum.

**Na TCL com Android 14, Google Fotos inicia pelo tempo de inatividade da TV.**
A opção não usa o antigo Somnambulator como se fosse um início imediato. O botão
Protetor explica esse funcionamento e abre a configuração; Visualizar protetor
imediatamente é oferecido para os três modos próprios. O tempo do Google Fotos
não é substituído pelo temporizador local de 30 segundos/1/2/5 minutos.

## Letras e reprodução

**Letra** abre uma tela própria com o título e as linhas sincronizadas destacadas,
quando o LRCLIB possui uma gravação compatível por música, artista e duração. Letras
comuns podem ser lidas; ausência e falhas são informadas. As fontes e suas restrições
continuam sendo as da preview19. Play/Pausa/próxima/anterior/seek não foram reescritos.

## Desempenho e testes

Capas são carregadas somente quando visíveis. As referências de imagens fora da tela
são liberadas; o cache de miniaturas é limitado a 2 MiB e as requisições pendentes
são limitadas. O cache de arquivos continua com limite aproximado de 8 MB. Fotos
maiores são decodificadas em resolução reduzida, sem acumular um álbum inteiro na
memória. Ao sair da tela, as atualizações visuais param. O consumo total de RAM e a
fluidez real ainda precisam de medição na TCL; o tamanho do APK não demonstra isso.

As capturas desta entrega são de componentes Android executados localmente em
Robolectric API 35, com músicas e artes de exemplo. Não são capturas da TCL nem de um
álbum real do Google Fotos. Foi validado o encaminhamento para as configurações do
sistema, não a reprodução do álbum privado dentro de um emulador.

Na TCL, confira a navegação pelo controle, áudio, retomada, imagens dos artistas,
letras, movimento dos três modos próprios e entrada do Google Fotos pelo temporizador.
Confira também o retorno à Home, as teclas físicas e os tempos de desligamento.

## Compilar

JDK 21, Android SDK plataforma 36 e build-tools 35.0.0. Na pasta `launcher/`:

```bash
./gradlew --no-daemon --max-workers=1 -I tools/native-tests.gradle testReleaseUnitTest lintRelease assembleRelease
```

As dependências permanecem fixadas e verificadas por checksum. A chave existente
não é publicada; para compilar com chave própria, veja [PLAYER-LICENSE.md](PLAYER-LICENSE.md).

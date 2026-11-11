# Música integrada — Launcherin preview19 (teste)

Esta versão acrescenta um player próprio da Launcherin. O aplicativo de Spotify e os
controles externos continuam disponíveis; a nova biblioteca não exige instalar
NewPipe ou SmartTube. Fontes implementadas: busca musical do YouTube e faixas do
SoundCloud. A disponibilidade depende dos provedores, da região e de suas respostas.

## Instalação

Atualize a TV com `Launcherin-TV-0.1.0-preview19.apk`, pacote `app.launcherin.tv`,
código 27. O certificado é o mesmo das versões anteriores. A instalação por cima
preserva configurações, pareamento, widgets e a organização dos aplicativos.

O celular permanece na preview18 nesta entrega. Seus comandos de configuração não
mudaram. O APK da TV deve ser instalado diretamente: o botão “Atualizar a tela
inicial” do celular preview18 contém a TV preview18, não esta versão experimental.
Não há um novo APK do celular nesta distribuição.

Se usar ADB no Windows, na pasta platform-tools:

```powershell
.\adb.exe devices -l
```

Use o endereço atual mostrado nessa lista e o arquivo baixado:

```powershell
$tv = "IP:PORTA"
.\adb.exe -s $tv install -r "$env:USERPROFILE\Downloads\Launcherin-TV-0.1.0-preview19.apk"
```

O comando instala uma atualização; não é necessário apagar os dados ou refazer a
configuração da TV. A atualização real por cima de uma instalação existente ainda
precisa ser conferida na TCL; identidade e assinatura do APK foram verificadas.

## Abrir e escolher músicas

1. Na Home, vá ao widget **Continuar ouvindo**.
2. Segure OK sobre Play, anterior ou próxima. Também é possível subir até o título
   “Continuar ouvindo ›” e apertar OK.
3. Pesquise uma música ou artista e selecione **Buscar**. **Fonte** alterna entre
   YouTube e SoundCloud.
4. Aperte OK em uma faixa. A lista exibida passa a ser a fila, começando na faixa
   selecionada. A biblioteca e a reprodução permanecem dentro da Launcherin.
5. Segure OK em um resultado para adicionar à fila ou favoritar. Na página Fila,
   esse menu também permite remover itens que não sejam a faixa atual.

A fila e os favoritos aceitam até 100 músicas cada. Progresso e fila são privados
no armazenamento da TV. Os links de áudio temporários são resolvidos novamente ao
retomar; não são gravados como se fossem endereços permanentes.

**Voltar à Home** mantém a música. **Apps externos** pausa o player próprio e
retorna ao modo que controla os aplicativos externos. Um novo início de reprodução
externa também pode devolver a seleção ao aplicativo correspondente.

## Artes e fotos

**Artes** amplia a apresentação do álbum dentro do player. **Fotos / Capa** alterna
entre a capa da música e as fotos já enviadas ao widget Porta-retratos pelo celular.
As fotos mudam a cada 20 segundos. Sem fotos enviadas, a capa permanece disponível.
São imagens estáticas; o player não decodifica vídeos ou visualizadores contínuos.
Pressione Voltar para retornar à biblioteca.

## Letras

**Letras** consulta o LRCLIB por música, artista e duração da gravação. Quando há
letra sincronizada compatível, as linhas acompanham o tempo do player, destacando a
linha atual com uma seta. Sem marcação de tempo, exibe a letra comum. Quando a letra
não é encontrada ou a consulta falha, informa a indisponibilidade. Não há garantia
de letras para todo o catálogo, versões ao vivo ou remixes.

## Protetor e desligamento

A regra anterior foi preservada: a música pode continuar com o protetor de tela,
e o desligamento fica suspenso durante reprodução/buffering do player próprio.
Ao pausar ou terminar a fila, a política de tempo de proteção já existente volta a
ser aplicada. Não são adicionadas permissões de Home ou serviços de acessibilidade.

## Desempenho e validação

O extrator é inicializado sob demanda. Pesquisa e resolução de áudio usam workers;
as consultas e as imagens têm limites. O cache de artes em disco é limitado a
aproximadamente 8 MB, mais um arquivo em gravação; as imagens são decodificadas em
resolução reduzida. As letras têm cache de até 12 entradas. Ao sair da tela, os
relógios visuais param e a imagem é liberada; ao pausar, o serviço pode ser encerrado
com retomada pela fila salva. O serviço não mantém a tela acesa.

O tamanho do APK não representa consumo de RAM. Uso real de memória, tempos de
quadro e fluidez na TCL P7K ainda precisam ser medidos. Não há alegação de melhoria
de performance medida em uma TV física.

Validações locais: testes JVM/Robolectric com gráficos nativos Android API 35, lint
release e compilação release, assinatura e integridade. Não foi usado emulador de
sistema nem TCL física neste ambiente. A busca e um segmento real de áudio do
SoundCloud foram recebidos; a consulta de letras respondeu. O ambiente bloqueia
YouTube com HTTP CONNECT 403, portanto não houve validação de reprodução dessa
fonte nem de seu catálogo ao vivo aqui.

Teste recomendado na TCL: buscar e tocar em cada fonte; pausar/retomar na Home;
próxima/anterior/seek; abrir outro streaming e verificar o foco de áudio; deixar o
protetor entrar; confirmar a suspensão do desligamento enquanto toca; conferir a
fluidez da barra de apps; testar fotos e letras; reiniciar e conferir favoritos e
retomada. A reprodução por codecs reais e as teclas físicas exigem esse teste.

O ícone de TV aberta foi corrigido com um vetor de dimensões intrínsecas. As ações
TV aberta, HDMI, configurações e Home permanecem iguais às da preview18.

Licenças, dependências e disponibilidade de fontes: [PLAYER-LICENSE.md](PLAYER-LICENSE.md).

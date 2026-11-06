# Launcherin preview13 — barra de aplicativos

Esta é uma candidata de teste para reduzir trabalho durante a navegação na barra de apps.
O usuário localizou os engasgos nessa barra; widgets e Continuar assistindo estão fluidos.
Ainda não há comparação de tempo de quadros, CPU ou memória na TCL depois destes ajustes.

## O que foi ajustado

- O recorte dos logos lê os pixels em lote, evitando chamadas ao bitmap para cada pixel
  enquanto um cartão novo é carregado. As cores, margens, proporções e resolução são preservadas.
- Os cartões só pedem novo layout quando tamanho ou alinhamento muda. Reutilizar um cartão
  com as mesmas dimensões deixa de disparar esse trabalho repetido; redimensionamentos continuam funcionando.
- Ativar o filtro de teclas com o auxiliar já conectado volta a habilitar a entrega de teclas.
  Outros flags de acessibilidade são preservados.

Não foi medido ganho de FPS nem redução de RAM na TCL. O uso informado de 1,2GB em1,9GB
não estabelece, sozinho, pressão de memória. A candidata mantém as funções e o visual dos widgets,
Continuar assistindo, protetor de tela e música.

## Instalação

Atualize sem desinstalar. As assinaturas permanecem as mesmas.

- **Launcherin TV preview13:** APK para a televisão, código21.
- **Launcherin Android preview13:** app do celular, código13, incluindo exatamente o mesmo APK da TV.

Pelo computador Windows, usando o endereço atual da depuração por Wi-Fi:

```powershell
$tv = "IP:PORTA"
.\adb.exe connect $tv
.\adb.exe -s $tv install -r .\Launcherin-TV-0.1.0-preview13.apk
```

Preserve o serviço **Botão Home**, da Launcherin, ativado em Acessibilidade para a tecla
de configurações funcionar nos streamings. O serviço chamado apenas Launcherin é o auxiliar de entradas.
A abertura do painel lateral pela tecla física foi confirmada na preview12 após ativar Botão Home.

## Comparação na TCL

Se possível, faça a mesma passagem antes de atualizar e depois de atualizar, com o mesmo
tema, fundo e lista de aplicativos:

1. Navegue pelos ícones que já estão visíveis.
2. Role para revelar aplicativos novos na faixa.
3. Volte pelos mesmos aplicativos já carregados.
4. Confira se o foco, o tamanho dos ícones e a abertura dos apps continuam corretos.

Compare a resposta do controle e os engasgos; não use apenas o número total de RAM como critério.
Para coletar dados opcionais, mantenha a Home visível:

```powershell
.\adb.exe -s $tv shell dumpsys meminfo app.launcherin.tv | Select-String "TOTAL PSS|TOTAL RSS|TOTAL SWAP|Views:|Activities:"
.\adb.exe -s $tv shell dumpsys cpuinfo | Select-String "app.launcherin.tv|com.klevico.monet|TOTAL"
.\adb.exe -s $tv shell df -h /data
```

Para comparar os quadros desenhados, este reset limpa somente as estatísticas de desenho:

```powershell
.\adb.exe -s $tv shell dumpsys gfxinfo app.launcherin.tv reset
```

Faça três passagens pelos mesmos aplicativos usando o controle físico. Depois execute:

```powershell
.\adb.exe -s $tv shell dumpsys gfxinfo app.launcherin.tv | Select-String "Total frames rendered|Janky frames|percentile|Missed Vsync|Slow bitmap uploads"
```

Os dados podem variar com temperatura, apps em segundo plano e carregamento inicial.
Nenhum pacote do sistema, cache de aplicativos ou serviço de acessibilidade é desativado nesta candidata.

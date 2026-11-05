# Instalação e configuração — Launcherin preview10

## 1. Preparar os arquivos

Use os instaladores de `Distribuicao/APKs/`:

- `Launcherin-Android-0.1.0-preview10.apk`: instalar no celular Android.
- `Launcherin-TV-0.1.0-preview10.apk`: instalar na TV Android/Google TV.

Em uma instalação existente da Launcherin, atualize por cima, sem desinstalar ou limpar os dados. A antiga Streamax é outro aplicativo: este pacote usa os identificadores app.launcherin.mobile e app.launcherin.tv.

O celular precisa de Android9 ou superior e bloqueio de tela. A TV e o celular devem estar na mesma rede, sem isolamento entre dispositivos. Os nomes das telas de sistema variam por versão da TCL/Google TV.

## 2. Instalar e conectar pelo celular

1. Transfira o APK Android ao celular. Abra o arquivo e autorize a instalação por essa fonte quando o Android solicitar.
2. Na TV, ative as opções do desenvolvedor: em Sobre, selecione repetidamente a versão/compilação do Android até a confirmação. Em Opções do desenvolvedor, ative a depuração necessária à conexão de rede.
3. Abra a Launcherin Mobile. Siga a conexão indicada pelo aplicativo, selecione sua TV e confirme a autorização ADB na tela da TV.
4. Se a TV oferecer somente pareamento por código, faça a etapa pelo computador descrita abaixo: esta versão do celular ainda não digita o código TLS diretamente.
5. Ao instalar/configurar a launcher pelo fluxo do aplicativo, confira o plano apresentado antes de aplicar. O app leva o APK da TV dentro dele; ele não instala na TV apenas por ter sido aberto no celular. A instalação depende de conexão, autorização e suporte do fluxo para esse modelo.
6. Se o fluxo oferecer somente limpeza e não instalar a Home nessa TCL, use a instalação manual abaixo. Não é necessário desativar a launcher do Google nem o Recovery para instalar e usar a Launcherin.
7. Para uma TV já gerenciada pelo app, aceite a atualização disponível da Launcherin TV. Não refaça a configuração inteira para cada alteração de agenda.

## 3. Alternativa: instalar na TV pelo Windows/ADB

Baixe e extraia Android SDK Platform-Tools do site oficial:
https://developer.android.com/tools/releases/platform-tools

Abra o PowerShell dentro da pasta que contém adb.exe. Use o endereço e as portas mostrados naquele momento pela TV: a porta do pareamento e a porta da conexão podem ser diferentes e mudar.

Em Depuração sem fio → Emparelhar dispositivo com código, execute:

```powershell
$ipTv = Read-Host 'IP da TV, sem a porta'
$portaPareamento = Read-Host 'Porta da tela de pareamento'
.\adb.exe pair "${ipTv}:$portaPareamento"
```

Digite o código de seis dígitos quando solicitado. Volte à tela principal de depuração sem fio da TV e use a porta de conexão:

```powershell
$portaConexao = Read-Host 'Porta de conexao da TV'
$tv = "${ipTv}:$portaConexao"
.\adb.exe connect $tv
.\adb.exe devices -l
```

A TV deve aparecer como `device`. Se já estiver pareada, normalmente basta conectar usando a porta atual. Se o Windows apontar mais de um dispositivo, use `-s $tv` nos comandos; evite comandos sem seletor.

Instale o APK usando o caminho do arquivo extraído:

```powershell
$apkTv = Read-Host 'Caminho completo do APK Launcherin-TV-0.1.0-preview10.apk'
.\adb.exe -s $tv install -r $apkTv
.\adb.exe -s $tv shell am start -n app.launcherin.tv/.MainActivity
```

Para o celular que exige ADB de rede tradicional, o fluxo de ajuda do app orienta habilitar a porta5555 uma vez pelo computador, após parear/conectar:

```powershell
.\adb.exe -s $tv tcpip 5555
```

Depois retorne ao celular e conecte ao IP da TV na porta5555. O comando pode desconectar a sessão TLS. A disponibilidade desse modo e sua persistência após reiniciar dependem do firmware; se for recusado, registre o erro em vez de alterar serviços do sistema.

## 4. Botão Home e permissões na TV

Abra a Launcherin manualmente primeiro. Caso queira retornar a ela pelo botão Home, use a opção de auxiliar Home oferecida pelo app e ative o serviço da Launcherin nas configurações de acessibilidade da TV. Preserve o Google TV ativo; este método pode permitir uma breve aparição da tela original antes do redirecionamento.

Se o Android bloquear a ativação por restrição de instalação externa, procure a opção de permitir configurações restritas nos detalhes do aplicativo. Se essa opção não existir na TCL, uma alternativa ADB já usada no projeto é:

```powershell
.\adb.exe -s $tv shell appops get app.launcherin.tv ACCESS_RESTRICTED_SETTINGS
.\adb.exe -s $tv shell appops set app.launcherin.tv ACCESS_RESTRICTED_SETTINGS allow
```

Depois tente ativar a acessibilidade manualmente na TV. Não use o identificador antigo com.streamax.launcher para esta versão. Se a operação não existir no firmware, registre o erro para diagnóstico.

Para Continuar assistindo e ouvindo, abra os controles de **Acesso a mídias** da Launcherin. A tela oferece **Histórico da TV** e **Controles de mídia**, que direcionam às autorizações correspondentes no sistema. A disponibilidade dos títulos, imagens, reprodução e retomada depende dos dados compartilhados por cada app.

## 5. Configurar pelo celular

Depois de conectar, o gerenciamento é organizado em quatro abas: **Início**, **Personalizar**, **Widgets** e **TV**. A busca ajuda a localizar cidade, transparência, times, fotos e tempos. Escolhas de aparência ficam em rascunho até **Aplicar na TV**.

### Papel de parede e aparência

Em Personalizar, escolha o fundo e o estilo. O pacote inclui um papel de parede natalino em `Midia/Papeis-de-parede/`. O tema decorativo Natal é independente do fundo: acrescenta enfeites que acompanham os cards e os apps. A transparência dos widgets é configurável.

### Organizar widgets

Em Widgets → Organizar widgets, selecione o módulo, ative/oculte, escolha Pequeno/Médio/Grande e arraste na prévia ou use os botões de posição. A grade limita posições e evita sobreposição. Aplique na TV. Ocultar elimina o card e seu espaço vazio.

### Agenda do dia

Ative o widget Agenda uma vez e aplique. Depois, em Agenda do dia, cadastre título, data e horário opcional. A TV mostra os compromissos de hoje. Cada inclusão/edição/exclusão envia somente os dados da agenda: não reaplica o tema nem reinicia a Home. Offline, os dados ficam guardados no celular. Na reconexão, sincronize; se o desbloqueio do celular tiver expirado, use Enviar agora.

### Porta-retratos

Escolha até dez fotos pelo seletor de imagens do Android e aplique. As cópias ficam na TV e trocam a cada quinze segundos. Esse widget usa fotos escolhidas no celular; não é o protetor de tela do Google Fotos.

### RAM e CPU

Ative o card para ver RAM real da TV. Quando o sistema bloqueia a medição global da CPU, o widget identifica o valor como CPU da launcher.

### Tempo e esportes

Configure a cidade e até dois times favoritos. O widget de tempo pode abrir uma previsão compacta de cinco dias; a animação acompanha condição e período do dia. Esportes usa dados públicos, portanto horários e placares dependem da disponibilidade do provedor. Avisos de gols precisam da autorização de sobreposição.

### Continuar assistindo

Em Personalizar → Continuar assistindo, escolha **Acima dos apps**, **Abaixo dos apps** ou **Oculto** e aplique. Na opção abaixo, pressione ↓ com foco nos apps: a lista aparece e os apps sobem sem sair da tela. Pressione ↑ na lista ou Voltar para recolher. Há limite de dez conteúdos; YouTube fica excluído. Segurar OK ou usar Menu num conteúdo permite remover da lista localmente.

### Protetor de tela e desligamento

Configure os tempos no grupo de inatividade. Zero desativa; o tempo de desligamento deve ser posterior ao do protetor. Desligamento significa standby.

O álbum é escolhido pelo sistema: TV → Configurações → Sistema → Modo ambiente/Protetor de tela → Google Fotos; em algumas versões a escolha do álbum fica no Google Home do celular. A Launcherin não acessa sua biblioteca privada.

Se faltarem permissões iniciais para os temporizadores, o script `Distribuicao/Scripts/Habilitar-Temporizadores.ps1` concede somente as permissões documentadas da Launcherin. Abra o script para conferir e execute a partir do PowerShell, informando seu adb.exe e o seletor atual da TV:

```powershell
$caminhoScript = Read-Host 'Caminho completo de Habilitar-Temporizadores.ps1'
& $caminhoScript -Adb '.\adb.exe' -Dispositivo $tv
```

O parâmetro opcional `-PermitirAvisos` também autoriza a sobreposição dos avisos de gols. Se a política do Windows impedir o script, examine essa política; este guia não altera automaticamente a segurança do PowerShell.

A entrada automática no protetor Google Fotos e a retomada de música depois de fechar Spotify/YouTube Music continuam pendentes de validação física nesta atualização. Apps que mantêm a tela ligada podem afetar a inatividade. Não apresentar esses comportamentos como garantidos no vídeo.

## 6. Verificação rápida após instalar

- Confirmar a versão preview10 nos dois apps.
- Conectar o celular e enviar uma mudança pequena de aparência.
- Ativar/ocultar um widget e testar a navegação pelo controle.
- Cadastrar um compromisso de hoje e confirmar que chega sem refazer o assistente.
- Testar ↓/↑ no modo Continuar assistindo abaixo dos apps.
- Conferir TV ao vivo pelo ícone e, separadamente, pela tecla física.
- Testar música com sessão ativa e novamente após fechar o app; registrar o resultado.
- Visualizar o protetor agora e testar o prazo de inatividade separadamente.

## 7. Problemas frequentes

- `adb.exe não reconhecido`: abrir o terminal na pasta platform-tools ou informar o caminho completo.
- `device not found`: executar devices -l e usar o seletor atual; as portas mudam.
- `more than one device/emulator`: usar -s com a conexão escolhida.
- `failed to connect`: conferir IP, porta de conexão, autorização e rede; a porta de pareamento não é a de conexão.
- assinatura incompatível ao atualizar: conferir se o app existente é a Launcherin deste pacote; não desinstalar como primeira tentativa, pois isso pode apagar configurações.
- O acesso de mídia não garante dados de todos os streamings. Conteúdos e imagens podem depender das permissões e das integrações disponibilizadas pelos apps.

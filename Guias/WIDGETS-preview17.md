# Launcherin preview17 — data, tempo e Sistema

TV25 / celular17. O relógio usa a fonte Outfit, com dia da semana em destaque e data separada. A previsão recebe temperatura e ilustrações coloridas de sol, nuvens, chuva, tempestade, neve e neblina, respeitando dia/noite da cidade configurada. A previsão expandida mantém a animação existente.

O antigo RAM e CPU passa a se chamar Sistema, com barras de RAM e CPU e download recebido pela TV em Mb/s. O identificador do widget permanece igual: posições, tamanhos e preferência de exibição são preservados. O fundo de vidro continua com transparência ajustável.

## Download em tempo real

A leitura usa a diferença entre contadores públicos de bytes recebidos nas interfaces de rede da TV e o tempo decorrido. Atualiza aproximadamente a cada cinco segundos, apenas com o widget visível na Home. Não inicia downloads, não faz teste de velocidade e não mostra upload. Inclui tráfego local; não mede a velocidade máxima contratada nem a taxa negociada do Wi-Fi. Zero significa ausência de tráfego no intervalo. A primeira leitura, um contador reiniciado ou uma API indisponível mostra traço. Ao voltar de outro app começa um novo intervalo.

A RAM continua incluindo o sistema e os aplicativos. Quando o Android bloqueia o uso total de CPU, o card identifica CPU · APP, que mede somente a Launcherin. CPU · TV aparece quando o contador global está disponível.

## Atualização

Atualize o celular com Launcherin-Android-0.1.0-preview17.apk, conecte-o à TV e use Atualizar a tela inicial para instalar a versão25 embarcada. Também é possível instalar o APK de TV diretamente como atualização. O celular16 continua compatível com TV25; o celular17 inclui o novo nome Sistema no editor.

Ative ou mova o widget em Widgets → Organizar widgets → Sistema. Os temporizadores mantêm os valores escolhidos anteriormente: esta versão não redefine tempos. A proteção durante música, o protetor Google Fotos, a Home, os atalhos TCL e os ajustes de fluidez dos apps são mantidos.

As ilustrações na Home são estáticas. As leituras de CPU, RAM e rede ocorrem fora da thread da interface e param ao sair da Home. A prévia usa componentes Android com dados de exemplo, não é uma captura da TCL. Validação física desta apresentação e da leitura de download ainda depende de teste na TV.

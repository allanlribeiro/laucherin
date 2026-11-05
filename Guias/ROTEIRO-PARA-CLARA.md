# Briefing para Clara — vídeo de instalação e configuração

## Pedido sugerido para enviar junto com o ZIP

Clara, analise este pacote da Launcherin preview10 e prepare um tutorial em português brasileiro ensinando a instalar a launcher na TV e o app de configuração no celular Android. Use o guia de instalação como referência principal e as capturas disponíveis como apoio visual. Explique o caminho pelo celular e, como alternativa, pelo Windows/ADB. Mostre a configuração dos widgets, agenda, fotos, Continuar assistindo, times, transparência e inatividade. Não confunda este fork com o CleanYourTV original ou a antiga Streamax. Não invente telas de pareamento, funções automáticas ou resultados na TCL que não estejam documentados. Se sua interface não conseguir gerar o vídeo final, entregue o roteiro narrado e o storyboard.

## Roteiro proposto de 6–8 minutos

| Trecho | O que explicar | Apoio visual |
| --- | --- | --- |
| 0:00–0:30 | O que é a Launcherin: Home da TV + app Android de configuração | Capturas Preview10 e home-pills-final.png, esta última identificada como prévia histórica |
| 0:30–1:00 | Requisitos: Android9+ no celular, TV Android8+, mesma rede; dois APKs distintos | Cartela com nomes dos instaladores; não exibir endereços ou códigos pessoais |
| 1:00–2:00 | Instalar o app no celular, preparar depuração na TV, autorizar a conexão e conferir o plano | Capturas mobile-preview9 mostram a organização do hub; telas de sistema devem ser gravadas separadamente |
| 2:00–3:00 | Alternativa Windows: platform-tools, pair, connect, devices e install-r | Comandos do guia com campos de IP/portas; distinguir porta de pareamento e conexão |
| 3:00–3:40 | Abrir a launcher e ativar o auxiliar Home e o acesso a mídias quando desejados | Fonte: guia; explicar que o Google TV permanece ativo e que pode piscar ao redirecionar |
| 3:40–4:40 | Organizar widgets, ocultar, alterar tamanho/posição e aplicar | celular-organizar-widgets.png; tv-widgets.png usa apps/foto sintéticos |
| 4:40–5:20 | Agenda sem reconfiguração e Porta-retratos até dez imagens | celular-agenda.png; seleção de imagens depende do seletor Android |
| 5:20–6:00 | Continuar assistindo acima/abaixo/oculto; ↓ revela e ↑ recolhe | celular-continuar-assistindo.png e tv-continuar-abaixo.png |
| 6:00–6:40 | Cidade, dois times, avisos de gols, transparência e Natal | Capturas históricas de clima/esportes/Natal como ilustração, sem simular teste real |
| 6:40–7:20 | Álbum Google Fotos escolhido no sistema, prazos de protetor e standby | Explicar diferença entre Porta-retratos e modo ambiente; mostrar configurações reais apenas se gravadas |
| 7:20–8:00 | Checklist e erros comuns do ADB; recursos pendentes de teste físico | Cartelas curtas e links internos do guia |

## Uso correto das imagens

- `Midia/Capturas/Preview10/`: estado atual e novos recursos.
- `Midia/Capturas/Mobile-preview9/`: organização do aplicativo móvel anterior, preservada como referência; não usar para afirmar que todos os controles novos estão nessas imagens.
- `Midia/Capturas/Historico/`: evolução visual, clima, esportes e Natal. Conferir os nomes das versões antes de usar.
- `Midia/Papeis-de-parede/`: fundo natalino das gatas, disponível como escolha do usuário e não instalado automaticamente nos APKs.
- Outras imagens de testes e recursos do projeto ficam nas pastas dos fontes.

As capturas atuais são de emulador/renderização de teste. O ícone Android nas capturas da TV identifica apps sintéticos; não é a identidade visual dos streamings do usuário. O retângulo azul do Porta-retratos é uma imagem de teste. Não substituir isso pela afirmação de uma instalação real na TCL. Não chamar uma captura estática de gravação do aparelho.

## Pontos que precisam ficar claros

- A instalação na TV exige conexão e autorização; não acontece só por instalar/abrir o app no celular.
- Esta versão não permite digitar diretamente no celular o código TLS de pareamento. Quando necessário, a etapa inicial usa um computador, conforme o guia.
- Atualizar os dois apps é necessário uma vez para os recursos da preview10. Cada compromisso posterior envia somente a agenda.
- Não orientar a desativar Google TV, Recovery, microfone ou serviços de sistema para seguir o tutorial.
- O auxiliar Home pode deixar a tela original aparecer brevemente, conforme firmware.
- O Google Fotos é gerenciado pelo Google TV/Google Home; não existe login Google incorporado na Launcherin para esse álbum.
- Protetor automático e retomada da música após fechar os clientes ainda precisam de teste físico nesta versão. Usar linguagem de configuração/teste, sem prometer funcionamento confirmado.
- Não instalar os APKs de instrumentação/fixtures contidos como código nos projetos.

Não há gravação bruta da instalação neste pacote. Se for necessário demonstrar telas do sistema passo a passo, solicitar/gravar essas cenas ou apresentar cartelas claramente identificadas como ilustração.

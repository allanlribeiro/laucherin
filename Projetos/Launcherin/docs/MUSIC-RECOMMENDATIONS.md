# Launcherin TV — preview23 de teste

A seção musical agora se chama **Recomendações** e aprende com o uso do player da Launcherin em cada perfil local. Não lê o histórico privado de Spotify, YouTube Music ou contas externas.

## Como usar

1. Atualize somente a Launcherin TV com `Launcherin-TV-0.1.0-preview23.apk`. Mantenha o aplicativo do celular preview22; não há alteração do protocolo ou das opções no celular nesta atualização.
2. Abra Música e selecione seu perfil no canto superior direito. O outro perfil mantém seus próprios dados.
3. Ouça e favorite suas músicas e artistas. Em **Recomendações**, escolha **Para você** ou **Descobertas**. Os outros cartões permitem filtrar Pop, Rock, Eletrônica e MPB e ordenar o catálogo pelas preferências do perfil.
4. Aperte OK em uma música ou abra ⋯ no player. Use **Gostei** ou **Não recomendar**. São ações separadas de favoritar e não alteram a reprodução atual.
5. Em **Ajustes → Recomendações deste perfil**, veja como funciona, reveja/remova avaliações ou zere o aprendizado. Favoritos, artistas, histórico, playlists, fila e preferências são preservados. Esses favoritos e artistas continuam servindo de ponto de partida após zerar.

As sugestões aparecem como uma lista para você escolher o que tocar. A busca pode precisar de alguns segundos. Se o catálogo estiver indisponível, Para você/Descobertas continuam com a seleção local disponível; não há atualização automática nem reprodução iniciada ao chegar um resultado atrasado.

## Como o aprendizado funciona

- Ouvir pelo menos 80% da duração real reforça a música e seu artista. Novas reproduções qualificadas reforçam a preferência, com limites para evitar que uma única faixa domine tudo.
- Um avanço manual à próxima faixa conta negativamente somente após pelo menos 2 segundos, antes de 30 segundos e antes de 20% da música. Pausas, erros e mudanças automáticas não são avaliações negativas.
- O tempo considera avanço real da reprodução e tempo decorrido. Buscar perto do fim, pausar ou ficar carregando não equivale a ouvir a música.
- Favoritos já existentes e artistas/bandas favoritos entram como sinais, sem recadastrar dados.
- **Gostei** é uma avaliação explícita. **Não recomendar** exclui aquela URL das sugestões do perfil, inclusive se ainda estiver nos favoritos. A faixa continua acessível na biblioteca, busca e fila. Não bloqueia todas as músicas do artista. Em Recomendações deste perfil → Rever avaliações é possível desfazer.
- Não há modelo de IA pesado: classificação local, diversidade de artistas, deduplicação e prioridade para novidades em Descobertas. Artistas e gêneros não são inventados ou inferidos como metadados confirmados.

O armazenamento é limitado a 200 faixas com sinais, incluindo avaliações, e a 60 candidatos em cache por perfil. Ao atingir o limite, sinais automáticos antigos são descartados primeiro. Se todas as entradas forem avaliações explícitas, as mais antigas também poderão ser substituídas. O cache dura 15 minutos. Há no máximo duas consultas por abertura de uma seleção personalizada sem cache, ou uma consulta por filtro de gênero. Não há consultas periódicas na Home ou por mudança de foco.

Os sinais e avaliações ficam nos dados privados do aplicativo na TV, sem telemetria. A busca usa os serviços públicos já integrados, enviando a consulta de artista ou a URL da faixa-semente necessária para obter relacionados; não envia o banco de histórico nem acessa sua conta do serviço.

## Atualização e compatibilidade

O pacote continua `app.launcherin.tv`, com o certificado existente. Instale por cima, sem desinstalar nem limpar dados. Com ADB conectado a apenas uma TV: `adb install -r Launcherin-TV-0.1.0-preview23.apk`.

Esta entrega não troca a Home, o teclado PC, abas musicais, letras, visualizador, protetores, widgets, navegação de aplicativos, atalhos do controle ou Continuar Assistindo. A integração e o resolvedor de áudio existentes são mantidos. As mudanças no serviço apenas observam eventos de reprodução para alimentar o aprendizado local.

Importante: o instalador de TV embutido no Mobile preview22 ainda contém a TV preview18, conforme documentado na preview22. Para receber esta atualização, instale o APK TV preview23 diretamente; essa opção antiga do celular não instala a preview23.

## Limites dos testes

Validação automatizada local com Robolectric Android 14/API34 e Android 15/API35; capturas dos componentes Android com músicas/artes de exemplo. Não equivalem a teste físico da TCL nem a um emulador completo. A qualidade das recomendações depende dos dados disponíveis e melhora conforme o uso; não é a recomendação de uma conta autenticada do serviço. A disponibilidade do catálogo depende do serviço e da rede.

Teste de atualização instalada, consumo de RAM, CPU, GPU e fluidez na TCL P7K continua pendente. Não houve medição física nem teste de espectro real nesta entrega. Consulte RESULTADOS.md e resultados.json para os testes desta versão.

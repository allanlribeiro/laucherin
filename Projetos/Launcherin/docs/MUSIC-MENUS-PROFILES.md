# Launcherin TV + Mobile — preview24 de teste

## O que mudou

- **Menus internos da música** com o acabamento da tela de áudio: painéis escuros de vidro, bordas suaves, fonte Outfit, ícones nos perfis, foco visível e botões próprios.
- **Menus da Home** usam as superfícies da família visual e as cores do tema atual. Inclui menus internos de configuração, widgets, ações do Continuar Assistindo e os popups de entradas e biblioteca de aplicativos. Não importa novos temas.
- **Foto no perfil musical**, escolhida e configurada no celular, com prévia, alteração e remoção. O nome também pode ser editado e novos perfis podem ser adicionados. Editar não troca o perfil ativo nem pausa a reprodução.
- **Visualizador circular em tela cheia**, com capa central, cores configuradas, movimento suave e o mesmo espectro real da sessão do player. Sem animações falsas de batidas. Há um modo ambiente opcional existente para aparelhos que bloqueiem a captura.
- Corrigida uma chamada de opacidade que podia deixar a capa quase invisível no modo de movimento reduzido.

## Instalação

Atualize **os dois APKs** para usar a configuração de fotos pelo celular:

1. Instale `Launcherin-TV-0.1.0-preview24.apk` na televisão, por cima da versão atual.
2. Instale `Launcherin-Mobile-0.1.0-preview24.apk` no Android do celular, também por cima.
3. Não desinstale os aplicativos nem limpe os dados. Ambos mantêm os identificadores de pacote e o certificado das versões anteriores.
4. Conecte o celular à TV pelo pareamento já utilizado no aplicativo.

O instalador de tela inicial embutido no Mobile ainda contém a TV preview18, por limite de tamanho do aplicativo. Ele **não instala a TV preview24**. Use o novo APK da TV diretamente, sem usar esse instalador antigo para esta atualização. O limite de 8 MiB do Mobile não foi aumentado nem foi removido conteúdo para acomodar outro bundle.

Com ADB já conectado a apenas uma TV, uma alternativa é: `adb install -r Launcherin-TV-0.1.0-preview24.apk`. O APK Mobile é instalado no próprio celular.

## Configurar nome e foto

No celular: **Personalizar → Perfis da música**.

1. Escolha o perfil que deseja editar. “Ativo na TV” indica qual está ouvindo naquele momento; selecionar para editar no celular não muda o perfil em reprodução.
2. Edite o nome ou toque em **Escolher foto no celular**.
3. O seletor do Android permite escolher uma imagem. Ela é centralizada em um recorte quadrado e a prévia mostra o recorte circular. Escolha uma foto com o rosto próximo do centro.
4. Confira e toque em **Salvar perfil na TV**. A foto aparece no cabeçalho da música e na lista de perfis da TV.
5. Para remover, use **Remover foto** e depois salve. A TV volta a mostrar a inicial ou o símbolo de perfil.

A escolha não exige permissão para ler toda a galeria. A imagem é convertida em JPEG de até 512 × 512 e 180 KB, sem os metadados EXIF originais. A transferência usa o pareamento existente, valida SHA-256, conteúdo e dimensões e verifica se o perfil mudou desde a leitura. Uma edição recusada não substitui o perfil anterior; o rascunho fica no celular para conferir e tentar novamente.

Se outro celular configurou a foto e ela não está no cache local deste aparelho, o painel informa que já há foto na TV; é possível substituí-la ou removê-la. Não há sincronização com conta de Google Fotos para avatares nem download automático de fotos pessoais de uma nuvem.

Os perfis continuam **locais ao módulo musical**, com bibliotecas e aprendizado separados. Não são contas de YouTube Music ou Spotify. Para mudar quem está ouvindo, abra o círculo de perfil na TV e escolha a pessoa; a troca continua pausando e salvando a reprodução anterior. Editar a foto ou o nome pelo celular não faz essa troca.

## Abrir o círculo em tela cheia

Na tela de música, com uma faixa tocando, escolha qualquer uma destas opções:

- Selecione a **capa circular** e pressione OK.
- Abra **⋯ → Visualizador em tela cheia**.
- Entre em **Ajustes → Abrir visualizador em tela cheia**.

O visualizador ocupa a tela com fundo preto e capa central. Usa as cores, intensidade e movimento já configurados em **Widgets → Visualizador e proteção musical** no celular. Pressione **Voltar** para retornar ao player, sem pausar. As regras de proteção inteligente permanecem disponíveis, incluindo redução de brilho e processamento. O modo de tela preta existente também continua disponível e não desliga o painel LED fisicamente.

A captura de espectro usa a sessão positiva do próprio player e a permissão de análise de áudio existente. Se o firmware bloquear a API, a música continua e o anel não inventa batidas. A liberação da captura na TCL precisa de teste físico, como nas versões anteriores.

## O que foi preservado

Home e posições dos elementos, rolagem e foco dos aplicativos, abas de música, letras, fila, favoritos, playlists, recomendações por perfil, widgets, Continue Assistindo, atalhos e serviços de acessibilidade, configuração rápida da TCL e regras de desligamento. Os quatro métodos de letras e o serviço/resolvedor musical permanecem iguais à preview23.

Os painéis da própria TCL, as permissões do Android e o seletor de fotos do celular continuam sendo telas do sistema. A personalização desta entrega se aplica aos menus internos da Launcherin.

## Validação

Veja RESULTADOS.md, resultados.json e SHA256SUMS.txt. As capturas são dos componentes Android com dados e artes de exemplo. O espectro mostrado na captura de teste é sintético e está identificado na própria imagem; não representa captura real de uma TCL conectada.

Não houve acesso físico à TCL, teste de atualização instalado nem medição de RAM, CPU, GPU ou FPS no aparelho. O código inclui limites de imagens/cache, renderização sem blur contínuo e suspensão dos efeitos quando invisíveis, mas o desempenho físico continua a ser validado.

# Launcherin preview18 — aparência e personalização

Versão de teste de 11/11/2026. TV: código 26. Celular: código 18.

## Instalação e uso

1. Atualize o **Launcherin Mobile** instalando o APK sobre o app existente.
2. Conecte o celular à TV cadastrada e use **Atualizar a tela inicial** para instalar a versão da TV. O APK da TV fornecido separadamente é o mesmo incluído no Mobile.
3. Em **Personalizar → Aparência**, escolha o tema visual, estilo dos widgets, acabamento dos aplicativos e intensidade dos efeitos. Toque em **Aplicar na TV**.
4. Em **Personalizar → Papel de parede**, escolha manter o fundo atual, o original do tema, uma imagem do celular ou uma coleção dinâmica. A foto pessoal é guardada separadamente do tema visual.
5. Os controles de transparência e organização dos widgets continuam nas suas opções existentes. A restauração do acabamento padrão não apaga a foto pessoal nem a organização da Home.

Os APKs usam os mesmos identificadores e certificado das versões anteriores. A atualização sobre a instalação existente preserva os dados. Não é necessário desinstalar.

## O que foi implementado

- Sete widgets existentes com acabamento compartilhado: relógio, previsão, times, música, sistema, agenda e porta-retratos.
- Três famílias: **Glass Premium**, com profundidade, reflexos estáticos e bordas iluminadas; **Minimal Cinematic**, com acabamento discreto; **Ambient Dynamic**, com iluminação e contraste adaptados à paleta.
- Aplicativos com opções **Original**, **Glass**, **Minimalista** e **Premium**. A arte oficial e os limites da linha de apps são mantidos. O arredondamento continua seguindo a regra existente dos cartões.
- Os três atalhos superiores, Continuar Assistindo, foco e barras de progresso acompanham a paleta. Artes reais, logos dos títulos e modos acima/abaixo/oculto são preservados.
- Novos ícones meteorológicos locais para nuvens, chuva e noite; ícones e cena continuam seguindo as condições informadas pela integração existente.
- Agenda com horários destacados; porta-retratos com moldura e proteção de contraste para o título; Sistema preserva RAM, CPU e download e acrescenta uso de armazenamento interno.
- Painéis expandidos usam o acabamento do tema. Abrir, fechar e retornar ao widget mantêm o mecanismo existente.
- Tema e papel de parede têm estado próprio. A aplicação de aparência não regrava o arquivo de organização da Home nem reinicia a Activity.
- Paleta automática opcional, amostrada em 64 × 36 pixels quando a imagem muda e armazenada em cache. A imagem original não é alterada.
- Coleções dinâmicas usam até dez imagens da galeria já existente, com intervalo configurável; a rotação acompanha a visibilidade da Home.
- Opção **Efeitos reduzidos** para simplificar as camadas visuais.

## Times favoritos e Brasileirão

O compacto mostra até dois confrontos distintos com escudos e horários, priorizando um próximo jogo por favorito. Um jogo entre os próprios favoritos não é duplicado.

A expansão tem as abas **Próximos jogos** e **Brasileirão Série A**. A primeira preserva os próximos dois jogos e último resultado de cada favorito. A classificação apresenta os vinte clubes, posição, pontos, jogos, vitórias, empates, derrotas e saldo; favoritos são marcados com estrela.

A tabela pode ser percorrida pelo controle dentro do painel. A fonte é a ESPN, já utilizada nos jogos. A consulta é feita ao abrir a aba, com cache de quinze minutos e intervalo de cinco minutos entre tentativas em caso de falha. Dados anteriores são identificados; ausência de informação não é convertida em zero. A tabela não inicia consultas periódicas na Home.

## Temas existentes

O catálogo existente foi preservado; a importação dos 29 temas foi cancelada e não faz parte desta versão. O celular mantém suas onze definições de temas, sujeitas às regras sazonais já existentes: Padrão, Seis colunas, Cinema, Console, Horizonte, Preto discreto, Diwali, Biblioteca, Arcade, Névoa e Aurora. Os dezesseis diretórios de temas do projeto da TV também foram mapeados na camada de cores; aliases de estilos e fundos existentes são resolvidos pelo mesmo mapa. Não são anunciados como um novo catálogo de 29 temas.

## Segurança e preservação

A comunicação usa o pareamento e a sessão ADB já existentes. A TV aceita somente as opções visuais previstas pelo protocolo. Imagens têm limite de tamanho, verificação SHA-256 completa, validação de dimensões e teste de decodificação antes da aplicação. Arquivos temporários usam nomes curtos derivados do hash para respeitar os limites do transporte existente.

A imagem pessoal e os arquivos originais dos temas não são substituídos. O cache privado de imagens mantém as seleções atual e anterior após aplicações bem-sucedidas. Falhas de validação não alteram a configuração ativa; uma exceção durante a aplicação dos componentes restaura as opções anteriores. Na troca assíncrona de wallpaper, a imagem anterior permanece até a nova ficar pronta.

Os arquivos de Home, acessibilidade, tecla de configurações, entradas e política de ociosidade foram comparados com a preview17. Os nove componentes protegidos e onze métodos de geometria/navegação conferidos permanecem idênticos. Nenhuma nova permissão Android foi acrescentada.

## Desempenho

O vidro usa gradientes, reflexos e contornos nativos; não usa desfoque de tela em tempo real. Shaders são preparados quando os limites dos componentes mudam. Os quatro novos ícones somam aproximadamente 29 KB comprimidos e possuem cache de bitmap limitado. O processamento de banners Glass/Minimalista é feito fora da UI, limitado em resolução e armazenado em cache. A opção Premium mantém a arte original sem essa extração.

Não há uma medição de ganho de fluidez na TCL nesta entrega. Os timers e a navegação já aprovados foram preservados, e os novos efeitos evitam loops contínuos. A confirmação de fluidez e consumo real de memória depende do teste físico.

## Limitações conhecidas

- As imagens conceituais aprovadas são direção artística. A implementação utiliza camadas nativas estáticas; não reproduz refração óptica real nem todo detalhe de uma imagem gerada.
- Em Glass/Minimalista, a remoção do fundo ocorre somente quando ele é uniforme e o resultado preserva contraste. Banners com gradientes ou marcas que perderiam legibilidade mantêm a arte original.
- A tabela identifica clubes por nome e favoritos por estrela. Escudos ficam no compacto e nos detalhes dos favoritos; a tabela não baixa automaticamente vinte imagens de escudos.
- A paleta é uma aproximação da cor predominante, não uma análise semântica da fotografia. O ajuste de contraste adaptativo pode escurecer mais a superfície sobre imagens claras.
- Dados meteorológicos e esportivos dependem das fontes existentes e podem ficar indisponíveis. O estado e a hora da consulta são mostrados.
- As limitações de reprodução do YouTube Music/YouTube TV em segundo plano permanecem as do aplicativo original. Esta atualização não acrescenta um player de streaming.
- Não foi executado emulador de sistema ou teste físico na TCL Android 14 nesta sessão. A máquina não disponibiliza aceleração KVM. Os testes gráficos locais usam Robolectric Native Graphics com API 35.

## Testes e artefatos

Os resultados, tamanhos, hashes, certificado e conferência do APK incluído no Mobile estão em **resultados.json** e **RESULTADOS.md**. As capturas de componentes Android ficam separadas dos conceitos artísticos.

O código está nas pastas `Projetos/Launcherin` e `Projetos/Launcherin-Mobile`. A relação precisa dos arquivos desta atualização está em **arquivos-alterados.json**. As chaves privadas de assinatura não fazem parte dos artefatos publicados.

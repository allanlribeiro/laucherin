# Launcherin TV — preview25

Correções sobre a preview24:

- Foto do perfil desenhada por uma ImageView circular dedicada no cabeçalho. Mantém nome, seleção por controle e imagens já salvas. O teste agora verifica pixels efetivamente desenhados, além da presença do arquivo/drawable.
- Seleção das imagens do catálogo por resolução, sem depender da ordem da lista. Considera a qualidade estimada quando a fonte não informa dimensões.
- Miniaturas de Google/YouTube Music, YouTube e SoundCloud usam variantes maiores dos respectivos CDNs, com limite de tamanho e retorno à URL original se a variante não estiver disponível.
- Decodificação preserva resolução para o recorte do cartão/capa; descarta pequenos placeholders de versões HD que não existam. Limita o bitmap remoto a1.048.576 pixels e mantém RGB565, cache RAM2MiB, cache de avatares512KiB, disco8MB e downloads em fila limitada.
- A decodificação das fotos locais do protetor mantém o comportamento anterior.
- Novo namespace do cache de capas evita reutilizar as miniaturas ruins da versão anterior. Não limpa fotos dos perfis, biblioteca, histórico ou dados pessoais.

## Instalação

Atualize somente a TV com `Launcherin-TV-0.1.0-preview25.apk`, por cima da versão atual, sem desinstalar nem limpar os dados. O Mobile preview24 permanece compatível e não precisa ser atualizado.

As fotos enviadas e salvas anteriormente continuam no mesmo lugar. Abra novamente Música depois de instalar. A foto deve aparecer na bolinha do perfil ativo; fotos de outros perfis são vistas ao selecionar esses perfis. Se o arquivo estava ausente, o cabeçalho volta a tentar carregá-lo em vez de guardar uma falha permanente.

No celular, o caminho continua Personalizar → Perfis da música → Escolher foto → Salvar perfil na TV. O instalador de tela inicial embutido ainda é antigo: instale o APK TV25 diretamente.

## Limites

Não há ampliação artificial que inventa detalhe. A qualidade depende da imagem que o catálogo realmente disponibiliza. Quando não existe versão maior, a URL original continua disponível como fallback; alguns serviços mostram uma miniatura de vídeo em vez da arte oficial de um álbum. A nova versão não troca essa imagem por uma capa sem correspondência confirmada.

Testes locais usam componentes Android reais por Robolectric e imagens/URLs de exemplo. A captura do cabeçalho tem foto de exemplo. Não houve acesso físico à TCL nem medição de FPS/RAM/CPU/GPU. A consulta ao CDN de imagens do YouTube foi recusada pelo proxy deste ambiente (CONNECT403); disponibilidade e qualidade de rede na TV precisam ser conferidas no aparelho.

Player, letras, recomendações, menus, protocolos do celular, widgets, Home, controle remoto, visualizador e regras de inatividade foram preservados.

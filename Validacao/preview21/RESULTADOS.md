# Validação — Launcherin TV preview21

Base imutável: `c104bbb97876ddcdb4d18e1ff2d40e534de2a11e` (preview20).
Variante TV: app.launcherin.tv, código 29, versão 0.1.0-preview21.

## Resultado local

- **521 testes passaram**, sem falhas ou erros.
- Lint release: **0 erros**. `testReleaseUnitTest lintRelease assembleRelease` terminou com sucesso.
- Dependências com verificação estrita por checksum; nenhuma dependência nova.
- APK: 1,604,290 bytes; SHA-256 em SHA256SUMS.txt.
- Certificado: `874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`, igual à versão anterior.
- 18 arquivos de Home, foco, teclas, idle, player de áudio e widgets conferidos por Git blob contra a base: sem alteração.

## O que mudou e foi exercitado

A tela principal da música agora tem player grande, capa quadrada, título, artista,
origem real, linha de progresso, fila, anterior/Play/Pausa/próxima, favorito,
opções da faixa e proteção. A letra sincronizada aparece ao lado, com a linha
atual destacada, e pode ser ampliada. Busca, fila e preferências mantêm uma barra
compacta sem rótulos cortados. Abas e atalhos levam a artistas, bandas e músicas
favoritas. A classificação é editável, persistida e não duplica favoritos.

A suíte inclui layout e limites dos componentes, rotas de foco por D-pad,
busca com respostas controladas, edição de categoria, dados legados, fila/progresso
preservados, ações da faixa atual mantendo a fila, letra em cache e atualização da
interface sem reconstruir o player a cada segundo. Foram novamente executados os
testes anteriores de Home, widgets, navegação, configurações, idle, player, áudio e
proteção. Imagens ocultas liberam referências; miniaturas têm cache de 2 MiB.

Google Fotos continua pelo modo ambiente da TV. O teste confirma o intent público
e que a tela de música não impede o temporizador nativo; não lê álbuns privados.
Os três protetores próprios mantêm troca de posição/fotos a cada 20 segundos,
saída consumindo a primeira tecla e suspensão das atualizações em segundo plano.

## Capturas

Os PNGs são renderizações de componentes Android reais em Robolectric API 35,
960×540, com dados, letras e artes de exemplo identificados no rodapé. O estado de
reprodução das capturas é simulado pela fixture. Nenhum streaming, conta, álbum
privado ou dado pessoal foi usado para criar essas capturas. Elas não são mockups
usados como interface nem fotografias da TCL.

## Limites e validação física pendente

Não foi usado emulador de sistema nem uma TCL física nesta execução. Permanecem
pendentes a instalação por cima na TV, D-pad físico, catálogo ao vivo, áudio real,
letras reais, entrada no álbum Google Fotos, retomada após voltar à Home e tempos
de ociosidade. RAM, GPU e tempo de quadros não foram medidos: o tamanho do APK e
os testes locais não comprovam fluidez na P7K.

A referência gráfica mostra FLAC/Dolby/Lossless, perfil, rádio e recomendações.
Essas capacidades não foram inventadas. A interface usa a origem real da faixa,
suas coleções e os recursos existentes. Não foram adicionados shuffle/repeat sem
suporte no motor. O serviço de reprodução permaneceu igual ao já testado pelo usuário.

O celular não foi alterado: segue preview18 com TV18 embutida. Instale o APK TV21
diretamente, sem desinstalar nem apagar os dados. O APK release mantém assinatura
e pacote; a migração foi exercitada em testes, não numa atualização física aqui.

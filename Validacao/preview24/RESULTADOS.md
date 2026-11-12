# Validação — preview24

Base: `8a2fab500aca38846ae213819fd4f85c773cd8b9`. Entrega de teste em branch própria, sem merge na main nem release de produção.

## Compilação, testes e assinatura

**1458 testes passaram**, sem falhas: TV616, fluxos do celular449, operações58, confiança51 e UI284. Lint: TV0 erros/164 avisos; app Mobile0/7; UI Mobile0/9. Não foram relaxados lint, testes, verificação de dependências ou limites de tamanho.

As suítes exercitam componentes Android por Robolectric nas APIs34/35, e Compose API35 para o celular. Não houve emulador completo nem TCL física conectada. As capturas são das implementações Android usando músicas e artes de exemplo. O FFT dos exemplos é sintético e identificado na imagem.

- TV: `Launcherin-TV-0.1.0-preview24.apk`, pacote `app.launcherin.tv`, code32, 1661518 bytes, SHA256 `54db09a8ec11cab2a67fb17740dd6060b8d13f79c2dcb35ef1f361941e850766`.
- Mobile: `Launcherin-Mobile-0.1.0-preview24.apk`, pacote `app.launcherin.mobile`, code24, 8101443 bytes, SHA256 `eeb0d4825b47bd374de1abf34cfc512c7aca067a80f20553869c7ec88cb43cfb`.
- Ambos assinados com o certificado existente: `874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`.
- Mobile passou no limite inalterado de8.388.608 bytes. Nenhuma permissão ou dependência nova.
- O primeiro R8 do celular foi interrompido pelo limite de memória do ambiente, com oom_kill registrado. A compilação final passou isoladamente, com heap1536m, metaspace512m e ActiveProcessorCount2. Não foi uma falha funcional dos testes; os logs estão retidos no ambiente de trabalho.

## O que foi verificado

- JPEG real com dimensão/tamanho/hash válidos; imagem corrompida, hash trocado, dimensão acima512 e caminhos arbitrários recusados.
- Commit de metadados vinculado ao hash esperado e à revisão do perfil: rejeita alteração concorrente e mantém os dados anteriores.
- Edição do nome/foto preserva ID, perfil ativo, posição e favoritos. Renomear pela TV também mantém a foto. Adicionar perfil não o ativa; limite de8 preservado.
- Remover foto preserva os demais perfis e só apaga arquivo compartilhado quando não é mais usado.
- Avatar circular, atualização no cabeçalho e menu de perfis; cache de imagem limitado a512KiB na TV.
- Celular usa os portos de pareamento, autenticação e armazenamento existentes. Nomes seguem em JSON, não no shell. Hash incorreto impede commit e conserva rascunho para retry. Não abre/reinicia a Home nem inicia reprodução.
- Picker explícito, recorte quadrado, JPEG<=512/180KB e liberação dos bitmaps intermediários. Não há leitura de toda a galeria ou permissão nova.
- Os menus mantêm callbacks de clique, OnShow e fechamento. Superfícies Home compartilham a família e paleta existentes; música usa seu acabamento próprio.
- Tela cheia pelo player, capa central e foco restaurado ao voltar, sem pausar nem reconstruir controles. Redução de processamento e movimento conservados. A capa mantém opacidade correta com movimento reduzido.
- Suite completa inclui letras, recomendações, fila, widgets, navegação e funcionalidades existentes.

## Comparação de código protegido

21 classes de áudio, letras, recomendações, widgets, apps, Watch Next, Home/acessibilidade e inatividade foram verificadas contra a revisão anterior e são idênticas. O serviço e resolvedor de reprodução não foram alterados. Os quatro métodos de letras continuam byte a byte idênticos. MainActivity só ganhou quatro chamadas para estilizar popups; removendo essas linhas, o arquivo é idêntico ao anterior. Pins e o bundle de TV do celular estão intactos.

## Limites e próximo teste físico

Instale ambos APKs por cima e valide na TCL: foto e nome por perfil, reabrir a launcher, troca de perfis, menus de Home/música, D-pad/foco, espectro real, tela cheia/Voltar, pausa/próxima faixa e comportamento em segundo plano. Validar também atualização instalada sem perda de dados e consumo/fluidez no hardware.

Não há medição física de FPS/CPU/GPU/RAM ou prova nova de acesso ao catálogo musical nesta entrega. APIs de espectro e desempenho dependem do firmware. Menus e permissões do Android/TCL permanecem do sistema. Foto configurada por outro celular pode não ter prévia no cache deste; o painel identifica a foto existente e permite substituir/remover. O instalador de TV antigo no Mobile continua na TV18: instalar o APK TV24 diretamente.

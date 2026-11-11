# Resultados — Launcherin preview18

Verificação local em 11/11/2026.

| Verificação | Resultado |
|---|---|
| TV: testes unitários, integração local e gráficos | 461 testes, zero falhas |
| Mobile: fluxos | 437 testes, zero falhas |
| Mobile: operações e transporte | 58 testes, zero falhas |
| Mobile: pin, integridade e confiança | 51 testes, zero falhas |
| Mobile: novas páginas de aparência | 5 testes, zero falhas |
| Mobile: widgets existentes | 6 testes, zero falhas |
| Total desses conjuntos | 1.018 testes aprovados |
| Lint da TV | 0 erros; 139 avisos |
| Compilação release TV e FullRelease Mobile | Concluídas |
| Limite de 8 MiB do Mobile | Aprovado |
| APK da TV incluído no Mobile | Idêntico, byte a byte |
| Certificado | Igual ao usado nas versões anteriores |
| Arquivos de Home, controle e ociosidade comparados | Nove idênticos à preview17 |
| Métodos de geometria/navegação comparados | Onze idênticos à preview17 |

## Artefatos assinados

### Launcherin-TV-0.1.0-preview18.apk

- Pacote: `app.launcherin.tv`
- Código de versão: `26`
- Tamanho: `790808` bytes
- SHA-256: `158ce647bd7668527134bb34d60e4e66ae0edea45e2fa5dadd8e8dca55325671`
- Certificado SHA-256: `874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`

### Launcherin-Mobile-0.1.0-preview18.apk

- Pacote: `app.launcherin.mobile`
- Código de versão: `18`
- Tamanho: `8052155` bytes
- SHA-256: `aa8e05856fa67457ad7cb301063cc77f5f9582189b21ed34c0c36b4923c8e13c`
- Certificado SHA-256: `874d2fbd801f80e4a78bc11e535aaa56990ffbbf616741312a6935bb256ea83d`

## Escopo dos testes

Os testes gráficos novos usam Robolectric Native Graphics / API 35. Não são uma execução em emulador de sistema nem validação física na TCL. Sem KVM nesta máquina e sem conexão ativa com a TV.

Foram verificados validação de opções e arquivos, SHA-256, imagens inválidas, restauração após falha síncrona de aplicação, persistência da foto ao trocar a paleta, coleção dinâmica, limpeza do cache após sucessos, contrastes e mapeamento de todas as paletas existentes nas três famílias. O teste de primeiro vínculo de um app confirma que o raio da borda usa a mesma regra do recorte original mesmo antes da medição.

Na tabela esportiva: resposta pública gravada da ESPN, vinte clubes, ordenação oficial, estatísticas ausentes, duplicação, cache, falhas, atualização sob demanda, favoritos e navegação de linhas. No Mobile: integridade antes do commit, rejeição por versão antiga, rascunhos sem escrita imediata, separação de wallpaper e estilo, ausência de reinício da Home e preservação dos recursos antigos.

Fluidez na TCL, consumo de memória sob carga real, navegação completa por controle físico, serviços de streaming, música e a retomada do protetor/standby ainda exigem teste no aparelho. Não foi declarado ganho de performance medido.

O código e as capturas reais foram organizados em uma candidata de teste separada da versão principal. Veja LEIA-ME.md para funcionalidades e limitações visuais.

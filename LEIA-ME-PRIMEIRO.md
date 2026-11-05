# Launcherin — pacote para Clara no ChatGPT

Versão entregue: 0.1.0-preview10. Este pacote contém o estado atual dos dois projetos, incluindo alterações locais que ainda não foram publicadas. Não é apenas um clone dos repositórios originais.

## Por onde começar

1. Leia `Guias/INSTALACAO-E-CONFIGURACAO.md`.
2. Para preparar o vídeo, leia `Guias/ROTEIRO-PARA-CLARA.md`.
3. Os dois instaladores atuais estão em `Distribuicao/APKs/`.
4. As capturas atuais estão em `Midia/Capturas/Preview10/`. As pastas anteriores mostram a evolução do projeto, não necessariamente a interface atual.
5. O código está em `Projetos/Launcherin/` (TV) e `Projetos/Launcherin-Mobile/` (celular). Recursos e capturas usados pelos testes também foram preservados nesses diretórios.
6. `MANIFESTO.json` relaciona os arquivos e seus hashes SHA-256. As evidências atuais de validação ficam em `Validacao/`.

## Identidade dos apps

- TV: `app.launcherin.tv`, código18, versão0.1.0-preview10; Android8/API26 ou superior.
- Celular: `app.launcherin.mobile`, código10, versão0.1.0-preview10; Android9/API28 ou superior.
- O APK do celular inclui o mesmo APK da TV entregue separadamente.
- O fork é baseado em CleanYourTV e Clean Your TV App. As licenças, créditos e fontes de terceiros estão preservados. Os READMEs originais podem citar apps, links e versões do projeto de origem; para este pacote, priorize este guia, o guia de instalação e o histórico LAUNCHERIN.md de cada projeto.

## Estado e limites

A preview10 inclui Agenda manual, Porta-retratos, RAM/CPU, organização/tamanho/visibilidade dos widgets e três posições de Continuar assistindo: acima, abaixo dos apps e oculto. Na posição abaixo, Down revela a lista mantendo os apps visíveis; Up ou Voltar recolhe. Editar a agenda não reaplica toda a aparência nem reinicia a TV.

A validação registrou165 testes JUnit e5 verificações de instrumentação Android, sem falhas. As capturas atuais da TV são de emulador API28 com apps e dados de exemplo; as telas do celular foram renderizadas em testes API35. Não apresentar essas imagens como teste físico na TCL.

A retomada de Spotify/YouTube Music após fechar os apps e a entrada automática no Google Fotos ainda dependem de teste na TCL. O auxiliar Home preserva o Google TV e pode permitir uma breve aparição da launcher original, conforme o firmware.

## O que foi excluído

Chaves de assinatura/ADB, credenciais locais, arquivos local.properties, diretórios .git, caches, ambientes virtuais, saídas de compilação, backups particulares e logs locais de trabalho. O ZIP não contém o histórico Git nem a chave privada necessária para assinar novas atualizações compatíveis com os APKs distribuídos. Os APKs atuais continuam assinados e instaláveis.

Para compilar do código-fonte, instale SDK/JDK conforme `Guias/COMPILACAO.md` e configure sua própria assinatura. O projeto de celular inclui o conteúdo do submódulo de terceiros; não é necessário ter o histórico Git desse submódulo para ler os arquivos.

Todos os arquivos foram reunidos localmente. O pacote não foi enviado à Clara nem publicado em nenhum serviço.

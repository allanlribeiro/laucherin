# Licenças do player integrado

A variante da Launcherin TV que inclui o player integrado utiliza NewPipe Extractor
v0.27.1, sob GPL-3.0-ou-posterior. O executável combinado desta variante é distribuído
sob GPL-3.0-ou-posterior. Os componentes anteriores sob MIT preservam seus avisos e
continuam disponíveis sob MIT. O aplicativo de celular não vincula o extrator.

O código correspondente da variante fica no mesmo ramo/commit do APK, no diretório
`Projetos/Launcherin/launcher/` do repositório de distribuição. O projeto inclui scripts
Gradle e versões fixadas das dependências. A chave privada de assinatura não é
necessária para estudar ou compilar o código; para assinar uma compilação própria,
crie `.keys/launcherin-test.keystore` com `keytool`, alias `launcherin`, senhas
`android`, ou ajuste o bloco de assinatura para uma chave sua. Uma assinatura nova
não substitui a instalação original sem a chave original.

Dependências adicionadas:

| Componente | Versão | Licença / fonte |
|---|---|---|
| NewPipe Extractor | v0.27.1 | GPL-3.0-ou-posterior; https://github.com/TeamNewPipe/NewPipeExtractor/tree/v0.27.1 |
| Rhino | 1.8.1 | MPL-2.0; https://github.com/mozilla/rhino/tree/Rhino1_8_1_Release |
| nanojson | e9d656ddb49a412a5a0a5d5ef20ca7ef09549996 | Apache-2.0; https://github.com/TeamNewPipe/nanojson/tree/e9d656ddb49a412a5a0a5d5ef20ca7ef09549996 |
| jsoup | 1.24.1 | MIT; https://github.com/jhy/jsoup/tree/jsoup-1.24.1 |
| Protocol Buffers Java Lite | 4.36.2 | BSD-3-Clause; https://github.com/protocolbuffers/protobuf/tree/v36.2 |
| JSR-305 annotations | 3.0.2 | Apache-2.0 segundo o POM; https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/ |
| desugar_jdk_libs_nio | 2.1.5 | GPL-2.0-com-Classpath-exception, Apache-2.0 e avisos originais do OpenJDK; https://github.com/google/desugar_jdk_libs |

Textos e avisos acompanham `app/src/main/assets/licenses/`. A tela Música → Sobre
também identifica as bibliotecas e oferece a leitura da GPL. O código do extrator
não foi copiado ou modificado: é resolvido como dependência fixada. O interpretador
Rhino é usado pelo extrator; a ponte `rhino-engine` para Java SE não é incluída.

Os direitos sobre músicas, capas e letras permanecem com seus respectivos titulares.
A consulta de letras usa https://lrclib.net/docs. Não são incluídos áudios, letras
de músicas ou capas baixadas no código ou nos APKs.

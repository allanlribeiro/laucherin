# Código-fonte e compilação

Os diretórios Projetos/Launcherin e Projetos/Launcherin-Mobile contêm os fontes atuais, incluindo testes, imagens, licenças, Gradle Wrapper e conteúdo do submódulo de terceiros. Não incluem SDK, JDK, caches, histórico Git, local.properties ou chaves privadas.

## TV

- Diretório Gradle: Projetos/Launcherin/launcher.
- SDK de compilação36, build-tools35.0.0, Java17 ou superior; o ambiente desta entrega usou JDK21.
- Identificador: app.launcherin.tv; código18.
- Comando habitual: `bash gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease`.
- A configuração local de assinatura em app/build.gradle referencia .keys/launcherin-test.keystore na raiz do projeto. Esse arquivo foi propositalmente excluído. Antes de compilar, configure um keystore próprio no local/configuração esperado; não use a expectativa de obter a assinatura dos APKs distribuídos.

## Celular

- Diretório Gradle: Projetos/Launcherin-Mobile.
- JDK21, SDK de compilação37, build-tools37 conforme configuração do projeto.
- Identificador: app.launcherin.mobile; código10.
- Configure ANDROID_HOME/ANDROID_SDK_ROOT ou gere seu local.properties, com caminhos locais da sua máquina.
- Para release, configure CYT_RELEASE_KEYSTORE, CYT_RELEASE_KEY_ALIAS, CYT_RELEASE_KEYSTORE_PASSWORD e CYT_RELEASE_KEY_PASSWORD com seus próprios valores, sem colocá-los no controle de versão.
- Exemplo de tarefas: `bash gradlew --no-daemon --max-workers=1 --no-parallel :app:assembleFullRelease :core:trust:test :ui:design:checkComposeStability :app:checkApkSizeFullRelease`.
- Verificação de recursos: `python3 scripts/check-launcherin-portuguese.py`.
- O APK TV18 já está incluído em android/platform/src/main/assets/bundled/cleanyourtv-launcher.apk. Seus pins correspondem à distribuição deste pacote.

Se recompilar a TV com outra assinatura, será necessário manter coerentes o APK embarcado e as verificações explícitas de certificado/hash/versão do companion. O empacotador scripts/bundle-launcherin-tv.py recusa troca silenciosa de certificado ou redução de versão; não trate a rejeição como defeito do ZIP nem desligue as verificações para instalar no aparelho do usuário.

Uma assinatura própria não substitui a original numa atualização Android por cima da instalação existente. Para instalar e usar, prefira os APKs assinados presentes em Distribuicao/APKs. Para estudo/alterações dos fontes, consulte LAUNCHERIN.md, testes e licenças de cada projeto. Dependências de Gradle podem precisar ser baixadas da internet.

Os READMEs de origem e comandos de CI foram preservados como documentação histórica do projeto base. Eles podem usar nomes, certificados ou links dos apps originais; o inventário desta entrega e os guias em português identificam a Launcherin atual.

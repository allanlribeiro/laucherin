# Launcherin preview12 — painel lateral TCL

Atualização candidata para teste. A chamada do painel foi confirmada na TCL real via ADB;
o botão físico com o novo APK ainda precisa ser confirmado. A versão está na ramificação
`fix/tcl-settings-preview12`, sem substituir a versão estável do repositório.

## Instalação

Baixe o APK da TV e atualize por cima da instalação existente, sem desinstalar:

```powershell
$tv = "IP:PORTA"
.\adb.exe -s $tv install -r .\Launcherin-TV-0.1.0-preview12.apk
```

Use o endereço atual da tela principal de Depuração por Wi-Fi. O app do celular preview12
também inclui exatamente esse APK da TV. Se atualizar pelo computador, o celular é opcional.
Os dois APKs preservam a assinatura anterior.

## Ativar a tecla dentro dos aplicativos

1. Abra as **Configurações da Launcherin → TV aberta, gols e inatividade**.
2. Use **Testar painel de configurações da TV**. Deve abrir o painel lateral desejado.
3. Ative **Tecla de configurações em outros apps** e mantenha o auxiliar de acessibilidade
   **Botão Home**, da Launcherin, ativado nas configurações de acessibilidade da TCL.
4. Abra a Netflix ou outro streaming e pressione a tecla física ⚙️. Confirme o painel lateral.
5. Feche com Voltar e confira que o aplicativo continua aberto. Teste também Home, volume,
   volume mantido pressionado, TV ao vivo e HDMI.

Se a atualização desativar o auxiliar por configurações restritas, libere a autorização e
ative-o novamente pela tela de acessibilidade da TV:

```powershell
.\adb.exe -s $tv shell appops set app.launcherin.tv ACCESS_RESTRICTED_SETTINGS allow
```

Não é necessário desativar ou reativar outros aplicativos do sistema. A leitura global é
opcional e pode afetar o volume mantido pressionado em algumas TCL. Para desligar essa
leitura, desative tanto a opção de configurações quanto a da tecla de TV em outros apps,
caso ambas estejam ativadas. Filtros personalizados já configurados são preservados.

## Comportamento

- Na TCL compatível, a tecla chama o serviço nativo com `Type=Settings` e `Catagory=picture`.
  A alternativa `Type=quicksettings` abriu uma barra inferior diferente e foi descartada.
- O painel é aberto diretamente, sem levar a Home para a frente. O auxiliar age ao soltar
  a tecla e ignora um evento cancelado.
- A preferência explícita pelo painel da Launcherin (`settings_key_panel`) continua valendo.
  Para usar o painel TCL, deixe essa preferência desativada se a configurou manualmente.
- Quando o serviço nativo não existe ou não permite o acesso, mantém-se a abertura anterior
  de configurações. A confirmação via ADB não substitui o teste com o APK comum.
- As telas completas de configurações continuam disponíveis pelos respectivos atalhos.
- Nenhum APK ou código da TCL é redistribuído neste pacote.

## Protetor e música

Inclui as correções da preview11: protetor nativo no Android14 e proteção do desligamento
durante música reconhecida. O protetor pode mostrar fotos enquanto a música toca; apenas
o standby fica suspenso. Ao pausar, começa um novo prazo completo.

Mantenha o álbum selecionado no modo ambiente da TCL. Para o ajuste solicitado, aplique
**Protetor após 5 minutos** e **Desligar após 10 minutos** na Launcherin, com acesso de mídia
autorizado e permissões para os temporizadores:

```powershell
.\adb.exe -s $tv shell pm grant app.launcherin.tv android.permission.WRITE_SECURE_SETTINGS
.\adb.exe -s $tv shell appops set app.launcherin.tv WRITE_SETTINGS allow
```

A proteção nova de música/standby também precisa de teste físico após a atualização.
No firmware observado, o YouTube Music perde a sessão ao sair; use-o no próprio aplicativo.

package app.launcherin.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/** Remote-friendly entry points, also configured by the companion through config.json. */
final class LauncherinControls {
    private static final int[] TIMES={0,1,2,5,10,15,30,45,60,90,120,180,240};
    static void choose(Activity host, Runnable changed) {
        String status=LauncherinIdle.apply(host);
        String[] labels={
                "Aprender tecla de TV aberta",
                "Tecla de TV em outros apps: "+(LauncherinRemote.global(host)?"ativada":"desativada"),
                "Avisos de gols: "+(LauncherinRuntimePrefs.read(host,LauncherinGoals.KEY)==1?"ativados":"desativados"),
                "Permitir avisos sobre outros apps",
                "Acesso de mídia para música e gols",
                "Protetor após: "+time(LauncherinIdle.saver(host)),
                "Desligar após: "+time(LauncherinIdle.sleep(host)),
                "Google Fotos · escolher álbum no modo ambiente",
                "Liberar alteração dos tempos",
                "Temporizadores: "+(status.isEmpty()?"configuração aplicada / padrão da TV":status),
                "Visualizar protetor agora · "+LauncherinDreams.selected(host),
                "Diagnóstico do protetor e da música"
        };
        new AlertDialog.Builder(host).setTitle("TV aberta, gols e inatividade")
                .setItems(labels,(d,index)->{
                    switch(index) {
                        case 0: open(host,new Intent(host,LauncherinRemoteActivity.class));break;
                        case 1:
                            if(LauncherinRemote.global(host))save(host,LauncherinRemote.FILTER,0,changed);
                            else new AlertDialog.Builder(host).setTitle("Usar tecla de TV em outros apps?")
                                .setMessage("Requer a acessibilidade da Launcherin ativada. O Android exige a leitura global de teclas; em algumas TCL isso pode afetar o volume ao manter o botão pressionado. Se acontecer, desative esta opção. Aprender a tecla sozinho não ativa essa leitura.")
                                .setPositiveButton("Ativar",(a,b)->save(host,LauncherinRemote.FILTER,1,changed))
                                .setNegativeButton("Cancelar",null).show();break;
                        case 2: save(host,LauncherinGoals.KEY,LauncherinRuntimePrefs.read(host,LauncherinGoals.KEY)==1?0:1,changed);break;
                        case 3: open(host,new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+host.getPackageName())));break;
                        case 4: open(host,new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));break;
                        case 5: minutes(host,true,changed);break;
                        case 6: minutes(host,false,changed);break;
                        case 7: photos(host);break;
                        case 8: permissions(host);break;
                        case 10: LauncherinDreams.start(host,true);break;
                        case 11: LauncherinMusicActions.diagnostic(host);break;
                        default: Toast.makeText(host,status.isEmpty()?"Os tempos valem em todos os apps. Vídeos e músicas que mantêm a TV acordada interrompem a contagem. Desligar significa entrar em espera.":status,Toast.LENGTH_LONG).show();
                    }
                }).setNegativeButton("Fechar",null).show();
    }
    static String time(int minutes) { return minutes==0?"desativado":minutes+" min de inatividade"; }
    private static void minutes(Activity host, boolean saver, Runnable changed) {
        String[] labels=new String[TIMES.length];int selected=-1,current=saver?LauncherinIdle.saver(host):LauncherinIdle.sleep(host);
        for(int i=0;i<TIMES.length;i++){labels[i]=time(TIMES[i]);if(current==TIMES[i])selected=i;}
        new AlertDialog.Builder(host).setTitle(saver?"Quando iniciar o protetor?":"Quando desligar a TV?")
                .setSingleChoiceItems(labels,selected,(d,index)->{
                    int a=saver?TIMES[index]:LauncherinIdle.saver(host),b=saver?LauncherinIdle.sleep(host):TIMES[index];
                    if(!LauncherinIdle.valid(a,b)){Toast.makeText(host,"O desligamento deve ocorrer depois do protetor.",Toast.LENGTH_LONG).show();return;}
                    save(host,saver?LauncherinIdle.SAVER:LauncherinIdle.SLEEP,TIMES[index],changed);d.dismiss();
                    String status=LauncherinIdle.apply(host);
                    if(!status.isEmpty())permissions(host);
                    else if(saver && TIMES[index]>0)photos(host);
                }).setNegativeButton("Cancelar",null).show();
    }
    private static void save(Activity host,String key,int value,Runnable changed) {
        try {LauncherinExtras.setInt(host,key,value);LauncherinRemote.refresh(host);HomeKeyService.applyTvRemote();changed.run();}
        catch(Exception failure){Toast.makeText(host,"Não foi possível salvar.",Toast.LENGTH_LONG).show();}
    }
    private static void permissions(Activity host) {
        String command="adb shell pm grant app.launcherin.tv android.permission.WRITE_SECURE_SETTINGS\nadb shell appops set app.launcherin.tv WRITE_SETTINGS allow";
        new AlertDialog.Builder(host).setTitle("Permissões dos temporizadores")
                .setMessage("A alteração dos tempos precisa de duas permissões na TV. Pelo computador já pareado, execute (no PowerShell use .\\adb.exe no lugar de adb):\n\n"+command+"\n\nDepois volte a esta tela. As configurações anteriores ficam salvas para restauração quando os dois tempos forem desativados.")
                .setPositiveButton("Alterar configurações",(d,w)->open(host,new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+host.getPackageName()))))
                .setNegativeButton("Fechar",null).show();
    }
    private static void photos(Activity host) {
        new AlertDialog.Builder(host).setTitle("Álbum do Google Fotos")
                .setMessage("No modo ambiente da TV, escolha Google Fotos e selecione seu álbum. A conta e a autorização ficam com o Google. Se a tela abrir só a lista de protetores, escolha Modo ambiente. Na TCL, também pode estar em Configurações → Sistema → Modo ambiente.\n\nA Launcherin usa o protetor escolhido no sistema; não baixa seu álbum nem recebe sua senha.")
                .setPositiveButton("Abrir modo ambiente",(d,w)->open(host,new Intent(Settings.ACTION_DREAM_SETTINGS)))
                .setNegativeButton("Fechar",null).show();
    }
    private static void open(Activity host,Intent intent) {
        try {host.startActivity(intent);}catch(RuntimeException unavailable){Toast.makeText(host,"Abra esta opção nas configurações da TV.",Toast.LENGTH_LONG).show();}
    }
}

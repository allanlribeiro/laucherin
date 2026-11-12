// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import android.content.Context;import android.content.pm.PackageManager;import android.media.audiofx.Visualizer;
/** Own player session only (>0). No microphone recorder and no global output-mix capture. */
final class LauncherinAudioSpectrum {
 interface Capture {void start(int session,int rate,Listener listener)throws Exception;void close();}
 interface Listener {void fft(byte[] fft);}
 interface Factory {Capture create();}
 static Factory factory=NativeCapture::new;
 private Capture capture;private int session=-1,rate;private String status="";
 boolean sync(Context c,int id,boolean playing,boolean minimal,Listener listener){int wantedRate=minimal?5000:20000;if(!playing||id<=0){close();status=playing?"Aguardando a sessão de áudio":"Reprodução pausada";return false;}if(c.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){close();status="Permita o acesso ao áudio nos Ajustes da música";return false;}if(id==session&&rate==wantedRate)return capture!=null;close();session=id;rate=wantedRate;Capture candidate=factory.create();try{candidate.start(id,wantedRate,listener);capture=candidate;status="Espectro real da sessão de áudio";return true;}catch(Exception|LinkageError e){candidate.close();status="O dispositivo não disponibilizou o espectro deste áudio";return false;}}
 String status(){return status;}
 void close(){if(capture!=null){capture.close();capture=null;}session=-1;}
 private static final class NativeCapture implements Capture {private Visualizer visualizer;
  public void start(int session,int rate,Listener listener){if(session<=0)throw new IllegalArgumentException("Sessão própria necessária");visualizer=new Visualizer(session);int[] range=Visualizer.getCaptureSizeRange();visualizer.setCaptureSize(Math.min(range[1],Math.max(range[0],512)));int r=visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener(){public void onWaveFormDataCapture(Visualizer v,byte[] data,int sampleRate){}public void onFftDataCapture(Visualizer v,byte[] fft,int sampleRate){listener.fft(fft);}},Math.min(Visualizer.getMaxCaptureRate(),rate),false,true);if(r!=Visualizer.SUCCESS||visualizer.setEnabled(true)!=Visualizer.SUCCESS)throw new IllegalStateException("Captura indisponível");}
  public void close(){if(visualizer!=null){try{visualizer.setEnabled(false);visualizer.release();}catch(RuntimeException ignored){}visualizer=null;}}
 }
}

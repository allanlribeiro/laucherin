// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.downloader.*;
import org.schabi.newpipe.extractor.localization.*;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Loaded on demand on the player's worker, never during Home creation or focus changes. */
final class LauncherinMusicSource {
    static final String USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static boolean initialized;
    private static synchronized void init(){if(!initialized){NewPipe.init(new Http(),new Localization("pt","BR"),new ContentCountry("BR"));initialized=true;}}
    static List<LauncherinTrack> search(String query,boolean soundcloud)throws Exception {
        init();String q=query.trim();if(q.isEmpty()||q.length()>160)throw new IOException("Digite o nome da música ou do artista.");
        StreamingService service=soundcloud?ServiceList.SoundCloud:ServiceList.YouTube;
        org.schabi.newpipe.extractor.search.SearchExtractor extractor=service.getSearchExtractor(q,Collections.singletonList(soundcloud?"tracks":"music_songs"),"");
        extractor.fetchPage();SearchInfo info=SearchInfo.getInfo(extractor);
        if(info.getRelatedItems().isEmpty()&&!info.getErrors().isEmpty())throw new IOException("A fonte não retornou resultados utilizáveis",info.getErrors().get(0));
        List<LauncherinTrack> out=new ArrayList<>();for(InfoItem item:info.getRelatedItems())if(item instanceof StreamInfoItem){StreamInfoItem s=(StreamInfoItem)item;
            if(s.getDuration()>0&&s.getDuration()<=86400&&LauncherinTrack.validPage(s.getUrl()))out.add(new LauncherinTrack(s.getUrl(),s.getName(),s.getUploaderName(),image(s.getThumbnails()),s.getDuration()*1000,soundcloud?"soundcloud":"youtube",image(s.getUploaderAvatars())));
            if(out.size()==30)break;
        }return out;
    }
    static String image(List<Image> images){if(images==null||images.isEmpty())return "";Image best=images.get(0);for(Image i:images)if(i.getHeight()>=160&&i.getHeight()<=720)best=i;return best.getUrl();}
    static String resolve(LauncherinTrack track)throws Exception {
        init();StreamInfo info=StreamInfo.getInfo(NewPipe.getServiceByUrl(track.url),track.url);
        AudioStream best=null;int score=Integer.MIN_VALUE;
        for(AudioStream a:info.getAudioStreams()){
            if(!a.isUrl()||!a.getContent().startsWith("https://"))continue;
            if(a.getDeliveryMethod()!=DeliveryMethod.PROGRESSIVE_HTTP&&a.getDeliveryMethod()!=DeliveryMethod.HLS)continue;
            // Prefer plain AAC/MP4, a modest bitrate and Android's hardware/system codecs.
            int rank=(a.getDeliveryMethod()==DeliveryMethod.PROGRESSIVE_HTTP?1000:0)+(a.getFormat()==MediaFormat.M4A?500:0)-Math.abs(a.getAverageBitrate()-128);
            if(rank>score){best=a;score=rank;}
        }
        if(best==null)throw new IOException("Esta faixa não oferece um fluxo de áudio compatível. Escolha outra.");
        return best.getContent();
    }
    static String error(Throwable e){String n=e.getClass().getSimpleName();if(n.contains("ReCaptcha")||n.contains("SignIn")||n.contains("Bot"))return "A fonte pediu verificação ou login. Tente outra faixa ou o SoundCloud.";if(n.contains("ContentNotAvailable")||n.contains("Geographic"))return "Música indisponível nesta fonte ou região.";if(e instanceof java.net.SocketTimeoutException)return "A fonte demorou para responder. Tente novamente.";return "Não foi possível carregar a música. Verifique a conexão ou tente outra fonte.";}
    static final class Http extends Downloader {
        @Override public Response execute(Request request)throws IOException {
            URL url=new URL(request.url());for(int redirect=0;redirect<=5;redirect++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Pesquisa cancelada");
                if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null)throw new IOException("Endereço inseguro");
                HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestMethod(request.httpMethod());c.setRequestProperty("User-Agent",USER_AGENT);c.setRequestProperty("Accept-Language","pt-BR,pt;q=0.9,en;q=0.5");
                for(Map.Entry<String,List<String>> h:request.headers().entrySet())if(!h.getKey().equalsIgnoreCase("Accept-Encoding"))c.setRequestProperty(h.getKey(),String.join(", ",h.getValue()));
                try {byte[] data=request.dataToSend();if(data!=null){c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(data);}}
                    int code=c.getResponseCode();if(code>=300&&code<400&&c.getHeaderField("Location")!=null){url=new URL(url,c.getHeaderField("Location"));continue;}
                    InputStream stream=code>=400?c.getErrorStream():c.getInputStream();String body="";if(stream!=null)try(InputStream in=stream){body=new String(LauncherinExtras.bytes(in,6_000_000),StandardCharsets.UTF_8);}
                    return new Response(code,c.getResponseMessage(),c.getHeaderFields(),body,url.toString());
                }finally{c.disconnect();}
            }throw new IOException("Redirecionamentos demais");
        }
    }
}

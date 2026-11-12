// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
import java.util.*;
/** Artist identity matching; avoids substituting album art or a similarly-named channel. */
final class LauncherinArtistPhotos {
 static final class Profile {final String name,url,art;Profile(String n,String u,String a){name=n;url=u;art=a;}}
 static String normalize(String name){return java.text.Normalizer.normalize(name.replaceFirst("(?i)\\s*-\\s*topic$",""),java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
 static Profile exact(String name,List<Profile> choices){Profile found=null;for(Profile p:choices)if(!p.art.isEmpty()&&normalize(name).equals(normalize(p.name))){if(found!=null&&!found.url.equals(p.url))return null;found=p;}return found;}
}

// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;
/** Positive-frequency FFT bins only. Silence stays zero; no random or time-generated amplitudes. */
final class LauncherinSpectrum {
 static void bands(byte[] fft,float[] out){java.util.Arrays.fill(out,0);if(fft==null||fft.length<8)return;int max=fft.length/2;for(int i=0;i<out.length;i++){int first=Math.max(1,(int)Math.floor(Math.pow(max-1,i/(double)out.length))),last=Math.min(max-1,Math.max(first,(int)Math.ceil(Math.pow(max-1,(i+1)/(double)out.length))));double peak=0;for(int k=first;k<=last;k++){double re=fft[k*2],im=fft[k*2+1];peak=Math.max(peak,Math.hypot(re,im));}out[i]=peak<2?0:(float)Math.min(1,Math.log1p(peak)/Math.log(182));}}
}

package app.launcherin.tv;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** Fits the installed app's original mark to the wide tile without recolouring or stretching it. */
final class LauncherinAppArtwork {
    /** Some TV banners include transparent outer margins. They must not shrink the visible pill. */
    static Drawable banner(Resources resources, Drawable live) {
        int width=live.getIntrinsicWidth(),height=live.getIntrinsicHeight();
        if(width<=0||height<=0)return live;
        float scale=Math.min(1f,320f/Math.max(width,height));
        width=Math.max(1,Math.round(width*scale));height=Math.max(1,Math.round(height*scale));
        Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Drawable copy=live.getConstantState()==null?live:live.getConstantState().newDrawable(resources).mutate();
        Rect old=new Rect(copy.getBounds());try{copy.setBounds(0,0,width,height);copy.draw(new Canvas(image));}finally{copy.setBounds(old);}
        int[] pixels=new int[width*height];image.getPixels(pixels,0,width,0,0,width,height);
        int left=width,top=height,right=-1,bottom=-1;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(Color.alpha(pixels[y*width+x])>16){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}
        if(right<left||left==0&&top==0&&right==width-1&&bottom==height-1){image.recycle();return live;}
        Bitmap trimmed=Bitmap.createBitmap(image,left,top,right-left+1,bottom-top+1);
        if(trimmed!=image)image.recycle();return new BitmapDrawable(resources,trimmed);
    }
    static final class Mark {
        final Drawable artwork;
        final int[] background;
        Mark(Drawable artwork, int[] background) { this.artwork = artwork; this.background = background; }
    }
    private static int difference(int a, int b) {
        return Math.max(Math.abs(Color.red(a)-Color.red(b)), Math.max(Math.abs(Color.green(a)-Color.green(b)), Math.abs(Color.blue(a)-Color.blue(b))));
    }
    static Mark of(Resources resources, Drawable live, String packageName) {
        if (live == null) return new Mark(null, null);
        Drawable source = live.getConstantState() == null ? live : live.getConstantState().newDrawable(resources).mutate();
        Rect saved = new Rect(source.getBounds());
        final int n = 192;
        Bitmap bitmap = Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888);
        try { source.setBounds(0,0,n,n); source.draw(new Canvas(bitmap)); }
        finally { source.setBounds(saved); }
        // One bitmap-to-array transfer: binding a new app must not cross into native graphics
        // once per pixel while the same UI thread is scrolling the row.
        int[] pixels=new int[n*n];bitmap.getPixels(pixels,0,n,0,0,n,n);
        int base = pixels[3*n+n/2];
        int[] samples = {base,pixels[(n-4)*n+n/2],pixels[(n/2)*n+3],pixels[(n/2)*n+n-4]};
        boolean flat = true;
        for(int color:samples) if(Color.alpha(color)<240 || difference(color,base)>32) flat=false;
        boolean transparent = Color.alpha(pixels[0])<20 && Color.alpha(pixels[n-1])<20
                && Color.alpha(pixels[(n-1)*n])<20 && Color.alpha(pixels[n*n-1])<20;
        // This older Disney TV icon has a dark gradient around its white wordmark.
        // Keep the original foreground colours and place that mark over the full-width brand gradient.
        if(!flat && packageName.equals("com.disney.disneyplus")) {
            int[] markPixels=new int[n*n];int count=0;
            for(int y=0;y<n;y++) for(int x=0;x<n;x++) {
                int pixel=pixels[y*n+x],l=pixels[y*n+2],r=pixels[y*n+n-3];
                float t=x/(float)(n-1);
                int background=Color.rgb(Math.round(Color.red(l)*(1-t)+Color.red(r)*t),Math.round(Color.green(l)*(1-t)+Color.green(r)*t),Math.round(Color.blue(l)*(1-t)+Color.blue(r)*t));
                int light=Math.min(Color.red(pixel),Math.min(Color.green(pixel),Color.blue(pixel)));
                if(light>150 && difference(pixel,background)>60) {
                    int alpha=Math.min(Color.alpha(pixel),Math.round(Math.min(1f,(light-150)/40f)*255));
                    markPixels[y*n+x]=(pixel&0x00ffffff)|(alpha<<24);count++;
                }
            }
            if(count>n*n/100 && count<n*n/2){
                bitmap.setPixels(markPixels,0,n,0,0,n,n);pixels=markPixels;transparent=true;
            }
        }
        int left=n,top=n,right=-1,bottom=-1;
        if(flat || transparent) for(int y=0;y<n;y++) for(int x=0;x<n;x++) {
            int pixel=pixels[y*n+x];
            boolean content=Color.alpha(pixel)>32 && (!flat || difference(pixel,base)>42);
            if(content){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}
        }
        if(right>left && bottom>top) {
            int padding=3;left=Math.max(0,left-padding);top=Math.max(0,top-padding);right=Math.min(n-1,right+padding);bottom=Math.min(n-1,bottom+padding);
            Bitmap cropped=Bitmap.createBitmap(bitmap,left,top,right-left+1,bottom-top+1);
            if(cropped!=bitmap) bitmap.recycle();bitmap=cropped;
        }
        return new Mark(new BitmapDrawable(resources,bitmap),flat?new int[]{base,base}:null);
    }
}

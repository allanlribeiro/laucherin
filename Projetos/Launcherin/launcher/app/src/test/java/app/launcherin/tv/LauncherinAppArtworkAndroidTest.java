package app.launcherin.tv;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

/** Native bitmap rendering checks the crop, original colors and opacity after bulk pixel processing. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LauncherinAppArtworkAndroidTest {
    private Resources resources;
    @Before public void prepare(){resources=RuntimeEnvironment.getApplication().getResources();}
    private Bitmap bitmap(int w,int h,int background,int color,int l,int t,int r,int b){
        int[] pixels=new int[w*h];Arrays.fill(pixels,background);
        for(int y=t;y<b;y++)Arrays.fill(pixels,y*w+l,y*w+r,color);
        Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        image.setDensity(Bitmap.DENSITY_NONE);image.setPixels(pixels,0,w,0,0,w,h);return image;
    }
    private Bitmap result(LauncherinAppArtwork.Mark mark){return ((BitmapDrawable)mark.artwork).getBitmap();}

    @Test public void transparentBannerMarginsAreRemovedWithoutChangingTheLogoColor(){
        Bitmap source=bitmap(320,180,Color.TRANSPARENT,0xff13a5cd,24,18,296,162);
        Bitmap cropped=((BitmapDrawable)LauncherinAppArtwork.banner(resources,new BitmapDrawable(resources,source))).getBitmap();
        assertEquals(272,cropped.getWidth());assertEquals(144,cropped.getHeight());
        assertEquals(0xff13a5cd,cropped.getPixel(0,0));assertEquals(0xff13a5cd,cropped.getPixel(271,143));
        assertFalse(source.isRecycled());
    }

    @Test public void opaqueBannerKeepsItsOriginalDrawableAndBounds(){
        Bitmap source=bitmap(320,180,0xffc31546,Color.WHITE,60,60,260,120);
        Drawable banner=new BitmapDrawable(resources,source);banner.setBounds(2,3,100,90);
        assertSame(banner,LauncherinAppArtwork.banner(resources,banner));
        assertEquals(new Rect(2,3,100,90),banner.getBounds());assertFalse(source.isRecycled());
    }

    @Test public void flatBrandColorFillsThePillAndTheMarkKeepsItsThreePixelMargin(){
        Bitmap source=bitmap(192,192,0xffb71248,Color.WHITE,45,70,147,122);
        LauncherinAppArtwork.Mark mark=LauncherinAppArtwork.of(resources,new BitmapDrawable(resources,source),"sample.brand");
        assertArrayEquals(new int[]{0xffb71248,0xffb71248},mark.background);
        Bitmap image=result(mark);assertEquals(108,image.getWidth());assertEquals(58,image.getHeight());
        assertEquals(Color.WHITE,image.getPixel(3,3));assertEquals(0xffb71248,image.getPixel(0,0));
    }

    @Test public void transparentIconKeepsItsOriginalForegroundAndTransparentMargin(){
        Bitmap source=bitmap(192,192,Color.TRANSPARENT,0xff10ba79,30,60,162,132);
        LauncherinAppArtwork.Mark mark=LauncherinAppArtwork.of(resources,new BitmapDrawable(resources,source),"sample.transparent");
        assertNull(mark.background);Bitmap image=result(mark);
        assertEquals(138,image.getWidth());assertEquals(78,image.getHeight());
        assertEquals(Color.TRANSPARENT,image.getPixel(0,0));assertEquals(0xff10ba79,image.getPixel(3,3));
    }

    @Test public void disneyGradientExtractionPreservesTheWhiteMarkAndTransparentSurroundings(){
        int n=192;int[] pixels=new int[n*n];
        for(int y=0;y<n;y++)for(int x=0;x<n;x++)pixels[y*n+x]=Color.rgb(0,20+y/2,60+y/2);
        for(int y=80;y<112;y++)Arrays.fill(pixels,y*n+40,y*n+152,Color.WHITE);
        Bitmap source=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888);source.setDensity(Bitmap.DENSITY_NONE);
        source.setPixels(pixels,0,n,0,0,n,n);
        LauncherinAppArtwork.Mark mark=LauncherinAppArtwork.of(resources,new BitmapDrawable(resources,source),"com.disney.disneyplus");
        assertNull(mark.background);Bitmap image=result(mark);
        assertEquals(118,image.getWidth());assertEquals(38,image.getHeight());
        assertEquals(Color.TRANSPARENT,image.getPixel(0,0));assertEquals(Color.WHITE,image.getPixel(3,3));
    }
}

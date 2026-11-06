package app.launcherin.tv;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

/** Reattached app cards must not request redundant layouts, but still resize with their viewport. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=35)
public class AppDockLayoutAndroidTest {
    static class CountedFrame extends FrameLayout {
        int params;
        CountedFrame(Context context){super(context);}
        @Override public void setLayoutParams(ViewGroup.LayoutParams value){params++;super.setLayoutParams(value);}
    }
    @Test public void repeatedAttachKeepsTheSameGeometryWithoutNewLayoutRequests(){
        Context context=RuntimeEnvironment.getApplication();
        AppAdapter adapter=new AppAdapter(context,null,6,Config.load(context));
        FrameLayout parent=new FrameLayout(context);parent.setPadding(12,4,12,4);parent.layout(0,0,1920,240);
        CountedFrame row=new CountedFrame(context),card=new CountedFrame(context);card.setId(R.id.card);
        row.addView(card,new FrameLayout.LayoutParams(180,101));
        for(int id:new int[]{R.id.banner,R.id.icon}){ImageView image=new ImageView(context);image.setId(id);card.addView(image);}
        TextView label=new TextView(context);label.setId(R.id.label);card.addView(label);
        TextView name=new TextView(context);name.setId(R.id.name);row.addView(name);
        parent.addView(row,new FrameLayout.LayoutParams(1,1));
        AppAdapter.Holder holder=new AppAdapter.Holder(row,null,true,false,false,0,false);
        adapter.onViewAttachedToWindow(holder);
        int rowWrites=row.params,cardWrites=card.params,width=card.getLayoutParams().width;
        assertEquals(316,row.getLayoutParams().width);assertTrue(width>0);assertTrue(width<=316);
        adapter.onViewAttachedToWindow(holder);adapter.onViewAttachedToWindow(holder);
        assertEquals(rowWrites,row.params);assertEquals(cardWrites,card.params);
        parent.layout(0,0,1280,190);adapter.onViewAttachedToWindow(holder);
        assertEquals(209,row.getLayoutParams().width);assertTrue(card.getLayoutParams().width<width);
        assertTrue(row.params>rowWrites);assertTrue(card.params>cardWrites);
    }
}

package app.launcherin.tv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Exactly one visible title: text remains until a real logo has decoded successfully. */
final class LauncherinTitle extends FrameLayout {
    final TextView text;
    final ImageView image;
    LauncherinTitle(Context context,String title){
        super(context);
        text=new TextView(context);text.setText(title);text.setTextColor(Color.WHITE);text.setTextSize(15);
        text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));text.setIncludeFontPadding(false);
        text.setSingleLine(true);text.setEllipsize(TextUtils.TruncateAt.END);text.setGravity(Gravity.BOTTOM);
        addView(text,new LayoutParams(-1,-1));
        image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_START);image.setVisibility(View.GONE);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);addView(image,new LayoutParams(-1,-1));
    }
    void showLoadedLogo(){
        boolean loaded=image.getDrawable()!=null&&image.getDrawable().getIntrinsicWidth()>0&&image.getDrawable().getIntrinsicHeight()>0;
        text.setVisibility(loaded?View.GONE:View.VISIBLE);image.setVisibility(loaded?View.VISIBLE:View.GONE);
    }
}

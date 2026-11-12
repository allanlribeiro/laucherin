// SPDX-License-Identifier: GPL-3.0-or-later
package app.launcherin.tv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared native menus with the current screen's glass surfaces. Keeps dialog semantics and callbacks. */
final class LauncherinDialogs {
    static final class Builder extends AlertDialog.Builder {
        final Context host;final boolean music;CharSequence title;java.util.List<LauncherinMusicProfiles.Profile> profileIcons=java.util.Collections.emptyList();
        Builder(Context c){super(c,R.style.LauncherinDialogTheme);host=c;music=c instanceof LauncherinPlayerActivity;}
        @Override public Builder setTitle(CharSequence value){title=value;super.setTitle(value);return this;}
        @Override public Builder setTitle(int value){return setTitle(host.getText(value));}
        Builder profileIcons(java.util.List<LauncherinMusicProfiles.Profile> rows){profileIcons=rows;return this;}
        @Override public AlertDialog create(){
            if(title==null&&music)return super.create();
            Palette p=new Palette(host,music);p.profiles=profileIcons;
            if(title!=null){TextView heading=new TextView(host);heading.setText(title);heading.setTextColor(p.text);heading.setTextSize(21);heading.setTypeface(p.skin.font,Typeface.BOLD);heading.setPadding(p.dp(24),p.dp(22),p.dp(24),p.dp(15));heading.setMaxLines(3);super.setCustomTitle(heading);}
            AlertDialog d=super.create();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawable(p.panel());w.setDimAmount(.58f);}
            View decor=w==null?null:w.getDecorView();if(decor!=null)decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){v.removeOnAttachStateChangeListener(this);v.post(()->style(d,p));}public void onViewDetachedFromWindow(View v){}});
            return d;
        }
        @Override public AlertDialog show(){AlertDialog d=create();d.show();return d;}
    }
    static final class Palette {
        java.util.List<LauncherinMusicProfiles.Profile> profiles=java.util.Collections.emptyList();final java.util.Map<TextView,String> boundIcons=new java.util.WeakHashMap<>();
        final Context c;final LauncherinAppearance.Style skin;final boolean music;final int text,accent,base;
        Palette(Context c,boolean music){this.c=c;this.music=music;skin=LauncherinAppearance.get(c);text=music?LauncherinMusicDesign.TEXT:skin.text;accent=music?LauncherinMusicDesign.VIOLET:skin.accent;base=music?0xff1c233a:skin.surface;}
        int dp(float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
        Drawable panel(){return music?new LauncherinMusicDesign(c).glass(false,22,true):new LauncherinSurface(c,20,false,false,false,22);}

        Drawable choice(boolean focus){if(music)return new LauncherinMusicDesign(c).glass(focus,12,focus);GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{LauncherinAppearance.blend(base,accent,focus?.34f:.08f),LauncherinAppearance.blend(base,accent,focus?.16f:.03f)});g.setCornerRadius(dp(12));g.setStroke(dp(focus?2:1),focus?accent:LauncherinAppearance.blend(base,accent,.25f));return g;}
        Drawable states(){StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_focused},choice(true));d.addState(new int[]{android.R.attr.state_pressed},choice(true));d.addState(new int[]{android.R.attr.state_checked},choice(true));d.addState(new int[]{},choice(false));return d;}
    }
    static void stylePopup(View root,Context context){Palette p=new Palette(context,false);root.setBackground(p.panel());root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){v.removeOnAttachStateChangeListener(this);v.post(()->popupRows(root,p));}public void onViewDetachedFromWindow(View v){}});}
    private static void popupRows(View view,Palette p){if(view.isFocusable())view.setBackground(p.states());if(view instanceof TextView)((TextView)view).setTypeface(p.skin.font);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)popupRows(((ViewGroup)view).getChildAt(i),p);}
    private static void style(AlertDialog d,Palette p){
        if(!d.isShowing())return;Window w=d.getWindow();if(w==null)return;
        embedded(w.getDecorView(),p);
        int width=Math.min(p.dp(660),(int)(p.c.getResources().getDisplayMetrics().widthPixels*.86f));w.setLayout(width,ViewGroup.LayoutParams.WRAP_CONTENT);w.setBackgroundDrawable(p.panel());
        TextView message=d.findViewById(android.R.id.message);if(message!=null){message.setTextColor(p.text);message.setTextSize(16);message.setTypeface(p.skin.font);message.setLineSpacing(p.dp(4),1);}
        ListView list=d.getListView();if(list!=null){list.setSelector(p.states());list.setDrawSelectorOnTop(false);list.setDivider(new ColorDrawable(Color.TRANSPARENT));list.setDividerHeight(p.dp(5));list.setPadding(p.dp(16),p.dp(5),p.dp(16),p.dp(8));list.setClipToPadding(false);styleRows(list,p);list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int s){}public void onScroll(AbsListView v,int first,int count,int total){styleRows(list,p);}});}
        for(int id:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){Button b=d.getButton(id);if(b==null)continue;b.setAllCaps(false);b.setTextColor(p.text);b.setTextSize(15);b.setTypeface(p.skin.font);b.setMinHeight(p.dp(44));b.setPadding(p.dp(19),p.dp(8),p.dp(19),p.dp(8));b.setBackground(p.states());}
        if(list!=null&&list.getCount()>0&&!list.hasFocus()){list.requestFocus();if(list.getSelectedItemPosition()<0)list.setSelection(Math.max(0,list.getCheckedItemPosition()));}
    }
    private static void embedded(View view,Palette p){
        if(view instanceof TextView){TextView t=(TextView)view;t.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{LauncherinAppearance.blend(p.text,p.base,.55f),p.text}));t.setTypeface(p.skin.font,t.getTypeface()==null?Typeface.NORMAL:t.getTypeface().getStyle());if(t instanceof EditText)((EditText)t).setHintTextColor(p.music?LauncherinMusicDesign.MUTED:p.skin.muted);}
        if(view instanceof CompoundButton)((CompoundButton)view).setButtonTintList(ColorStateList.valueOf(p.accent));
        if(view instanceof SeekBar){((SeekBar)view).setProgressTintList(ColorStateList.valueOf(p.accent));((SeekBar)view).setThumbTintList(ColorStateList.valueOf(p.accent));}
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)embedded(((ViewGroup)view).getChildAt(i),p);
    }
    private static void styleRows(ListView list,Palette p){for(int i=0;i<list.getChildCount();i++){View row=list.getChildAt(i);styleText(row,p);int at=list.getFirstVisiblePosition()+i;if(row instanceof TextView&&!p.profiles.isEmpty()){TextView text=(TextView)row;String key=at<p.profiles.size()?p.profiles.get(at).id+":"+p.profiles.get(at).photo:"action:"+at;if(!key.equals(p.boundIcons.get(text))){p.boundIcons.put(text,key);Drawable icon=at<p.profiles.size()?LauncherinProfilePhotos.avatar(p.c,p.profiles.get(at),96):null;if(icon==null)icon=new LauncherinMusicDesign.Icon(at<p.profiles.size()?"artist":at==p.profiles.size()?"settings":"add",p.accent,p.dp(32));icon.setBounds(0,0,p.dp(32),p.dp(32));text.setCompoundDrawablesRelative(icon,null,null,null);text.setCompoundDrawablePadding(p.dp(14));}}}}
    private static void styleText(View v,Palette p){if(v instanceof TextView){if("launcherin-menu-styled".equals(v.getTag()))return;v.setTag("launcherin-menu-styled");TextView t=(TextView)v;t.setTextColor(p.text);t.setTypeface(p.skin.font);t.setTextSize(17);t.setMinHeight(p.dp(49));t.setPadding(p.dp(17),p.dp(10),p.dp(17),p.dp(10));if(t instanceof CheckedTextView)((CheckedTextView)t).setCheckMarkTintList(ColorStateList.valueOf(p.accent));}else if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)styleText(((ViewGroup)v).getChildAt(i),p);}
}

package app.launcherin.tv;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Lets tools/theme.sh write the theme files into {@link Config#dir} on firmware where the adb shell is
 * locked out of Android/data (TCL G07, Android 11). Only the files {@link ThemeFiles} allows, and only for
 * callers holding WRITE_SECURE_SETTINGS, which the adb shell has and ordinary apps cannot get.
 *
 *   content write --uri content://app.launcherin.tv.theme/config.json < /data/local/tmp/config.json
 *   content write --uri content://app.launcherin.tv.theme/wallpapers/dusk.jpg < /data/local/tmp/dusk.jpg
 *   content delete --uri content://app.launcherin.tv.theme/config.json
 *   content delete --uri content://app.launcherin.tv.theme/wallpapers      (every image in the collection)
 */
public class ThemeProvider extends ContentProvider {

    private File file(Uri uri) throws FileNotFoundException {
        if(uri.getPathSegments().size()==1&&LauncherinProfilePhotos.incoming(uri.getLastPathSegment()))return LauncherinProfilePhotos.incoming(getContext(),uri.getLastPathSegment());
        if(uri.getPathSegments().size()==1&&LauncherinVisualOptions.PENDING.equals(uri.getLastPathSegment()))return LauncherinVisualOptions.pending(getContext());
        if(uri.getPathSegments().size()==1&&LauncherinAppearance.pending(uri.getLastPathSegment()))try{return LauncherinAppearance.incoming(getContext(),uri.getLastPathSegment());}catch(java.io.IOException e){throw new FileNotFoundException("Arquivo visual indisponível");}
        String path = ThemeFiles.path(uri.getPathSegments());
        File dir = Config.dir(getContext());
        if (dir == null || path == null) throw new FileNotFoundException(String.valueOf(uri));
        File f = new File(dir, path);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new FileNotFoundException(String.valueOf(uri));
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = file(uri);
        if (mode.contains("w")) {
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File dir = Config.dir(getContext());
        if (dir != null && ThemeFiles.isCollection(uri.getPathSegments())) {
            File[] images = new File(dir, ThemeFiles.WALLPAPERS).listFiles((d, name) -> ThemeFiles.collectionImage(name));
            int n = 0;
            if (images != null) for (File f : images) if (f.delete()) n++;
            return n;
        }
        try {
            return file(uri).delete() ? 1 : 0;
        } catch (FileNotFoundException e) {
            return 0;
        }
    }

    @Override public android.os.Bundle call(String method,String arg,android.os.Bundle extras) {
        if(android.os.Binder.getCallingUid()!=android.os.Process.myUid())getContext().enforceCallingPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS,"Agenda access");
        if("musicProfilesStatus".equals(method)||"commitMusicProfile".equals(method))return LauncherinProfilePhotos.call(getContext(),method,arg);
        if("musicVisualStatus".equals(method)||"commitMusicVisual".equals(method))return LauncherinVisualOptions.call(getContext(),method);
        if("commitAppearance".equals(method)||"appearanceStatus".equals(method))return LauncherinAppearance.call(getContext(),method);
        android.os.Bundle result=new android.os.Bundle();
        if(!"commitAgenda".equals(method)){result.putBoolean("ok",false);return result;}
        try{LauncherinAgendaData.commit(getContext());result.putBoolean("ok",true);}catch(java.io.IOException error){result.putBoolean("ok",false);}
        return result;
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}

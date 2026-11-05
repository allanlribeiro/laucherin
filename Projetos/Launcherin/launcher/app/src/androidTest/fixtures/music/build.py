from pathlib import Path
import subprocess,os,zipfile
sources=Path(__file__).parent
root=Path(os.environ.get('LAUNCHERIN_FIXTURE_OUTPUT','/tmp/launcherin-music-fixture'));root.mkdir(parents=True,exist_ok=True)
sdk=Path(os.environ['ANDROID_SDK_ROOT']);bt=sdk/'build-tools/35.0.0';jar=sdk/'platforms/android-36/android.jar'
java=Path(os.environ['JAVA_HOME']);key=Path(os.environ['LAUNCHERIN_TEST_KEYSTORE']);env=dict(os.environ)
for name in ['MusicService.java','TunerTrap.java','MediaReceiver.java','PrivateResume.java']:
 if (sources/name).resolve()!=(root/name).resolve():(root/name).write_bytes((sources/name).read_bytes())
def run(args):subprocess.run([str(a) for a in args],check=True,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
(root/'classes').mkdir(exist_ok=True);(root/'dex').mkdir(exist_ok=True)
run([java/'bin/javac','-source','8','-target','8','-classpath',jar,'-d',root/'classes',root/'MusicService.java',root/'TunerTrap.java',root/'MediaReceiver.java',root/'PrivateResume.java'])
run([bt/'d8','--lib',jar,'--min-api','26','--output',root/'dex',*sorted((root/'classes').rglob('*.class'))])
for pkg in ['com.spotify.music','com.google.android.youtube.tv.music','com.globo.globotv']:
 manifest='<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="'+pkg+'"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="28"/><uses-permission android:name="android.permission.FOREGROUND_SERVICE"/><application android:label="Fixture de teste (não é streaming real)"><service android:name="app.launcherin.fixture.MusicService" android:exported="true"><intent-filter><action android:name="android.media.browse.MediaBrowserService"/></intent-filter></service><receiver android:name="app.launcherin.fixture.PrivateResume" android:exported="false"/><receiver android:name="app.launcherin.fixture.MediaReceiver" android:exported="true"><intent-filter><action android:name="android.intent.action.MEDIA_BUTTON"/></intent-filter></receiver><activity android:name="app.launcherin.fixture.TunerTrap" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LEANBACK_LAUNCHER"/></intent-filter><intent-filter><action android:name="android.intent.action.VIEW"/><category android:name="android.intent.category.DEFAULT"/><data android:scheme="content" android:host="android.media.tv" android:pathPrefix="/channel"/></intent-filter></activity></application></manifest>'
 (root/'AndroidManifest.xml').write_text(manifest)
 run([bt/'aapt2','link','--manifest',root/'AndroidManifest.xml','-I',jar,'-o',root/'unsigned.apk'])
 with zipfile.ZipFile(root/'unsigned.apk','a') as z:z.write(root/'dex/classes.dex','classes.dex')
 run([bt/'zipalign','-f','4',root/'unsigned.apk',root/'aligned.apk'])
 run([bt/'apksigner','sign','--ks',key,'--ks-key-alias','launcherin','--ks-pass','pass:android','--key-pass','pass:android','--out',root/(pkg+'.apk'),root/'aligned.apk'])
 print('Built isolated emulator fixture:',pkg,flush=True)

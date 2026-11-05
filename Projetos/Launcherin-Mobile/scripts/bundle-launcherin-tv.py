#!/usr/bin/env python3
"""Bundle a locally built Launcherin TV APK, preserving the existing certificate pin."""
from pathlib import Path
import argparse,hashlib,os,re,shutil,subprocess
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk',type=Path,default=root.parent/'launcherin/launcher/app/build/outputs/apk/release/app-release.apk')
parser.add_argument('--sdk',type=Path,default=Path(os.environ.get('ANDROID_SDK_ROOT',os.environ.get('ANDROID_HOME','/workspace/android-sdk'))))
args=parser.parse_args();apk=args.apk.resolve();tools=args.sdk/'build-tools/35.0.0'
if not apk.is_file():raise SystemExit('Compile o APK release da TV antes de empacotar.')
badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True).splitlines()[0]
identity=re.search(r"name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
if not identity or identity.group(1)!='app.launcherin.tv':raise SystemExit('O APK não pertence à Launcherin TV.')
signing=subprocess.check_output([str(tools/'apksigner'),'verify','--print-certs',str(apk)],text=True)
cert=re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',signing).group(1)
pin=root/'core/trust/src/main/kotlin/tv/cleanyour/core/trust/Pins.kt';source=pin.read_text();start=source.index('object PinnedLauncher {');end=source.index('\n}',start)+2;block=source[start:end]
previous=re.search(r'const val SIGNER_SHA256 = "([0-9a-f]+)"',block).group(1)
if cert!=previous:raise SystemExit('A assinatura mudou. Preserve a chave já usada pela Launcherin.')
old_code=int(re.search(r'const val VERSION_CODE = (\d+)L',block).group(1))
if int(identity.group(2))<old_code:raise SystemExit('O APK tem uma versão inferior à já fixada.')
values={'PACKAGE':'"'+identity.group(1)+'"','VERSION_CODE':identity.group(2)+'L','VERSION_NAME':'"'+identity.group(3)+'"','SIZE':str(apk.stat().st_size)+'L','SHA256':'"'+hashlib.sha256(apk.read_bytes()).hexdigest()+'"','SIGNER_SHA256':'"'+cert+'"'}
for key,value in values.items():
 block,n=re.subn(r'(const val '+key+r' = )[^\n]+',lambda m:m.group(1)+value,block)
 if n!=1:raise SystemExit('Pin inválido: '+key)
asset=root/'android/platform/src/main/assets/bundled/cleanyourtv-launcher.apk';temporary=asset.with_suffix('.apk.tmp')
shutil.copy2(apk,temporary);os.replace(temporary,asset);pin.write_text(source[:start]+block+source[end:])
print('Launcherin TV incluída:',identity.group(3),'código',identity.group(2),'-',apk.stat().st_size,'bytes')

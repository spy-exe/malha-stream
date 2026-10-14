"""Run as development user in dedicated CT, after checking PVE free capacity."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

root=Path('/home/malha/android-sdk')
if os.geteuid()==0 or not Path.home().samefile('/home/malha'):
    raise SystemExit('Run as user malha inside dedicated CT')
root.mkdir(exist_ok=True)
metadata=ET.fromstring(urllib.request.urlopen('https://dl.google.com/android/repository/repository2-3.xml',timeout=30).read())
package=next(item for item in metadata.iter('remotePackage') if item.attrib['path']=='cmdline-tools;23.0')
archive=next(item for item in package.findall('archives/archive') if item.findtext('host-os')=='linux')
complete=archive.find('complete')
name=complete.findtext('url')
checksum=complete.find('checksum')
expected=checksum.text
algorithm=checksum.attrib.get('type','sha1')
download=root/'commandline-tools.zip'
urllib.request.urlretrieve('https://dl.google.com/android/repository/'+name,download)
with download.open('rb') as stream:actual=hashlib.file_digest(stream,algorithm).hexdigest()
if actual!=expected:raise SystemExit('Official command-line tools checksum mismatch')
target=root/'cmdline-tools/latest'
if not target.exists():
    staging=root/'.tools-staging'
    with zipfile.ZipFile(download) as archive:
        for item in archive.infolist():
            if not (staging/item.filename).resolve().is_relative_to(staging.resolve()):
                raise SystemExit('Unsafe SDK archive path')
        archive.extractall(staging)
    target.parent.mkdir(exist_ok=True)
    shutil.move(str(staging/'cmdline-tools'),target)
    shutil.rmtree(staging)
for file in (target/'bin').iterdir():file.chmod(file.stat().st_mode|0o111)
download.unlink()
env=dict(os.environ,ANDROID_HOME=str(root),ANDROID_SDK_ROOT=str(root))
manager=str(target/'bin/sdkmanager')
subprocess.run([manager,'--licenses'],input='y\n'*200,text=True,env=env,check=True,stdout=subprocess.DEVNULL)
subprocess.run([manager,'platform-tools','platforms;android-37.0','build-tools;36.0.0','ndk;29.0.14206865'],env=env,check=True)
print('Pinned Android SDK/NDK ready; Gradle wrapper supplies Gradle 9.7.1.')

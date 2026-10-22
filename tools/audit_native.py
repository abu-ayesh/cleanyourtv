"""Local static audit of the new app only, plus read-only source integrity checks."""
from pathlib import Path
import json, hashlib, zipfile, re, sys
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.tooling/python'))
from fontTools.ttLib import TTFont
NS='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
permissions=[n.get(NS+'name') for n in manifest.findall('uses-permission')]
assert not permissions, permissions
categories=[n.get(NS+'name') for n in manifest.findall('.//intent-filter/category')]
assert 'android.intent.category.HOME' not in categories
java=list((ROOT/'app/src/main/java').rglob('*.java'))
for p in java:
    assert not re.search(r'WebView|android\.webkit|JavascriptInterface|ReactNative|androidx\.compose',p.read_text(encoding='utf-8')),p
source=json.loads((ROOT.parent/'mihrab-html/assets/content.js').read_text(encoding='utf-8').split('window.MIHRAB_CONTENT = ',1)[1].rstrip().removesuffix(';'))
native=json.loads((ROOT/'app/src/main/assets/content.json').read_text(encoding='utf-8'))
assert source==native
fonts={}
for path in (ROOT/'app/src/main/res/font').glob('*.ttf'):
    font=TTFont(path)
    assert 0x0627 in font.getBestCmap()
    fonts[path.name]={'bytes':path.stat().st_size,'glyphs':len(font.getGlyphOrder()),'fsType':font['OS/2'].fsType}
frozen=json.loads((ROOT/'validation/source-hashes.json').read_text(encoding='utf-8'))
changes=[name for name,expected in frozen.items() if not (ROOT.parent/name).is_file() or hashlib.sha256((ROOT.parent/name).read_bytes()).hexdigest()!=expected]
assert not changes,changes
apk=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
apk_data={}
if apk.exists():
    with zipfile.ZipFile(apk) as archive:
        names=archive.namelist()
        assert not any(n.endswith(('.html','.js','.woff2','.so')) for n in names)
        assert not any(b'android/webkit/WebView' in archive.read(n) for n in names if n.endswith('.dex'))
        apk_data={'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'nativeLibraries':0,'entries':len(names)}
result={'permissions':permissions,'categories':categories,'javaFiles':len(java),'fonts':fonts,'contentExactMatch':True,'sourcesUnchanged':len(frozen),'apk':apk_data}
(ROOT/'validation/native-audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=True,indent=2))

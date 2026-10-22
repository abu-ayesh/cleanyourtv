"""Freeze a concise local QA summary and a side-by-side visual comparison."""
from pathlib import Path
import sys, json, xml.etree.ElementTree as ET
from collections import Counter
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.tooling/python'))
from PIL import Image, ImageDraw
def focused(file):
    return [dict(n.attrib) for n in ET.parse(ROOT/'validation'/file).iter('node') if n.get('focused')=='true']
assert focused('emulator-initial.xml')[0]['resource-id'].endswith('/nav_home')
assert focused('emulator-restore.xml')[0]['resource-id'].endswith('/hadith_detail')
assert focused('emulator-app-return.xml')[0]['content-desc']=='Clock'
assert {n.get('package') for n in ET.parse(ROOT/'validation/emulator-clock.xml').iter('node')}=={'com.google.android.deskclock'}
assert focused('emulator-page-right1.xml')[0]['content-desc']=='Google'
assert focused('emulator-page-right2.xml')[0]['content-desc']=='Gmail'
labels={'قوس من نور','ليلة رمضانية','سكينة الغابة','ذهب الغروب','فانوس السحاب','دفء الحكاية'}
gallery=[n for n in ET.parse(ROOT/'validation/emulator-gallery.xml').iter('node') if n.get('content-desc') in labels]
assert len(gallery)==6 and len({n.get('bounds') for n in gallery})==6
assert ET.parse(ROOT/'validation/emulator-home-1280.xml').find('node').get('bounds')=='[0,0][1280,720]'
# SurfaceFlinger screenshots retain the emulator panel's physical 1920x1080 size even
# when wm size changes the logical viewport. Preserve that source and export at 720p.
small=ROOT/'validation/native-home-1280.png'
if Image.open(small).size!=(1280,720):
    image=Image.open(small).convert('RGB')
    image.save(ROOT/'validation/native-home-1280-physical.png')
    image.resize((1280,720),Image.Resampling.LANCZOS).save(small)
for name, size in [('native-home-1920.png',(1920,1080)),('native-home-1280.png',(1280,720))]:
    assert Image.open(ROOT/'validation'/name).size==size
tests=[]
for p in sorted((ROOT/'app/build/test-results/testDebugUnitTest').glob('*.xml')):
    t=ET.parse(p).getroot();tests.append({k:t.get(k) for k in ['name','tests','failures','errors']})
assert sum(int(t['tests']) for t in tests)==13
assert all(int(t['failures'])==int(t['errors'])==0 for t in tests)
issues=ET.parse(ROOT/'app/build/reports/lint-results-debug.xml').findall('issue')
counts=Counter(i.get('severity') for i in issues)
assert counts['Error']==counts['Fatal']==0
summary={'tests':tests,'lint':dict(counts),'runtime':{'environment':'PC emulator: Android 17 / API 37 / x86_64','resolutions':['1920x1080','1280x720'],'initialFocus':True,'hadithFocusRestored':True,'realAppLaunchAndReturn':'com.google.android.deskclock / Clock','gallerySixDistinctTiles':True,'dockPageReturn':'Google to Gmail using RIGHT at the page boundary','tvTested':False}}
(ROOT/'validation/qa-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
out=Image.new('RGB',(1920,576),'#0b1320');draw=ImageDraw.Draw(out)
for x,name,label in [(0,'html-reference-1920.png','Approved HTML reference'),(960,'native-home-1920.png','Native Android - local emulator')]:
    out.paste(Image.open(ROOT/'validation'/name).convert('RGB').resize((960,540)),(x,36));draw.text((x+16,12),label,fill='#e9ddc4')
out.save(ROOT/'validation/html-native-comparison.jpg',quality=92)
print(json.dumps(summary,ensure_ascii=True))

"""Read approved sources only; freeze Android compatible resources in this fork.
Run: python -m pip install --target .tooling/python fonttools brotli pillow
     python tools/prepare_native_assets.py
"""
from pathlib import Path
import sys, json, hashlib, shutil, re, xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / '.tooling/python'))
from fontTools.ttLib import TTFont
from PIL import Image, ImageFilter, ImageOps
SOURCE = ROOT.parent / 'mihrab-html'
RES = ROOT / 'app/src/main/res'
ASSETS = ROOT / 'app/src/main/assets'
for d in [RES/'font', RES/'drawable-nodpi', RES/'drawable', ASSETS, ROOT/'validation']:
    d.mkdir(parents=True, exist_ok=True)
for family, name in [('text','mihrab_text'), ('display','mihrab_display')]:
    for weight in ['Regular','Bold']:
        original = SOURCE/f'assets/font/thmanyahserif{family}/thmanyahserif{family}-{weight}.woff2'
        font = TTFont(original); font.flavor = None
        font.save(RES/'font'/f'{name}{"_bold" if weight=="Bold" else ""}.ttf')
positions = [.54,.56,.55,.52,.5,.54]
for n, position in enumerate(positions, 1):
    original = SOURCE/f'assets/background/{n}.jpg'
    shutil.copy2(original, RES/'drawable-nodpi'/f'wallpaper_{n}.jpg')
    # Blur behind glass is frozen at 1/2 resolution, no runtime blur on API 30.
    img = Image.open(original).convert('RGB')
    img = ImageOps.fit(img, (800,450), centering=(.5,position))
    img.filter(ImageFilter.GaussianBlur(10)).save(RES/'drawable-nodpi'/f'glass_{n}.jpg', quality=92)
content = (SOURCE/'assets/content.js').read_text(encoding='utf-8')
data = json.loads(content[content.index('{'):].rstrip().removesuffix(';'))
(ASSETS/'content.json').write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
ImageOps.fit(Image.open(SOURCE/'assets/background/5.jpg'), (320,180)).save(RES/'drawable-nodpi/tv_banner.jpg', quality=90)
html = (SOURCE/'mihrab.html').read_text(encoding='utf-8')
android = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', android)
for match in re.finditer(r'<symbol id="([^"]+)" viewBox="([^"]+)">(.*?)</symbol>',html,re.S):
    name, viewport, body = match.groups()
    _, _, width, height = viewport.split()
    vector = ET.Element('vector', {f'{{{android}}}width':'22dp',f'{{{android}}}height':'22dp',f'{{{android}}}viewportWidth':width,f'{{{android}}}viewportHeight':height})
    for item in ET.fromstring('<svg>'+body+'</svg>'):
        a=item.attrib; path=a.get('d')
        if item.tag=='circle':
            x,y,r=map(float,(a['cx'],a['cy'],a['r']))
            path=f'M{x-r},{y} a{r},{r} 0,1 0,{2*r},0 a{r},{r} 0,1 0,{-2*r},0'
        elif item.tag=='rect':
            x,y,w,h=map(float,(a['x'],a['y'],a['width'],a['height']))
            path=f'M{x},{y} h{w} v{h} h{-w} Z'
        if path: ET.SubElement(vector,'path',{f'{{{android}}}pathData':path,f'{{{android}}}fillColor':'#00000000',f'{{{android}}}strokeColor':'#d5c5a4',f'{{{android}}}strokeWidth':'1.5',f'{{{android}}}strokeLineCap':'round',f'{{{android}}}strokeLineJoin':'round'})
    ET.ElementTree(vector).write(RES/'drawable'/f'ic_{name}.xml',encoding='utf-8',xml_declaration=True)
snapshot = {}
for folder in ['mihrab-html', 'cleanyourtv']:
    for p in sorted((ROOT.parent/folder).rglob('*')):
        if p.is_file() and '.git' not in p.parts and not any(s in p.parts for s in ['build','.gradle']):
            snapshot[str(p.relative_to(ROOT.parent)).replace('\\','/')] = hashlib.sha256(p.read_bytes()).hexdigest()
(ROOT/'validation/source-hashes.json').write_text(json.dumps(snapshot, indent=2), encoding='utf-8')
print('Imported 6 original wallpapers, 6 blurred glass maps, 4 fonts and unmodified source content.')

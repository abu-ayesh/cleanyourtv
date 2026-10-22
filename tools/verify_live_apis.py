"""Read public primary APIs, save response evidence outside application assets."""
from pathlib import Path
import urllib.request,json,datetime
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'validation/api';DEST.mkdir(parents=True,exist_ok=True)
urls={'reciters':'https://mp3quran.net/api/v3/reciters?language=ar','suwar':'https://mp3quran.net/api/v3/suwar?language=ar','hadith':'https://hadeethenc.com/api/v1/hadeeths/one/?language=ar&id=4792','prayer':'https://api.aladhan.com/v1/timings/22-10-2026?latitude=31.5326&longitude=35.0998&method=3&iso8601=true&timezonestring=Asia%2FHebron'}
results={}
for key,url in urls.items():
    try:
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'MIHRAB-TV-local-validation/1.0'}),timeout=25) as response:
            raw=response.read(3_000_000);data=json.loads(raw)
            (DEST/f'{key}.json').write_bytes(raw)
            results[key]={'status':response.status,'url':response.url,'bytes':len(raw),'keys':list(data)}
    except Exception as e:results[key]={'error':str(e)}
(DEST/'inspection.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
print(json.dumps(results,indent=2))
if (DEST/'reciters.json').exists():
    data=json.loads((DEST/'reciters.json').read_text(encoding='utf-8'))
    r=data['reciters'][0];m=r['moshaf'][0]
    print(json.dumps({'reciters':len(data['reciters']),'example':r},ensure_ascii=True)[:1800])
    url=m['server'].rstrip('/')+'/001.mp3'
    try:
        with urllib.request.urlopen(urllib.request.Request(url,headers={'Range':'bytes=0-4095'}),timeout=20) as response:
            sample=response.read(4096)
            (DEST/'audio-probe.json').write_text(json.dumps({'url':url,'status':response.status,'contentType':response.headers.get('Content-Type'),'sampleBytes':len(sample)},indent=2),encoding='utf-8')
    except Exception as e:print('Audio probe:',str(e))

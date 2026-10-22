"""PC-only QA. Fixed isolated server and serial; refuses every other target."""
from pathlib import Path
import argparse, subprocess, os, json, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
ADB='G:/Android/SDK/platform-tools/adb.exe'
env={**os.environ,'ADB_SERVER_SOCKET':'tcp:127.0.0.1:5041','ADB_MDNS_AUTO_CONNECT':'0'}
def adb(*args):
    return subprocess.run([ADB,'-P','5041','-s','emulator-5580',*args],capture_output=True,check=True,env=env).stdout
avd=adb('shell','getprop','ro.boot.qemu.avd_name').decode().strip()
assert avd=='Mihrab_Phase1' and adb('shell','getprop','ro.kernel.qemu').strip()==b'1', 'Refusing non-Mihrab local emulator'
p=argparse.ArgumentParser();p.add_argument('--install',action='store_true');p.add_argument('--start',action='store_true');p.add_argument('--capture');p.add_argument('--key',action='append',default=[]);p.add_argument('--dump');args=p.parse_args()
if args.install:print(adb('install','-r',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk')).decode())
if args.start:print(adb('shell','am','start','-n','com.mihrab.tv/.ui.home.HomeActivity').decode())
for key in args.key:
    assert key in ['UP','DOWN','LEFT','RIGHT','CENTER','BACK','ENTER'],key
    adb('shell','input','keyevent','KEYCODE_'+('DPAD_'+key if key in ['UP','DOWN','LEFT','RIGHT','CENTER'] else key))
if args.capture:
    target=ROOT/'validation'/args.capture
    assert target.suffix=='.png' and target.resolve().parent==ROOT/'validation'
    target.write_bytes(adb('exec-out','screencap','-p'))
    print('Captured '+target.name)
if args.dump:
    adb('shell','uiautomator','dump','/sdcard/mihrab-local-qa.xml')
    xml=adb('exec-out','cat','/sdcard/mihrab-local-qa.xml')
    target=ROOT/'validation'/args.dump
    assert target.resolve().parent==ROOT/'validation'
    target.write_bytes(xml)
    tree=ET.fromstring(xml)
    focused=[{k:node.get(k) for k in ['text','content-desc','resource-id','bounds']} for node in tree.iter('node') if node.get('focused')=='true']
    print(json.dumps({'focus':focused,'nodes':len(list(tree.iter('node')))},ensure_ascii=True))

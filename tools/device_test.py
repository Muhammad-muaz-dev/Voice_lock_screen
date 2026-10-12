import subprocess
import xml.etree.ElementTree as E
import re
import sys
from pathlib import Path
adb=r'C:\Users\TF\AppData\Local\Android\Sdk\platform-tools\adb.exe'
serial='adb-0E65315G341009A9-3NtFqN._adb-tls-connect._tcp'
def run(*args): return subprocess.run([adb,'-s',serial,*args],capture_output=True,check=True).stdout
def tree():
    run('shell','uiautomator','dump','/sdcard/voice_lock_ui.xml')
    return E.fromstring(run('shell','cat','/sdcard/voice_lock_ui.xml'))
root=tree()
nodes=[n for n in root.iter('node') if n.get('package')=='com.futurecode.voicelockscreen']
if not nodes: raise RuntimeError('Voice Lock is not in the foreground')
if len(sys.argv)>1:
    target=sys.argv[1]
    found=next((n for n in nodes if n.get('resource-id','').endswith(':id/'+target) or n.get('text')==target),None)
    if found is None:raise RuntimeError('No visible node: '+target)
    if found.get('enabled')!='true':raise RuntimeError('Node is disabled: '+target)
    x1,y1,x2,y2=map(int,re.findall(r'\d+',found.get('bounds')))
    run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    root=tree()
for n in root.iter('node'):
    if n.get('package')=='com.futurecode.voicelockscreen' and (n.get('text') or n.get('content-desc')):
        print(n.get('resource-id','').split('/')[-1],repr(n.get('text') or n.get('content-desc')),n.get('bounds'),n.get('enabled'))

from pathlib import Path
import subprocess
import sys

adb=r'C:\Users\TF\AppData\Local\Android\Sdk\platform-tools\adb.exe'
serial='adb-0E65315G341009A9-3NtFqN._adb-tls-connect._tcp'
out=Path('artifacts/device')
out.mkdir(parents=True,exist_ok=True)
name=sys.argv[1]
result=subprocess.run([adb,'-s',serial,'exec-out','screencap','-p'],capture_output=True,check=True)
out.joinpath(name+'.png').write_bytes(result.stdout)
print(out.joinpath(name+'.png').resolve())

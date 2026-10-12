from pathlib import Path
import xml.etree.ElementTree as E
A='http://schemas.android.com/apk/res/android';P='http://schemas.android.com/apk/res-auto'
E.register_namespace('android',A);E.register_namespace('app',P);E.register_namespace('tools','http://schemas.android.com/tools')
def a(k):return '{'+A+'}'+k
def p(k):return '{'+P+'}'+k
res=Path('app/src/main/res')
for path in (res/'layout').glob('*.xml'):
    root=E.parse(path).getroot()
    for e in root.iter():
        id=e.get(a('id'),'').split('/')[-1]
        if path.name=='fragment_home.xml':
            if id=='appBar':e.set(a('layout_height'),'wrap_content');e.set(a('minHeight'),'56dp')
            if id.startswith(('tvCamera','tvPin','tvPattern','tvFingerprint')):
                e.set(a('layout_width'),'0dp');e.set(p('layout_constraintEnd_toEndOf'),'parent')
        if path.name=='fragment_camera_voice.xml' and id=='tvTitle':
            e.set(a('layout_width'),'0dp');e.set(p('layout_constraintStart_toEndOf'),'@id/btnBack');e.set(a('layout_marginStart'),'12dp');e.set(a('layout_marginEnd'),'16dp')
        if id in ('btnConfirm','btnRecord') and e.tag=='ImageButton':
            e.set(a('layout_width'),'57dp');e.set(a('layout_height'),'57dp')
        if path.name in ('fragment_pin_lock.xml','dialog_pin_unlock.xml') and e.tag=='Button':
            e.set(a('background'),'@drawable/bg_pin_key');e.set(a('textSize'),'@dimen/text_titleL');e.set(a('lineHeight'),'28sp');e.set(p('lineHeight'),'28sp');e.set(a('minHeight'),'50dp')
            e.set(a('layout_height'),'wrap_content')
        if path.name=='fragment_voice_note.xml' and id=='recordCard':
            e.set(p('layout_constraintBottom_toTopOf'),'@id/btnRecord')
        if path.name=='fragment_voice_note.xml' and id=='rvNotes':
            e.set(p('layout_constraintBottom_toTopOf'),'@id/btnRecord')
    E.indent(root,space='    ');path.write_text(E.tostring(root,encoding='unicode'),encoding='utf-8')
print('Improved wrapping, header sizing, and keypad touch targets.')

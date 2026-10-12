from pathlib import Path
import xml.etree.ElementTree as E
A='http://schemas.android.com/apk/res/android';P='http://schemas.android.com/apk/res-auto'
E.register_namespace('android',A);E.register_namespace('app',P);E.register_namespace('tools','http://schemas.android.com/tools')
def a(k):return '{'+A+'}'+k
def p(k):return '{'+P+'}'+k
path=Path('app/src/main/res/layout/fragment_see_allthemes.xml');root=E.parse(path).getroot()
button=E.SubElement(root,'TextView',{a('id'):'@+id/btnUpload',a('layout_width'):'0dp',a('layout_height'):'50dp',a('layout_margin'):'16dp',a('text'):'@string/upload_picture',a('textColor'):'@color/textPrimary',a('textSize'):'@dimen/text_buttonL',a('fontFamily'):'@font/sora_bold',a('gravity'):'center',a('background'):'@drawable/bg_cta_gradient',p('layout_constraintTop_toBottomOf'):'@id/ivBack',p('layout_constraintStart_toStartOf'):'parent',p('layout_constraintEnd_toEndOf'):'parent'})
for e in root.iter():
    if e.get(a('id'))=='@+id/rvThemes':e.set(p('layout_constraintTop_toBottomOf'),'@id/btnUpload')
E.indent(root,space='    ');path.write_text(E.tostring(root,encoding='unicode'),encoding='utf-8')

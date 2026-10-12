from pathlib import Path
import xml.etree.ElementTree as E
A='http://schemas.android.com/apk/res/android';P='http://schemas.android.com/apk/res-auto'
E.register_namespace('android',A);E.register_namespace('app',P);E.register_namespace('tools','http://schemas.android.com/tools')
def a(k):return '{'+A+'}'+k
def save(path,root):
    E.indent(root,space='    ');path.write_text(E.tostring(root,encoding='unicode'),encoding='utf-8')
path=Path('app/src/main/res/layout/fragment_setting.xml');root=E.parse(path).getroot()
for e in root.iter():
    if e.get(a('id')) in ('@+id/llGeneralSettings','@+id/llOtherSettings'):
        inner=e[0]; inner.tag='LinearLayout';inner.set(a('orientation'),'vertical')
        for row in inner:
            for key in list(row.attrib):
                if 'layout_constraint' in key: del row.attrib[key]
            if row.tag.endswith('ConstraintLayout'):
                row.set(a('minHeight'),'66dp');row.set(a('paddingHorizontal'),'16dp')
    if e.get(a('id'),'').split('/')[-1] in ('tvLangTitle','tvSecurityTitle','tvDisableLockTitle','tvShareTitle','tvFeedbackTitle','tvPrivacyTitle'):
        e.set(a('textSize'),'@dimen/text_bodyM');e.set(a('fontFamily'),'@font/sora_semibold')
save(path,root)
path=Path('app/src/main/res/values/typography.xml');root=E.parse(path).getroot()
for style in root:
    for item in list(style):
        if item.get('name')=='android:lineHeight':style.remove(item)
save(path,root)

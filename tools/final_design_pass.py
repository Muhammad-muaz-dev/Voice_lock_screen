from pathlib import Path
import xml.etree.ElementTree as E
A='http://schemas.android.com/apk/res/android';P='http://schemas.android.com/apk/res-auto'
E.register_namespace('android',A);E.register_namespace('app',P);E.register_namespace('tools','http://schemas.android.com/tools')
def a(k):return '{'+A+'}'+k
def p(k):return '{'+P+'}'+k
res=Path('app/src/main/res')
def save(path,root):
    E.indent(root,space='    ');path.write_text('<?xml version="1.0" encoding="utf-8"?>\n'+E.tostring(root,encoding='unicode'),encoding='utf-8')
for path in (res/'layout').glob('*.xml'):
    root=E.parse(path).getroot()
    for e in root.iter():
        id=e.get(a('id'),'').split('/')[-1]
        if id=='brandIconContainer':e.set(a('layout_width'),'40dp');e.set(a('layout_height'),'40dp')
        if id in ('tvAppTitle','tvTitle') and path.name in ('fragment_home.xml','fragment_themes.xml','fragment_voice_note.xml'):
            e.set(a('fontFamily'),'@font/sora_semibold');e.set(a('textSize'),'@dimen/text_titleXL');e.set(a('lineHeight'),'28sp');e.set(p('lineHeight'),'28sp');e.attrib.pop(a('textStyle'),None)
        if id in ('tvSettingTitle','tvTitle') and path.name in ('fragment_setting.xml','fragment_camera_voice.xml','fragment_see_allthemes.xml'):
            e.set(a('textSize'),'@dimen/text_titleL');e.set(a('fontFamily'),'@font/sora_semibold')
        if id in ('btnPrimary','btnUseQuote','btnRecord','btnGoPremium') and e.tag=='TextView':
            e.set(a('layout_height'),'50dp');e.set(a('textSize'),'@dimen/text_buttonL');e.set(a('fontFamily'),'@font/sora_bold')
        if path.name=='fragment_camera_voice.xml' and id in ('contentColumn','btnPrimary'):e.set(a('paddingHorizontal'),'16dp');e.set(a('layout_marginHorizontal'),'16dp')
        if path.name=='fragment_premium.xml' and id=='btnPurchase':e.set(a('layout_height'),'56dp')
        if path.name=='fragment_premium.xml' and id=='tvTitleMain':
            e.set(a('textSize'),'@dimen/text_display');e.set(a('lineHeight'),'42sp');e.set(p('lineHeight'),'42sp');e.set(a('text'),'@string/go_pro_title')
        if path.name=='item_topic.xml':
            e.set(a('layout_height'),'wrap_content');e.set(a('minHeight'),'58dp');e.set(a('paddingVertical'),'16dp');e.set(a('textSize'),'@dimen/text_bodyL');e.set(a('layout_marginBottom'),'12dp')
        if path.name=='item_onboarding.xml' and e is root:e.set(a('paddingStart'),'16dp');e.set(a('paddingEnd'),'16dp')
    save(path,root)
path=res/'layout/fragment_setting.xml';root=E.parse(path).getroot();other=next(e for e in root.iter() if e.get(a('id'))=='@+id/llOtherSettings')
row=E.SubElement(other,'TextView',{a('id'):'@+id/btnRate',a('layout_width'):'match_parent',a('layout_height'):'66dp',a('paddingHorizontal'):'16dp',a('gravity'):'center_vertical',a('text'):'@string/rate_app',a('textSize'):'@dimen/text_bodyM',a('fontFamily'):'@font/sora_semibold',a('textColor'):'@color/textPrimary',a('drawableStart'):'@drawable/ic_crown_simple',a('drawablePadding'):'16dp'})
save(path,root)
print('Final component sizes and settings actions aligned.')

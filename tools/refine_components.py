from pathlib import Path
import xml.etree.ElementTree as E
A='http://schemas.android.com/apk/res/android'; P='http://schemas.android.com/apk/res-auto'
E.register_namespace('android',A); E.register_namespace('app',P); E.register_namespace('tools','http://schemas.android.com/tools')
res=Path('app/src/main/res')
def a(k): return '{'+A+'}'+k
def p(k): return '{'+P+'}'+k
def save(path,root):
    E.indent(root,space='    '); path.write_text('<?xml version="1.0" encoding="utf-8"?>\n'+E.tostring(root,encoding='unicode')+'\n',encoding='utf-8')
path=res/'layout/fragment_home.xml'; root=E.parse(path).getroot()
items={e.get(a('id'),'').split('/')[-1]:e for e in root.iter() if a('id') in e.attrib}
for id in ('cardUnlock','cardLock'):
    items[id].set(p('cardCornerRadius'),'@dimen/radius_xxl')
    items[id].set(a('layout_height'),'wrap_content')
    items[id].set(a('minHeight'),'168dp')
    items[id][0].set(a('layout_height'),'wrap_content')
    items[id][0].set(a('minHeight'),'168dp')
for id in ('badgeUnlock','badgeLock'):
    items[id].set(a('layout_width'),'44dp'); items[id].set(a('layout_height'),'44dp'); items[id].set(a('background'),'@drawable/bg_brand_icon_box')
for id in ('statusPillUnlock','statusPillLock'):
    e=items[id]; e.set(p('cardCornerRadius'),'100dp'); e.set(p('strokeWidth'),'1dp'); e.set(p('strokeColor'),'@color/borderStrong')
    e.set(a('layout_marginTop'),'16dp'); e.set(p('layout_constraintTop_toBottomOf'),'@id/'+('badgeUnlock' if 'Unlock' in id else 'badgeLock'))
    inner=e[0]; inner.set(a('minHeight'),'58dp')
    phrase=E.SubElement(inner,'TextView',{a('layout_width'):'0dp',a('layout_height'):'wrap_content',a('layout_marginTop'):'8dp',a('text'):'@string/'+('say_unlock_phrase' if 'Unlock' in id else 'say_lock_phrase'),a('fontFamily'):'@font/sora_regular',a('textSize'):'@dimen/text_caption',a('textColor'):'@color/textSecondary',p('layout_constraintTop_toBottomOf'):'@id/'+('tvUnlockStatus' if 'Unlock' in id else 'tvLockStatus'),p('layout_constraintStart_toStartOf'):'parent',p('layout_constraintEnd_toEndOf'):'parent'})
for id,string in [('tvCameraDesc','home_camera_hint'),('tvPinDesc','home_pin_hint'),('tvPatternDesc','home_pattern_hint'),('tvFingerprintDesc','home_fingerprint_hint')]:
    items[id].set(a('text'),'@string/'+string)
for id in ('badgeCamera','badgePin','badgePattern','badgeFingerprint'): items[id].attrib.pop(a('backgroundTint'),None)
for id in ('tvCameraTitle','tvPinTitle','tvPatternTitle','tvFingerprintTitle'): items[id].set(a('textSize'),'@dimen/text_buttonS'); items[id].set(a('fontFamily'),'@font/sora_bold'); items[id].set(a('layout_marginTop'),'8dp')
for id in ('cardCamera','pinLock','patternLock','fingerLock'):
    items[id].set(a('layout_height'),'wrap_content'); items[id].set(a('minHeight'),'112dp'); items[id][0].set(a('layout_height'),'wrap_content'); items[id][0].set(a('minHeight'),'112dp')
    last=items[id][0][-1]; last.set(p('layout_constraintBottom_toBottomOf'),'parent')
items['pinLock'].set(a('layout_marginStart'),'12dp'); items['fingerLock'].set(a('layout_marginStart'),'12dp')
items['patternLock'].set(a('layout_marginTop'),'12dp'); items['fingerLock'].set(a('layout_marginTop'),'12dp')
for id in ('tabIconContainer','tabIconContainerThemes','tabIconContainerVoice'):
    items[id].set(a('layout_width'),'66dp'); items[id].set(a('layout_height'),'33dp'); items[id].set(a('layout_marginTop'),'12dp'); items[id].attrib.pop(p('layout_constraintBottom_toBottomOf'),None)
for id in ('tvTabVoiceLock','tvTabThemes','tvTabVoiceNote'):
    items[id].set(a('text'),'@string/'+{'tvTabVoiceLock':'tab_voice_lock','tvTabThemes':'tab_themes','tvTabVoiceNote':'tab_voice_note'}[id]); items[id].set(a('layout_marginTop'),'8dp'); items[id].set(a('fontFamily'),'@font/sora_semibold'); items[id].attrib.pop(p('layout_constraintBottom_toBottomOf'),None)
save(path,root)
path=res/'layout/include_bottom_tabs.xml'; root=E.parse(path).getroot()
for e in root.iter():
    if e.tag=='FrameLayout': e.set(a('layout_width'),'66dp');e.set(a('layout_height'),'33dp')
    if e.tag=='TextView': e.set(a('textSize'),'@dimen/text_caption');e.set(a('fontFamily'),'@font/sora_semibold');e.set(a('layout_marginTop'),'8dp')
save(path,root)
path=res/'layout/item_security_question.xml'
root=E.Element('LinearLayout',{a('layout_width'):'match_parent',a('layout_height'):'wrap_content',a('minHeight'):'58dp',a('layout_marginBottom'):'12dp',a('padding'):'16dp',a('gravity'):'center_vertical',a('orientation'):'horizontal',a('background'):'@drawable/bg_row_language'})
E.SubElement(root,'TextView',{a('id'):'@+id/tvQuestion',a('layout_width'):'0dp',a('layout_height'):'wrap_content',a('layout_weight'):'1',a('fontFamily'):'@font/sora_medium',a('textColor'):'@color/textPrimary',a('textSize'):'@dimen/text_bodyL'})
E.SubElement(root,'ImageView',{a('id'):'@+id/imgRadio',a('layout_width'):'20dp',a('layout_height'):'20dp',a('layout_marginStart'):'12dp',a('src'):'@drawable/ic_question_radio_off'})
save(path,root)
for name,fill in [('bg_switch_track_unlock','@color/switch_track_lock'),('bg_switch_track_unlock_active','@color/success')]:
    (res/'drawable'/f'{name}.xml').write_text(f'<shape xmlns:android="{A}"><solid android:color="{fill}"/><corners android:radius="100dp"/></shape>')

# Explicit line heights and zero tracking for every token text style.
line={'displayNumber':52,'clock':44,'display':42,'headline':36,'titleXL':28,'titleL':28,'titleM':24,'titleS':24,'bodyL':22,'bodyM':20,'bodyS':20,'caption':16,'overline':14,'buttonL':24,'buttonS':20}
styles=E.Element('resources')
for token,height in line.items():
    s=E.SubElement(styles,'style',{'name':'Text.VoiceLock.'+token,'parent':''})
    for k,v in [('android:textSize','@dimen/text_'+token),('android:lineHeight',str(height)+'sp'),('lineHeight',str(height)+'sp'),('android:letterSpacing','0')]: E.SubElement(s,'item',{'name':k}).text=v
save(res/'values/typography.xml',styles)
for path in (res/'layout').glob('*.xml'):
    root=E.parse(path).getroot()
    for e in root.iter():
        size=e.get(a('textSize'),'')
        if size.startswith('@dimen/text_'):
            token=size.split('text_')[-1]
            if token in line:
                e.set(a('lineHeight'),str(line[token])+'sp')
                e.set(p('lineHeight'),str(line[token])+'sp')
        if e.get(p('tint'))=='@color/black': e.set(p('tint'),'@color/primaryLight')
    save(path,root)
print('Refined Home, radio rows, navigation, and typography line heights.')

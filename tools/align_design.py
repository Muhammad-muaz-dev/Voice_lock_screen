from pathlib import Path
import re
import xml.etree.ElementTree as ET

res = Path('app/src/main/res')
A = 'http://schemas.android.com/apk/res/android'
P = 'http://schemas.android.com/apk/res-auto'
ET.register_namespace('android', A)
ET.register_namespace('app', P)
ET.register_namespace('tools', 'http://schemas.android.com/tools')
sizes = {11:'overline',12:'caption',13:'bodyS',14:'bodyM',15:'bodyL',16:'titleS',18:'titleM',20:'titleL',22:'titleXL',28:'headline',34:'display',36:'clock',44:'displayNumber'}
spacing = {4:'xs',8:'sm',12:'md',16:'lg',20:'xl',24:'xxl',32:'xxxl',40:'huge',48:'max'}
palette = {'FFFFFF':'textPrimary','000000':'bg','142242':'textPrimary','172238':'textPrimary','28C7F4':'textPrimary','1E9BFF':'primaryLight','1E88FF':'primaryLight','22AFFF':'accentCyan','2480FF':'primarySoft','11AD74':'successSoft','EDF1F7':'border'}
for path in (res/'layout').glob('*.xml'):
    root = ET.parse(path).getroot()
    for e in root.iter():
        tag = e.tag.split('}')[-1]
        attrs = e.attrib
        if tag in ('TextView','EditText','Button') or tag.endswith('MaterialButton'):
            font = attrs.get(f'{{{A}}}fontFamily','')
            weight = 'bold' if 'bold' in font else 'semibold' if 'semibold' in font else 'medium' if 'medium' in font else 'regular'
            attrs[f'{{{A}}}fontFamily'] = '@font/sora_'+weight
            attrs[f'{{{A}}}letterSpacing'] = '0'
            size = attrs.get(f'{{{A}}}textSize','')
            m = re.fullmatch(r'(?:@dimen/_)?([\d.]+)(?:ssp|sp)',size)
            if m:
                n = float(m[1])
                if 'ssp' in size: n = max(12,n)
                attrs[f'{{{A}}}textSize'] = '@dimen/text_'+sizes[min(sizes,key=lambda v:abs(v-n))]
            elif not size: attrs[f'{{{A}}}textSize'] = '@dimen/text_bodyM'
            if tag.endswith('Button'):
                attrs[f'{{{A}}}layout_height'] = '50dp'
                attrs[f'{{{A}}}textSize'] = '@dimen/text_buttonL'
                attrs[f'{{{A}}}fontFamily'] = '@font/sora_bold'
                attrs[f'{{{A}}}insetTop'] = '0dp'
                attrs[f'{{{A}}}insetBottom'] = '0dp'
                attrs[f'{{{P}}}backgroundTint'] = '@null'
                if f'{{{A}}}background' not in attrs: attrs[f'{{{A}}}background'] = '@drawable/bg_cta_gradient'
        for k,v in list(attrs.items()):
            if 'padding' in k or 'layout_margin' in k:
                m = re.fullmatch(r'(?:@dimen/_)?([\d.]+)(?:sdp|dp)',v)
                if m and float(m[1]) > 0:
                    n = float(m[1]); attrs[k] = '@dimen/spacing_'+spacing[min(spacing,key=lambda x:abs(x-n))]
                if k.endswith(('layout_marginStart','layout_marginEnd','paddingHorizontal')) and v.startswith('@dimen/_'):
                    attrs[k] = '@dimen/spacing_lg'
            elif re.fullmatch(r'@dimen/_\d+sdp',v):
                attrs[k] = re.search(r'_(\d+)',v)[1]+'dp'
            if v.startswith('#'):
                h=v[1:].upper()
                if 'textColorHint' in k: token='textPlaceholder'
                elif 'textColor' in k: token=palette.get(h,'textSecondary')
                elif 'background' in k: token='border' if tag=='View' else 'surface'
                else: token=palette.get(h,'primaryLight')
                attrs[k]='@color/'+token
        ident=attrs.get(f'{{{A}}}id','').split('/')[-1]
        if ident in ('btnBack','ivBack','ivClose','ivSettings','ivPremium'):
            attrs[f'{{{A}}}layout_width']='44dp'; attrs[f'{{{A}}}layout_height']='44dp'
        if ident in ('tvToolbarTitle','tvHeaderTitle'): attrs[f'{{{A}}}textSize']='@dimen/text_titleL'
    ET.indent(root,space='    ')
    path.write_text('<?xml version="1.0" encoding="utf-8"?>\n'+ET.tostring(root,encoding='unicode')+'\n',encoding='utf-8')

# Shared surfaces must use the dark system, including legacy setup screens.
for name,fill,radius,stroke in [
    ('bg_voice_card','surface','xl','border'),('bg_answer_field','field','xl','border'),
    ('bg_dropdown_field','field','xl','border'),('bg_detected_phrase','field','xl','border'),
    ('bg_recorded_badge','surfaceTint','md','border'),('bg_input_dark','field','xl','border'),
    ('bg_pin_back','surfaceTint','xl','border'),('bg_outline_button','bg','xl','primarySelected')]:
    (res/'drawable'/f'{name}.xml').write_text(f'''<shape xmlns:android="{A}"><solid android:color="@color/{fill}"/><corners android:radius="@dimen/radius_{radius}"/><stroke android:width="1dp" android:color="@color/{stroke}"/></shape>''')
print('Aligned layout typography, spacing, and shared surfaces.')

#!/usr/bin/env python3
"""Check the Portuguese resource catalog and the ICU/printf argument contract."""
from pathlib import Path
import xml.etree.ElementTree as ET
import re, collections, sys
ROOT=Path(__file__).resolve().parents[1]
MODULES=['app','ui/design','feature/connect','feature/setup','feature/manage','feature/launcher','feature/backup','feature/modules','tv/curtain']

def closing(text,opening):
    depth=1
    for i in range(opening+1,len(text)):
        if text[i]=='{':depth+=1
        if text[i]=='}':
            depth-=1
            if not depth:return i
    raise ValueError('unclosed message brace')

def contract(text):
    args=set();formats=[]
    def message(start,end):
        i=start
        while i<end:
            if text[i]!='{':i+=1;continue
            stop=closing(text,i);inside=text[i+1:stop]
            match=re.match(r'\s*([^\W\d]\w*)\s*(,|$)',inside)
            if not match:i=stop+1;continue
            name=match.group(1);args.add(name)
            if match.group(2)==',':
                rest=inside[match.end():];kind=re.match(r'\s*([^\s,}]+)',rest)
                if kind:
                    typ=kind.group(1);selectors=[]
                    if typ in ['plural','select','selectordinal']:
                        position=i+1+match.end()+kind.end()
                        if position<stop and text[position]==',':position+=1
                        while position<stop:
                            while position<stop and text[position].isspace():position+=1
                            offset=re.match(r'offset:\s*\d+',text[position:stop])
                            if offset:position+=offset.end();continue
                            selector=re.match(r'([^\s{}]+)\s*\{',text[position:stop])
                            if not selector:break
                            selectors.append(selector.group(1));opening=position+selector.end()-1;finish=closing(text,opening)
                            message(opening+1,finish);position=finish+1
                    formats.append((name,typ,tuple(sorted(selectors))))
            i=stop+1
    message(0,len(text))
    printf=collections.Counter(re.findall(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]',text))
    return args,collections.Counter(formats),printf

count=0;failures=[]
def resources(folder):
    result={}
    for file in sorted(folder.glob('*.xml')):
        for node in ET.parse(file).getroot():
            if node.tag not in ('string','string-array','plurals'):continue
            key=node.get('name')
            if key:
                if key in result:failures.append((str(folder),key,'duplicate resource'))
                result[key]=node
    return result

for module in MODULES:
    base=ROOT/module/'src/main/res';en=resources(base/'values')
    pt=resources(base/'values-pt-rBR')
    for key,node in en.items():
        if node.get('translatable')=='false':continue
        if key not in pt:failures.append((module,key,'missing translation'));continue
        count+=1
        a=''.join(node.itertext());b=''.join(pt[key].itertext())
        if a.strip() and not b.strip():failures.append((module,key,'empty translation'))
        try:
            if contract(a)!=contract(b):failures.append((module,key,'argument or formatter mismatch',contract(a),contract(b)))
        except ValueError as error:failures.append((module,key,str(error)))
for error in failures:print('ERROR',*error)
print(f'{count} recursos revisados; {len(failures)} problemas de contrato')
sys.exit(bool(failures))

"""Validate real publication values and export the exact configured app policies for hosting.

Default is validation only. --export writes configured HTML/Markdown under configured-legal/.
This never publishes, sends email, builds an APK or uploads private data.
"""
import argparse
import html
import json
import re
from pathlib import Path
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--export', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
config = {e.get('name'): ''.join(e.itertext()).strip() for e in ET.parse(root/'app/src/main/res/values/legal_config.xml').getroot()}
errors=[]
for name in ['legal_developer_name','legal_contact_email','legal_privacy_url','legal_terms_url']:
    if not config.get(name): errors.append(name+' is missing')
email=config.get('legal_contact_email','')
if email and not re.fullmatch(r'[^\s@]+@[^\s@]+\.[^\s@]+',email):errors.append('Contact email is invalid')
for name in ['legal_privacy_url','legal_terms_url']:
    value=config.get(name,'')
    if value:
        u=urlsplit(value)
        if u.scheme!='https' or not u.hostname or u.username or u.password: errors.append(name+' must be a public HTTPS URL without credentials')
        if u.hostname and (u.hostname in ['localhost','example.com','example.org','example.net'] or u.hostname.endswith('.example')): errors.append(name+' contains a placeholder/local host')
        if u.path.lower().endswith('.pdf'):errors.append(name+' must be a web page, not a PDF')
if errors:
    print('NOT READY FOR PUBLICATION:')
    for error in errors:print('- '+error)
    raise SystemExit(2)
print('Configuration syntax passed. Actual ownership, public accessibility and live hosted content still require verification.')
if args.export:
    replacements={'{{DEVELOPER_NAME}}':config['legal_developer_name'],'{{CONTACT_EMAIL}}':config['legal_contact_email'],
        '{{PRIVACY_URL}}':config['legal_privacy_url'],'{{TERMS_URL}}':config['legal_terms_url']}
    docs=json.loads((root/'app/src/main/assets/legal/documents.json').read_text())
    target=root/'configured-legal';target.mkdir(exist_ok=True)
    for key,doc in docs.items():
        sections=[]
        for heading,body in doc['sections']:
            for token,value in replacements.items():body=body.replace(token,value)
            sections.append((heading,body))
        text='# Call Capture — '+doc['title']+'\n\nVersion '+doc['version']+' · Effective '+doc['effectiveDate']+'\n\n'
        text+='\n\n'.join('## '+heading+'\n\n'+body for heading,body in sections)+'\n'
        (target/(key+'.md')).write_text(text)
        body=''.join('<section><h2>'+html.escape(h)+'</h2><p>'+html.escape(b)+'</p></section>' for h,b in sections)
        page='<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>'+html.escape(doc['title'])+' — Call Capture</title><style>body{max-width:760px;margin:40px auto;padding:0 22px;font:17px/1.65 system-ui;color:#202124}h1{line-height:1.2}h2{font-size:21px;margin-top:30px}</style><main><h1>Call Capture — '+html.escape(doc['title'])+'</h1><p>Version '+doc['version']+' · Effective '+doc['effectiveDate']+'</p>'+body+'</main></html>'
        (target/(key+'.html')).write_text(page)
    print('Exported configured documents to',target)

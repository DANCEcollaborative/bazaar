"""Offline HTTPS receiver verifies the exact bytes in the production Java sender."""
import base64
import hashlib
import io
import json
import math
import os
from pathlib import Path
import shutil
import ssl
import subprocess
import sys
import tempfile
import threading
import unittest
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
OPERATOR=Path(os.environ.get('FCDS_OPERATOR_REPO',str(ROOT.parents[1]/'ope-platform-operator')))
sys.path.insert(0,str(OPERATOR/'jupyterlab/fcds-dev/recorder'))
from fcds_recorder.collector import Collector
ROOM='fcds-p2-26-fall-1a-room260911970'

class LlmRotationTests(unittest.TestCase):
    def test_production_https_images_equal_first_twenty_evidence_files(self):
        with tempfile.TemporaryDirectory() as directory:
            work=Path(directory)
            evidence=Path(os.environ.get('FCDS_ROTATION_EVIDENCE_OUT',str(work/'evidence')))
            evidence.mkdir(parents=True,exist_ok=False)
            shutil.copytree(ROOT/'bazaar/runtime',work,dirs_exist_ok=True)
            for name in ['logs','chat_history','planstatus']:(work/name).mkdir(exist_ok=True)
            key=work/'capture.key';key.write_bytes(b'offline-rotation-test-key-at-least-32-bytes')
            collector=Collector(work/'archive',key);collector.event(ROOM,'activity.phase',{'phase':'Paper'})
            image=Image.new('RGB',(240,160),'white');draw=ImageDraw.Draw(image)
            draw.rectangle((0,0,119,79),fill='#e02020');draw.rectangle((120,0,239,79),fill='#20c020')
            draw.rectangle((0,80,119,159),fill='#2040e0');draw.rectangle((120,80,239,159),fill='#e0c020')
            draw.polygon([(90,70),(170,70),(170,45),(215,80),(170,115),(170,90),(90,90)],fill='black')
            raw=io.BytesIO();image.save(raw,'PNG');original=raw.getvalue();source64=base64.b64encode(original).decode()
            (work/'source.base64').write_text(source64)
            # One archived original is reused across all requests while the student angle changes.
            expires=str(__import__('time').time().__int__()+3600)
            import hmac
            token=expires+'.'+hmac.new(key.read_bytes(),f'{ROOM}|1|{expires}'.encode(),hashlib.sha256).hexdigest()
            collector.camera_frame({'room_id':ROOM,'participant_id':'1','frame_id':'test-frame','producer_id':'test-camera',
                'producer_sequence':1,'captured_at':'2026-11-04T12:00:00Z','imageBase64':source64,'mimeType':'image/png','width':240,'height':160},token)
            cert=work/'server.crt';private=work/'server.key';trust=work/'trust.p12'
            subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-keyout',str(private),'-out',str(cert),
                '-days','1','-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost,IP:127.0.0.1'],check=True,capture_output=True)
            subprocess.run([os.environ.get('KEYTOOL','keytool'),'-importcert','-noprompt','-alias','mock','-file',str(cert),'-keystore',str(trust),
                '-storetype','PKCS12','-storepass','testpass1'],check=True,capture_output=True)
            received=[];lookups=[];failed_lookups=[];angles=[0,90,-90,180,17.5]
            class Receiver(BaseHTTPRequestHandler):
                def log_message(self,*args):pass
                def do_POST(self):
                    raw=self.rfile.read(int(self.headers['Content-Length']));body=json.loads(raw)
                    if self.path=='/rotation':
                        if len(lookups)==21 and not failed_lookups:
                            failed_lookups.append(body);self.send_response(503);self.send_header('Content-Length','0');self.end_headers();return
                        angle=angles[len(lookups)%len(angles)]
                        if lookups or angle!=0:
                            collector.camera_image_rotation({'room_id':ROOM,'participant_id':'1','frame_id':'test-frame',
                                'rotation_degrees':angle,'mutation_id':str(uuid.uuid4())},token)
                        lookups.append(body)
                        auth=self.headers.get('Authorization','')
                        result=collector.camera_image_rotation(body,auth.removeprefix('Camera '))
                    else:
                        received.append((raw,body))
                        result={'choices':[{'message':{'content':json.dumps({'response_type':'No response','reply':''})}}]}
                    output=json.dumps(result).encode();self.send_response(200);self.send_header('Content-Length',str(len(output)));self.end_headers();self.wfile.write(output)
            server=ThreadingHTTPServer(('127.0.0.1',0),Receiver)
            context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.load_cert_chain(cert,private)
            server.socket=context.wrap_socket(server.socket,server_side=True)
            thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
            url='https://localhost:'+str(server.server_port)
            properties=work/'properties'
            (properties/'State.properties').write_text('ignore_prefixes=Private_,Camera_,tab_group\n')
            (properties/'TransportCamera.properties').write_text('name=OPEBot\nmodel=openai\nopenai.request.url='+url+'/llm\n'
                'openai.prompt.context=Offline rotation test.\nopenai.context.flag=true\nopenai.context.length=20\nopenai.temperature=0.2\n'
                'openai.model.name=offline-test\ncamera.rotation.url='+url+'/rotation\nuser-poll-rate=3600\nuser-poll-timeout=1\n')
            (properties/'apiKeys.properties').write_text('offline-test-placeholder\n')
            cp=str(ROOT/'bazaar/build/fcdsrepresentationagent.jar')+':'+str(ROOT/'bazaar/build/llmcamera-runtime.jar')
            try:
                built=subprocess.run([os.environ.get('JAVAC','javac'),'--release','8','-Xlint:-options','-cp',cp,'-d',str(work),str(ROOT/'tests/RotationTransportHarness.java')],capture_output=True,text=True)
                self.assertEqual(built.returncode,0,built.stderr)
                run=subprocess.run([os.environ.get('JAVA','java'),'-Djava.awt.headless=true','-Dbasilica2.handsfree=true',
                    '-Djavax.net.ssl.trustStore='+str(trust),'-Djavax.net.ssl.trustStorePassword=testpass1',
                    '-Dfcds.llm.evidence.dir='+str(evidence),'-cp',str(work)+':'+cp,
                    'basilica2.agents.listeners.plan.RotationTransportHarness'],cwd=work,capture_output=True,text=True,timeout=45)
                self.assertEqual(run.returncode,0,(run.stdout+run.stderr)[-8000:])
                self.assertIn('PASS: production rotation',run.stdout)
            finally:
                server.shutdown();server.server_close();thread.join(5)
            self.assertEqual(len(lookups),22);self.assertEqual(len(failed_lookups),1);self.assertEqual(len(received),23)
            self.assertTrue(all(b['current_rotation'] and b['participant_id']=='1' for b in lookups))
            self.assertTrue(all(b['current_rotation'] for b in failed_lookups))
            records=sorted(evidence.glob('image-*.json'));self.assertEqual(len(records),20)
            self.assertEqual(len(list(evidence.glob('image-*.jpg')))+len(list(evidence.glob('image-*.png'))),20)
            verified=[]
            for n,record in enumerate(records):
                metadata=json.loads(record.read_text());wire,body=received[n+1]
                data=body['messages'][metadata['message_index']]['content'][metadata['part_index']]['image_url']['url']
                sent=base64.b64decode(data.split(',',1)[1]);saved=(evidence/metadata['image_file']).read_bytes()
                self.assertEqual(sent,saved);self.assertEqual(hashlib.sha256(wire).hexdigest(),metadata['request_sha256'])
                self.assertEqual(hashlib.sha256(sent).hexdigest(),metadata['sent_sha256'])
                self.assertEqual(metadata['original_sha256'],hashlib.sha256(original).hexdigest())
                degrees=angles[n%5];self.assertEqual(metadata['rotation_degrees'],degrees)
                rendered=Image.open(io.BytesIO(sent));r=math.radians(degrees)
                expected=(math.ceil(240*abs(math.cos(r))+160*abs(math.sin(r))-1e-9),math.ceil(240*abs(math.sin(r))+160*abs(math.cos(r))-1e-9))
                self.assertEqual(rendered.size,expected)
                if degrees==0:self.assertEqual(sent,original)
                else:self.assertNotEqual(sent,original)
                # A red upper-left source marker must move clockwise, not counterclockwise.
                x,y=30-120,30-80
                px=round(rendered.width/2+x*math.cos(r)-y*math.sin(r));py=round(rendered.height/2+x*math.sin(r)+y*math.cos(r))
                color=rendered.convert('RGB').getpixel((px,py));self.assertGreater(color[0],150);self.assertLess(color[1],80);self.assertLess(color[2],80)
                verified.append({'file':metadata['image_file'],'degrees':degrees,'exact_request_image_match':True,'request_hash_match':True,'size':list(rendered.size)})
            self.assertEqual((work/'archive/rooms'/ROOM/'blobs'/hashlib.sha256(original).hexdigest()).read_bytes(),original)
            # Journal the original request template and rendering hashes, never persistent rotated image blobs.
            journals=''.join(p.read_text() for p in (work/'capture_spool').glob('*.jsonl'))
            self.assertNotIn(base64.b64encode((evidence/'image-02.jpg').read_bytes()).decode(),journals)
            source=evidence/'original.png';source.write_bytes(original)
            (evidence/'verification.json').write_text(json.dumps({'test':'Local HTTPS receiver, production Java transport; no external LLM calls',
                'received_image_requests':22,'captured_images':20,'cap_verified':True,'original_unchanged':True,'images':verified},indent=2))
            collector.store.db.close()

if __name__=='__main__':unittest.main()

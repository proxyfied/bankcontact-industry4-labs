"""Test-only OpenAI HTTP fixture. This server does not run a language model."""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
fixture = json.loads((Path(__file__).resolve().parents[1]/'src/test/resources/fixtures/article.json').read_text())
class Handler(BaseHTTPRequestHandler):
 def do_POST(self):
  if self.path != '/v1/chat/completions':self.send_error(404);return
  if self.headers.get('Transfer-Encoding','').lower() == 'chunked':
   chunks = []
   while True:
    size = int(self.rfile.readline().split(b';',1)[0].strip(),16)
    if not size:
     while self.rfile.readline().strip(): pass
     break
    chunks.append(self.rfile.read(size))
    self.rfile.read(2)
   raw = b''.join(chunks)
  else:
   raw = self.rfile.read(int(self.headers.get('Content-Length','0')))
  request = json.loads(raw)
  text = request['messages'][-1]['content']
  if request.get('model') != 'gpt-4o-mini' or request.get('response_format',{}).get('type') != 'json_object':self.send_error(400);return
  if 'INVALID_JSON' in text: content = 'This is not JSON'
  elif 'INCOMPLETE' in text: content = json.dumps({'title_of_the_article':'Partial citation','language':'eng','specialized':'Category A'})
  else: content = json.dumps(fixture,ensure_ascii=False)
  body = json.dumps({'id':'local-http-stub','object':'chat.completion','created':1,'model':'gpt-4o-mini','choices':[{'index':0,'message':{'role':'assistant','content':content},'finish_reason':'stop'}]},ensure_ascii=False).encode()
  self.send_response(200);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
HTTPServer(('127.0.0.1',8099),Handler).serve_forever()

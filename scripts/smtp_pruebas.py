"""Buzón SMTP local de pruebas: no reenvía ningún mensaje a Internet."""
from pathlib import Path
from email import policy
from email.parser import BytesParser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from socketserver import StreamRequestHandler, ThreadingTCPServer
import argparse
import json
import threading
import uuid

ROOT = Path(__file__).resolve().parents[1] / '_trabajo_apf3' / 'correos'
ROOT.mkdir(parents=True, exist_ok=True)


class SMTP(StreamRequestHandler):
    def respuesta(self, value):
        self.wfile.write(value.encode('ascii') + b'\r\n')
        self.wfile.flush()

    def handle(self):
        self.connection.settimeout(30)
        self.respuesta('220 localhost Tezza SMTP de pruebas')
        while raw := self.rfile.readline(8192):
            command = raw.decode('utf-8', errors='replace').strip().upper()
            if command.startswith(('EHLO', 'HELO')):
                self.respuesta('250 localhost')
            elif command.startswith(('MAIL FROM:', 'RCPT TO:', 'RSET', 'NOOP')):
                self.respuesta('250 OK')
            elif command == 'DATA':
                self.respuesta('354 End with dot')
                lines = []
                while True:
                    line = self.rfile.readline(65536)
                    if not line or line in (b'.\r\n', b'.\n'):
                        break
                    lines.append(line[1:] if line.startswith(b'..') else line)
                content = b''.join(lines)
                (ROOT / f'{uuid.uuid4().hex}.eml').write_bytes(content)
                self.respuesta('250 Guardado solo en el buzon local')
            elif command == 'QUIT':
                self.respuesta('221 Bye')
                break
            else:
                self.respuesta('502 Comando no soportado')


def mensajes():
    result = []
    for path in sorted(ROOT.glob('*.eml'), key=lambda p: p.stat().st_mtime, reverse=True):
        m = BytesParser(policy=policy.default).parsebytes(path.read_bytes())
        result.append({'archivo': path.name, 'para': str(m.get('To', '')),
                       'asunto': str(m.get('Subject', '')), 'texto': m.get_content()})
    return result


class Web(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/mensajes.json':
            body = json.dumps(mensajes(), ensure_ascii=False).encode('utf-8')
            kind = 'application/json; charset=utf-8'
        else:
            body = b'''<!doctype html><html lang="es"><meta charset="utf-8"><title>Buzon de pruebas Tezza</title>
            <style>body{font:16px Arial;background:#f4f7fb;margin:40px;color:#18324b}article{background:white;padding:24px;margin:20px 0;border-radius:12px}pre{white-space:pre-wrap}button{padding:12px}</style>
            <h1>Buzon local de pruebas</h1><p>Estos mensajes no se han enviado a Gmail ni a otros destinatarios reales.</p>
            <button onclick="location.reload()">Actualizar</button><main id="m"></main>
            <script>fetch('/mensajes.json').then(r=>r.json()).then(ms=>{for(const x of ms){const a=document.createElement('article');const h=document.createElement('h2');h.textContent=x.asunto;const p=document.createElement('p');p.textContent='Para: '+x.para;const t=document.createElement('pre');t.textContent=x.texto;a.append(h,p,t);document.querySelector('#m').append(a)}})</script></html>'''
            kind = 'text/html; charset=utf-8'
        self.send_response(200)
        self.send_header('Content-Type', kind)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


if __name__ == '__main__':
    args = argparse.ArgumentParser()
    args.add_argument('--smtp-port', type=int, default=1025)
    args.add_argument('--web-port', type=int, default=8025)
    params = args.parse_args()
    smtp = ThreadingTCPServer(('127.0.0.1', params.smtp_port), SMTP)
    smtp.daemon_threads = True
    threading.Thread(target=smtp.serve_forever, daemon=True).start()
    print(f'Buzon local http://127.0.0.1:{params.web_port}', flush=True)
    ThreadingHTTPServer(('127.0.0.1', params.web_port), Web).serve_forever()

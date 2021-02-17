from flask import Flask, render_template, request, url_for, redirect, send_file, jsonify, abort, send_from_directory
from werkzeug.utils import secure_filename
import requests
from threading import Thread
from pathlib import Path
import socket
import os
import sys
import hashlib
import json
from settings import *

THIS_IP = socket.gethostbyname(socket.gethostname())

app = Flask(__name__)

def mkdir(dir):
    try:
        os.mkdir(dir)
    except:
        pass

def sha1_f(file):
    return sha1_fh(open(file, 'rb'))

def sha1_fh(filehandler):
    sha1=hashlib.sha1()
    while True:
        data = filehandler.read(BUFFER_SIZE)
        if not data:
            break
        sha1.update(data)
    return sha1

def _dl_faithful():
    r = requests.get('https://github.com/FaithfulTeam/Faithful/raw/releases/1.16.zip')
    ro = open('faithful-1.16.zip', 'wb').write(r.content)
    print(f'OK {ro}')

def dl_faithful():
    t = Thread(target=_dl_faithful)
    t.start()

def read_props():
    props = open(os.path.join(SERVER_DIR, 'server.properties'), 'r').read()
    top = props.split('\n')[:2] 
    props = [p for p in props.split('\n')[2:] if p]
    prop_dict = {}
    for pp in props:
        s_buf = pp.split('=')
        prop_dict[s_buf[0]] = s_buf[1]
    return top, prop_dict

def write_props(top, props):
    return open(os.path.join(SERVER_DIR, 'server.properties'), 'w').write(
        '\n'.join(top + [f'{x}={props[x]}' for x in props])
    )

def get_curr_pack():
    top, props = read_props()
    for file in os.listdir(UPLOADS_DIR):
        sha1 = sha1_f(os.path.join(UPLOADS_DIR, file)).hexdigest()
        if props['resource-pack-sha1'] == sha1:
            return file
    return 0

@app.route('/')
def index():
    mkdir(UPLOADS_DIR)
    mkdir(TMP_DIR)
    top, props = read_props()
    props['resource-pack'] = f'http://{THIS_IP}:{PORT}/pack'
    write_props(top, props)
    f = get_curr_pack()
    return render_template('index.html', rp_name=str(f).split('/')[-1])


def update_sha1(fn):
    sha1 = sha1_f(os.path.join(UPLOADS_DIR, fn)).hexdigest()
    top, props = read_props()
    props['resource-pack-sha1'] = sha1
    write_props(top, props)


def is_unique_pack(fpath):
    sha1f = sha1_f(fpath).hexdigest()
    for fn in os.listdir(UPLOADS_DIR):
        print(fn)
        if sha1_f(os.path.join(UPLOADS_DIR, fn)).hexdigest() == sha1f:
            return fn
    return 0
        

@app.route('/pack', methods=['GET', 'POST'])
def pack():
    if request.method == 'GET':
        f = get_curr_pack()
        if f != 0:
            print(os.path.join(UPLOADS_DIR, f), f.split('/')[-1])
            return send_from_directory(UPLOADS_DIR, f.split('/')[-1], as_attachment=True)
        return redirect(url_for('index'))
    if request.method == 'POST':
        data = request.data
        data = json.loads(data)
        print(data)
        if 'set' in data:
            fn = secure_filename(data['set'])
            if fn in os.listdir(UPLOADS_DIR):
                update_sha1(fn)
                return jsonify({'msg': 'success'})
        return abort(404)


@app.route('/upload', methods=['GET', 'POST'])
def upload():
    if 'file' not in request.files:
        return 'NO DATA'
    file = request.files['file']
    if not file.filename:
        return 'NO FILE'
    if file.filename.split('.')[-1] == 'zip':
        fn = secure_filename(file.filename)
        import uuid
        fuuid = str(uuid.uuid4())
        fpath = os.path.join(TMP_DIR, fuuid)
        file.save(fpath)
        ident = is_unique_pack(fpath)
        if ident == 0:
            open(os.path.join(UPLOADS_DIR, fn), 'wb').write(open(fpath, 'rb').read())
        else:
            return jsonify({'msg': f'identical pack exists: {ident}'})
    return redirect(url_for('index')) 

@app.route('/uploads/<file>')
def get_upload(file):
    if file in os.listdir(UPLOADS_DIR):
        print('Sending file :' + file)
        return send_file(os.path.join(UPLOADS_DIR, file))
    return 'NOT EXISTING'


@app.route('/uploads')
def uploads():
    return jsonify([{'name': x, 'size': os.path.getsize(os.path.join(UPLOADS_DIR, x)), 'time': os.path.getmtime(os.path.join(UPLOADS_DIR, x))} for x in os.listdir(UPLOADS_DIR)])


if __name__ == '__main__':

    app.run(host='0.0.0.0', port=PORT, debug=True)

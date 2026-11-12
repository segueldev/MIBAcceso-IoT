#!/usr/bin/env python3
"""Relay de laboratorio: retrasa tramas cifradas sin conocer el código de misión."""
import argparse
import socket
import struct
import threading
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--escucha', type=int, default=8988)
parser.add_argument('--destino', type=int, default=18988)
parser.add_argument('--retardo-ms', type=float, default=150)
args = parser.parse_args()
if not 0 <= args.retardo_ms <= 2000:
    parser.error('El retardo debe estar entre 0 y 2000 ms.')


def atender(cliente):
    base = socket.create_connection(('127.0.0.1', args.destino), timeout=5)
    base.settimeout(None)
    def pasar(origen, destino):
        def leer(tamano):
            contenido = b''
            while len(contenido) < tamano:
                bloque = origen.recv(tamano - len(contenido))
                if not bloque:
                    raise EOFError()
                contenido += bloque
            return contenido
        try:
            while True:
                cabecera = leer(4)
                tamano = struct.unpack('>i', cabecera)[0]
                if not 28 <= tamano <= 8192:
                    raise ValueError('Tamaño de trama inválido.')
                cifrado = leer(tamano)
                time.sleep(args.retardo_ms / 1000)
                destino.sendall(cabecera + cifrado)
        except (OSError, EOFError, ValueError):
            for conexion in (cliente, base):
                try:
                    conexion.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass
                conexion.close()
    for origen, destino in ((cliente, base), (base, cliente)):
        threading.Thread(target=pasar, args=(origen, destino), daemon=True).start()


with socket.socket() as servidor:
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind(('127.0.0.1', args.escucha))
    servidor.listen(1)
    print(f'Relay local en {args.escucha}; destino {args.destino}; retardo {args.retardo_ms} ms por trama y sentido.')
    print('No se descifran ni se registran los mensajes. Finalizar con Ctrl+C.')
    try:
        while True:
            cliente, _ = servidor.accept()
            threading.Thread(target=atender, args=(cliente,), daemon=True).start()
    except KeyboardInterrupt:
        pass

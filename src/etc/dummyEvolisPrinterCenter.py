#!/usr/bin/env python2
# -*- coding: utf-8 -*-

import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
well_known_port = 18000
sock.bind(('', well_known_port))
sock.listen(30)

try:
    while 1:
        newSocket, address = sock.accept(  )
        print "Connected from", address
        while 1:
            receivedData = newSocket.recv(1024)
            if not receivedData: break
            newSocket.send(b'{"id":"1","jsonrpc":"2.0","result":"OK"}')
        newSocket.close(  )
        print "Disconnected from", address
finally:
    sock.close(  )

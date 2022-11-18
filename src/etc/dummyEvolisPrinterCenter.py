#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import asyncio
import time

async def handle_echo(reader, writer):
    message = await reader.readline()
    if(len(message)>200):
        message = message[0:200]
    print(f"Received {message!r}")
    time.sleep(2)
    
    message = b'{"id":"1","jsonrpc":"2.0","result":"OK"}'
    print(f"Send: {message!r}")
    writer.writelines([message])
    await writer.drain()
    writer.close()

async def main():
    server = await asyncio.start_server(handle_echo, '127.0.0.1', 18000, limit=1024*1024*10)

    addrs = ', '.join(str(sock.getsockname()) for sock in server.sockets)
    print(f'Serving on {addrs}')

    async with server:
        await server.serve_forever()

asyncio.run(main())



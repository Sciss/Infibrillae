# Python 2

import SimpleHTTPServer
import SocketServer

class WasmHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
    def end_headers(self):        
        self.send_header("Cross-Origin-Opener-Policy", "same-origin")
        self.send_header("Cross-Origin-Embedder-Policy", "require-corp")
        SimpleHTTPServer.SimpleHTTPRequestHandler.end_headers(self)


# Python 3.7.5 adds in the WebAssembly Media Type. Version 2.x doesn't
# have this so add it in.
WasmHandler.extensions_map['.wasm'] = 'application/wasm'


if __name__ == '__main__':
    PORT = 8080
    httpd = SocketServer.TCPServer(("", PORT), WasmHandler)
    print("Listening on port {}. Press Ctrl+C to stop.".format(PORT))
    httpd.serve_forever()

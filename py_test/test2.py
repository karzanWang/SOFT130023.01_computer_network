import socket

HOST='127.0.0.1'
PORT=21
a= socket.socket()
a.connect((HOST,PORT))


host = '127.0.0.1'
port = 9999
s= socket.socket()   
s.bind((host, port))
s.listen(1)
a.sendall("PORT 127,0,0,1,39,54\n")

clientsocket, addr = s.accept()

print(clientsocket, addr)

a.sendall("QUIT\n")
s.close()
a.close()

import socket
HOST='127.0.0.1'
PORT=21
s= socket.socket()   
s.connect((HOST,PORT))
#cmd=input("Please input cmd:")
s.sendall("USER test\n")
s.sendall("PASS test\n")
data=s.recv(1024)
print(data)
s.sendall("quit\n")
s.close()

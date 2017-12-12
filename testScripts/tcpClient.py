import socket

pic = open("test.jpeg", 'rb')
data = pic.read()
ADDR = ("192.168.1.145", 8023)
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(ADDR);
print "Sending file... len=" + str(len(data))
client.send(data)
client.close()
print "END"

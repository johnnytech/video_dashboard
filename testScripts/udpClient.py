#!/usr/local/bin/python
import json
import random
import socket
import time
from random import randint

clientSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
addr = ("192.168.1.150", 8023)
index = 1;

while True:
    data = {'jds_score': round(random.uniform(0.0, 10.0), 1), 'lec_alarm': randint(0,1), 'face_track': randint(0,1), 'face_track_alarm': randint(0,1), 'jds_alert': randint(0,2), 'valid': randint(0,1)}
    print "SENDING DATA [" + str(index) + "]: " + json.dumps(data)
    clientSocket.sendto(json.dumps(data), addr)
    index += 1
    time.sleep(3)

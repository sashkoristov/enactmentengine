import sys
import json
import time

def main(args):
    sleepSec = int(args.get("sleepSec"))
    name = args.get("name", "stranger")
    greeting = "Hello " + name + "!"
    print(greeting)
    bytearray(sleepSec)

#    time.sleep(sleepSec) 

    return {"message": greeting}
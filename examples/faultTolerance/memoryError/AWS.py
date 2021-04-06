import json
import time

def lambda_handler(event, context):
    # TODO implement
    sleepSec = int(event['sleepSec'])
    name = event['name']
    message = 'Hello ' + event['name']
    bytearray(sleepSec)
    #time.sleep(sleepSec) 
    return {
        'message': message 
    }

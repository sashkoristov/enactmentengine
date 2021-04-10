import json
import time

def lambda_handler(event, context):
    # TODO implement
    sleepSec = int(event['sleepSec'])
    name = event['name']
    message = 'Hello ' + event['name']
    time.sleep(sleepSec) 
    return {
        'message': message 
    }

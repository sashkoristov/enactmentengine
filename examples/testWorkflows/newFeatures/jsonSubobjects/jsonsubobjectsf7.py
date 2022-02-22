import json

def lambda_handler(event, context):
    # TODO implement
    return {
        'statusCode': 200,
        'body': {
            'f7out1': event['f7input1']   
        }
    }

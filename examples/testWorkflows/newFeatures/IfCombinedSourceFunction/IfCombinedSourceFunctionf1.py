def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f1out1': event["f1input1"]
        }
    }

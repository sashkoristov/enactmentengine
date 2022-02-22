def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f3out1': event["f3input1"]
        }
    }

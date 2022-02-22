def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f5out1': event['f5input1']
        }
    }

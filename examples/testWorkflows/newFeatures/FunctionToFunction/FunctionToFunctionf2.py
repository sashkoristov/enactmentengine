def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f2out1': 'This is a Dummy function'
        }
    }

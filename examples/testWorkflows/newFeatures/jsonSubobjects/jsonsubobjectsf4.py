def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f4out1': 'This is a Dummy function' 
        }
    }

def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f1out1': [1, 2, 3],
            'f1out2': 42
        }
    }

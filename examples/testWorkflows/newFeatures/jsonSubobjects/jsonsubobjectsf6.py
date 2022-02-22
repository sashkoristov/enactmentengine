def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f6out1': "This testing string comes from here"
        }
    }

def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f2out1': event["f2input1"],
            'f2out2': 42
        }
    }
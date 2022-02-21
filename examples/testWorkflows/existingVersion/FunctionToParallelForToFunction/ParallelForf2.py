def lambda_handler(event, context):
    return {
        'f2out1': float(event['f2input1'])
    }

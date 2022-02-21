def lambda_handler(event, context):
    return {
        'f1out1': event["f1input1"]
    }

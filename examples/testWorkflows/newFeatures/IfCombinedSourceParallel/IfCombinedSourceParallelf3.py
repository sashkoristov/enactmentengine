def lambda_handler(event, context):
    return {
        'f3out1': event["f3input1"],
        'f3out2': event["f3input2"]
    }

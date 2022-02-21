def lambda_handler(event, context):
    return {
        'f1out1': event["f1input1"],
        'f1out2': event["f1input1"],
        'f1out3': event["f1input2"],
        'f1out4': event["f1input3"]
    }

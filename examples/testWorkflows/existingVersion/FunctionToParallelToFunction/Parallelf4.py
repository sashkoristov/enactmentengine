def lambda_handler(event, context):
    return {
        'f4out1': "Passed: " + event["f4input2"]
    }

def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': {
            'f3out1': 'Finished successfully'
        }
    }
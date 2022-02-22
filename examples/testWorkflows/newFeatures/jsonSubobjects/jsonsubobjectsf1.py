def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': { 
            'f1out1': {'test': {"subtest" : [0, 1, 2, 3]}, "test2": 42},
            'f1out2': 5    
        }
    }

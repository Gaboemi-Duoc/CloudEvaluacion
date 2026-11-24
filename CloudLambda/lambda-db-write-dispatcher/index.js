const { LambdaClient, InvokeCommand } = require('@aws-sdk/client-lambda');

const lambdaClient = new LambdaClient({
    region: process.env.AWS_REGION || 'us-east-1'
});

const ROUTES = {
    ADMIN_CREATE_COURSE: process.env.COURSE_LAMBDA_NAME,
    ADMIN_ASSIGN_TEACHER: process.env.COURSE_LAMBDA_NAME,
    ADMIN_DELETE_COURSE: process.env.COURSE_LAMBDA_NAME,

    ADMIN_ENROLL_STUDENT: process.env.ENROLLMENT_LAMBDA_NAME,
    TEACHER_ENROLL_STUDENT: process.env.ENROLLMENT_LAMBDA_NAME,
    STUDENT_ENROLL: process.env.ENROLLMENT_LAMBDA_NAME,

    TEACHER_CREATE_TASK: process.env.TASK_LAMBDA_NAME,
    GENERATE_REPORT: process.env.REPORT_LAMBDA_NAME
};

exports.handler = async (event) => {
    for (const record of event.Records || []) {
        try {
            const snsEnvelope = JSON.parse(record.body);

            const payload = typeof snsEnvelope.Message === 'string'
                ? JSON.parse(snsEnvelope.Message)
                : snsEnvelope.Message;

            const action = payload.action;
            const functionName = ROUTES[action];

            if (!functionName) {
                throw new Error(`Unsupported action: ${action}`);
            }

            if (!payload.data || typeof payload.data !== 'object') {
                throw new Error(`Invalid data for action: ${action}`);
            }

            const serviceEvent = {
                ...payload.data,
                action
            };

            console.log(`Routing ${action} to ${functionName}`);

            const response = await lambdaClient.send(
                new InvokeCommand({
                    FunctionName: functionName,
                    InvocationType: 'RequestResponse',
                    Payload: Buffer.from(JSON.stringify(serviceEvent))
                })
            );

            if (response.FunctionError) {
                throw new Error(`Lambda ${functionName} failed`);
            }

            const result = response.Payload
                ? JSON.parse(new TextDecoder().decode(response.Payload))
                : {};

            if (result.statusCode >= 400) {
                throw new Error(`Service returned ${result.statusCode}`);
            }

            console.log(`Successfully processed ${action}`);

        } catch (error) {
            console.error('Dispatcher error:', error);
            throw error;
        }
    }

    return {
        statusCode: 200,
        body: 'Messages dispatched successfully'
    };
};

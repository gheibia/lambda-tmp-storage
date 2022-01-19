# lambda-tmp-storage

A simple Lambda function built with Java (Corretto 11) to be deployed twice: once using default ephemeral storage and once with large ephemeral storage.

Each deployed function is behind a REST API method allowing us to test it.

The entire stack is deployed using CloudFormation (see `template.yml`).

## Build and Deploy

```
$  ./gradlew -q clean packageLibs && mv build/distributions/lambdaEphemeral.zip build/distributions/lambdaEphemeral-libs.zip && ./gradlew -q build

$  aws cloudformation package --template-file template.yml --s3-bucket ephemeral-tester-src --output-template-file out1.yml --region eu-west-1

$  aws cloudformation deploy --template-file out1.yml --stack-name ephemeral-tester --capabilities CAPABILITY_NAMED_IAM --region eu-west-1

$  aws cloudformation package --template-file template.yml --s3-bucket ephemeral-tester-src2 --output-template-file out2.yml --region eu-south-1

$  aws cloudformation deploy --template-file out1.yml --stack-name ephemeral-tester --capabilities CAPABILITY_NAMED_IAM --region eu-south-1
```

These commands respectively:
 - Build and package the application: function and its dependency JARs are packaged in 2 separate zip files
 - Prepare CloudFormation template twice for 2 different regions: upload zip files to S3 and update the template with S3 URIs
 - Deploy the stack in 2 different regions


 ## Notes

 - Prior to these steps, you have to create the `ephemeral-tester-src` and `ephemeral-tester-src2` S3 buckets. **DO NOT** make the buckets public. Instead, configure your AWS CLI correctly to be able to upload source zip file to S3 (when running `aws cloudformation package`).
 - As it can be seen, `gradlew` is used for building the app. The wrapper script will automatically pull `gradle` the first time it runs. thus, one doesn't need to install `gradle`.
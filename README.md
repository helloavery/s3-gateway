# S3 Gateway

Securely manages the storage and retrieval of application secrets stored in S3. Acts as a proxy for application secrets retrieval (i.e. API keys) using AWS API Gateway and S3 where the secrets are stored.

# Structure
* Spring based application that is deployed via AWS Elastic Beanstalk
* Acts as a proxy between the app retrieving and sending secrets and the S3 bucket(s) containing the secrets

# How it works
For retrieving secrets:
* Requesting app makes an API call to S3 Gateway with the bucket, bucket object, as well an encoded Public Key from a generated key pair
* S3 Gateway generates a signature using AWS 4 Signature SDK and returns the requested data
* Then, using the public key sent from the request, the data is encrypted and a signature is created using a private key
* The encrypted data as well as the encoded public key counterpart of the private key that was used to sign the data is returned to the requesting app
* The app first uses its private key from that initial generated key pair to decrypt the data
* Lastly, the encoded public key sent from S3 Gateway is decoded and used to verify the signature

For sending/uploading secrets:
* Requesting app makes an API call containing a random long to S3 Gateway and an encoded public key from a generated key pair is returned
* The generated key pair from above needs to be remembered, so it is temporary cached using the random long generated
* Secrets are encrypted on the requesting app side using S3 Gateway's public key and signed using a generated private key
* Encoded public key as well as encrypted data is sent to S3 Gateway
* S3 Gateway decrypts data using the private key stored in cache and signature is verified
* Then API call is made to upload secrets to the requested S3 bucket 

You can see this in action by [peeking at my Forecaster project][s3gatewayinaction]

[s3gatewayinaction]: https://github.com/helloavery/forecast/tree/master/forecast-common/src/main/java/com/itavery/forecast/external
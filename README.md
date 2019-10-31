# Secrets Chest

Securely manages the storage and retrieval of encrypted application secrets using Envelope Encryption via AWS KMS that are stored S3.

# How it works
For sending secrets:
* A new data key is generated using the master key
* The plaintext data key is used to encrypt the data and the encrypted data key is stored in Hazelcast cluster cache
* Reference is generated for the encrypted data key and encrypted data
* Encrypted data and key and uploaded to S3 buckets
* Reference of the data and key is returned for future reference (i.e. retrieving secrets)

For retrieving secrets:
* Reference to data and key is passed to Secrets Chest
* Data is fetched using reference key
* Secrets Chest checks if the encrypted key is stored within cache, if not, a call it made to retrieve it from the bucket
* Key is decrypted using master key
* Decrypted key decrypts data and returns it

For updating secrets:
* Reference to data and key is passed to Secrets Chest
* Secrets Chest checks if the encrypted key is stored within cache, if not, a call it made to retrieve it from the bucket
* Key is decrypted using master key
* Decrypted key encrypts the new data
* New data is then uploaded


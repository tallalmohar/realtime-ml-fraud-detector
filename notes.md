Just recording down my thoughts as I work on the project.


Sprint 0 - Notes+Thoughts:
- fraud-producer -> Data generation (this is replaceable with real transaction system if needed)

- fraud-producer -> Business Logic (ML inference + persistence)

- fraud-common -> Shared contracts

The multi module system helps because then we can run 5 producers but only 2 consumer instances, you can scale them independently based on load. Being deployed independently, we can deploy a new version of the consumer without needed to touch the producer.

Helpful in keeping the docker images small because the producer doesn't need PostgreSQL driver or the ONNX runtime library. (because it doesn't need to)

Parent POM
General "Global Settings", decides the big decisions like what version of the tool the whole project is going to use. Can also define shared plugins and testing for every module.

Local POM
Local settings, can also define internal relationships between the different modules that might depend on eachother.

Just recording down my thoughts and what I understand as I work on the project. I can refer to this during the testing phase.

Sprint 0 - Notes+Thoughts:

- fraud-producer -> Data generation (this is replaceable with real transaction system if needed)

- fraud-consumer -> Business Logic (ML inference + persistence)

- fraud-common -> Shared contracts

The multi module system helps because then we can run 5 producers but only 2 consumer instances, you can scale them independently based on load. Being deployed independently, we can deploy a new version of the consumer without needed to touch the producer.

Helpful in keeping the docker images small because the producer doesn't need PostgreSQL driver or the ONNX runtime library. (because it doesn't need to)

Parent POM
General "Global Settings", decides the big decisions like what version of the tool the whole project is going to use. Can also define shared plugins and testing for every module.

Local POM
Local settings, can also define internal relationships between the different modules that might depend on eachother.

the docker compose file - docker-compose.yml
My application isnt made up of many different services that work together
include:

- a message broker (kafka) to handle the stream of transactions
- a db (postgresql) to store fraud alerts
- a monitoring tool (prometheus ) to watch over the system
- a coordination service for kafka (zookeeper)

Manually installing, configuring and running each of these steps on my laptop won't get me too far so I run them on isolated containers.

docker-compose : a tool that lets you define and manage this entire multi-container application with single config file (docker-compose.yml)

Breakdown of the Docker file:
(This will also help with understanding the application and how each service plays a role )

services: this is the main section where I defined each component of our infrastructure
zookeeper: Kafka uses Zookeeper for managing it's cluster state, tracking which broker are alive and storing configuration metadata
kafka: this is the core message broker. I've configured two listeners:
-localhost:9092 : For the local application to connect to Kafka from outside the docker network
-kafka:29092: For internal communication between services within the docker network.
postgres: A postgreSQL db for storing fradualent transactions. I've set up a persistent volume (postgres_data) to ensure that the data is saved even if you restart the container
prometheus: This is the monitoring service. It's configured to look for pormetheus.eml file
networks: fraud-detection-network has been defined. This allows the containers to communicated with each other using their services name - for example kafka service can reach the zookeeper service using it's hostname, zookeeper
volumes: This seciton defines the persistent storange for the pg db

Last note on the prometheus.yml

- this is just a simple config file for Prometheus. It tells prometheus to look at host.docker.interanal:8080 and scrape metrics from its /actuator/prometheus endpoint.

host.docker.internal is a special DNS name that allows docker containers to connect to services running on the host machine (Prometheus running on a container will be able to interact with my springboot application running on my laptop ex.)

Testing Kafka Connectivity:
The most basic test is just creating a topic
(topic is like a category or a feed anme, producers write the messages to a topic and consumers read messages from a topic, I am going to test the transaction topic)

docker exec -it kafka kafka-topics --create --topic transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

docker exec it kafka: this tells the docker to execute a command inside the container named kafka(the -it makes the command interactive)
kafka-topics: this is the name of the Kafka script for managing topics
--create: self explanatory
--topic transactions: the name of our topic
--bootstrap-server localhost:9092: this tells the script how to connect to the kafka broker
--partitions 3: this split the topic into 3 partitions. allowing for parallel processing by consumers which is good for scalability (prob not necessary as I dont have any users lol)
--replication-factor 1: this basically means that there will be only on copy of our data. In a prod env you would have a higher replication factor for fault tolerance, but since this is a local env 1 is good enough

we now need to test the postgresql connectivity

docker exec -it postgres psql -U fraud_user -d fraud_detection
docker exec -it postgres : execute command in postgres container
psql: postgres command line
-U fraud_user: connect as the user fraud_user (this is defined in docker-compose.yml )
-d fraud_detection: connect db named fraud_detection, which is also defined

after we connect just run a simple query like:
SELECT version(); and exit

Sprint 1 - Thoughts + notes
Sprint 1 comprieses of creating a springboot application that consistently generates realistic and random financial transactions and sends them to a Kafka Topic.

Transaction Data Model
This hellps us create a shared model between the producer and consumer so both can understand the data passed between them.

TransactionGenerator.java is needed so it can produce the transaction data for my application. This will be usefull later in testing when I want to stress test.

Spring framework -> it works here because this is a service (class that contains business logic)

faker library for fake transaction, this was very useful actually in generating realistic fake data.

application.properties - this file is the standard for springboot app to store its config settings, need to put server ports , db connections and kafka connections

kakfa serializers in applications.properties -> when we send the transaction object to kafka. it needs to converted to stream of bytes (serializers duh!) here we tell kafka how to preform the conversion for the key and value fo the message, JSON STRINGS SUPRISE!
just a quick review of what each one means (just look at the file if you are confused): key-serializer (simple string for our message key so string serializer) value-serializer: JsonSerializer converst my POJO into a JSON String

Implementing the kafka producer: we need something that sends the messages now that we can construct the messages.

KafkaProducerService.java - isolated logic to interact with the external kafka system

KafkaTemplate -> spring kafka helper class that helps with the process of sending the messages. manually constructing this would have been a headache. Need to dependecy inject this.

the sendTransaction method just sends my transaction pojo to kafka (serialized as highlighted before)
the first param is a string that needs to match the topic that kafka docker instance already has (case sensitive name)

Schedule Transaction Generation
The goal is to create a continious stream of data. Easy with teh spring built-in sceduling, we can sed new transactions every few seconds.

very interesting service! @EnableScheduling on the springbootapplication class. My ScheduledTransactionProducer implements the produceTransaction that generates a transaction and sends it to kafka every 2seconds.

TESTED! the Kafkaproducerservice works and sends fake transaction's to the Kafka!
example:
{"transactionID":"af3a49bb-b930-4dc7-8db4-b7ef47073611","userId":"6007-2249-8567-0443","amount":214.92,"merchantId":"Stark Inc","timestamp":null,"location":null,"paymentMethod":null}

Next step: I need to implement the fraud-consumer service that listens to these "transaction" topic, processes each transaction through a ML model and determines if it's fraudulent

we have already made the fraud producer spring app, now we need a spring app for our consumer.
likewise with our producers application.properties file we need to add some deserializers

ONNX Runtime Dependency
This allows java applications to load and run ml models

@KafkaListener annotation automatically:
-Polls the kafka topics in the background
-deserializes json messages into transaction pojos
-calls method for each message
-handles the offset commits
-manages connection failures and retries

How this will all work: (Subject to change if too confusing or unecessary!!)

- This is the entry point for all the messages from kafka
- Every transaction produced by fraud-producer will flow through a method
- this is where you will call fraud detection service
- before implementing the fraud dectection service i can just log the recieved transaction
  fraud-dection-group (if i run multiple instances of fraud consumer with the same group-id
  kafka can automatically distribute the messages)

We need to build a proper OnnxModelConfig because we want a solution where a model is run onnce at startup and shared across all requests
Springs @Bean : IOC -> creates exactly one instance, the benefit of this is that all services get the same loaded model (not new copies)

ONNX Runtime Components
OrtEnvironment - methods

- Global ONNX runtime environment
- One per application
  OrtSession - methods
- the loaded ML model
- resuable for all prediction
  OrtSession.SessionOptions - methods
- Configuration for how to load the model
- Controls CPU/GPU usage, threading

OnnxModelConfig.java
the purpose of the this class is to create a ONNX env (the runtime engine)
Load the model file from your resources
Return an OrtSession -> the loaded model ready for predictions
handles error if the model file is corrupted or missing

inside OnnxModelConfig.java

we run the OrtEnvironment to run the ML model
we use classpath and then read the entire file into mem as a byte array
we use bytes because ONNX models are binary files and the ONNX runtime expects the raw binary data

Feature engineering service
the purpose of this service would be to turn my transaction onejct into a numerical array that the model can easily undrstand

convert this
Transaction {
transactionID: "TXN-12345"
amount: $450.50
merchantId: "AMAZON"
timestamp: 2026-01-29 14:30:00
location: "New York"
paymentMethod: "CREDIT_CARD"
}

into
float[] features = [0.045, 14, 3, 0, 2, 789, 432]
↑ ↑ ↑ ↑ ↑ ↑ ↑
amount hour day weekend payment merchant location
(normalized) (encoded) (hash) (hash)

The flow of the application so far

1- Kafka message -> TransactionConsumer receives Transaction Object
2- FeatureEngineeringService.extractFeatures(transaction)
3- the method above returns float[] array
4- FraudDetectionService uses this for ML interference

features[0] -> this is the amount nomalized (keeps the values between 0-1 ) models train better this way
features[1] -> hors of the day
features[2] -> day of the week
features[3] -> binary feature for weekend dection
features[4] -> payment methods encoded
features[5] -> merchant hash -> converts merchant name to a consisten number
features[5] -> location has (same logic as merchat)


fraud detection service
this is the service that ties everything together

in this service we will 
- inject the ortsession bean
- inject the featureEngineeringService
- method boolean isFradulent 
	- this extracts features 
	- wraps feature in ONNX tensor
	- runs ML inference
	- interprets the output
	-return true/ false

i havn't added a real trained model yet so i will be using a temp rule-based logic (real model gets added @Spring 5)

Transactions > $900 → flag as fraud
Payment method "CRYPTO" → flag as fraud
Everything else → legitimate

im just using temp rule based logic before i train the model
the flow will look like 
Producer -> Kafka -> Consumer -> feature Engineering -> Rules -> logging 
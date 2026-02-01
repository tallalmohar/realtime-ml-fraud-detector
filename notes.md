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

how the app works as of now
1- TransactionConsumer recieves from kafka
2- Calls FraudDectectionService.isFradulent(transaction) on said transaction from kafka
3- FraudDetectionService.detectFraudUsingRules()
4- Simple if/else rules check amount and payment method
5- returns true/false
6-TransactionConsumer logs: "Fraud" or "Ok"

some of the errors that needed correcting and why

- my FraudDetectionService constructor expected an OrtSession bean
  but that bean didn't exist because i made it conditional (only loads if spring.ml.enabled=true)
  by default spring requires all constructors params to have matching beans available
  Solution => we add 2 annotaions
  -@Autowired - explicitly tells spring "use constructor injections here"
  -@Nullable - Tells spring this param is optional (ok to be null)

Sprint 3 - DataSink and Alerting

As of right now my data just gets logged and then it disappears we need to
1- save the data into postgreSQL (perm storage)
2- send fraud alerts to a seperate kafka topic (for downstream systems )
3- route transaction based to fraud detection result

spring.datasource.* -> tells spring how to connect to docker postgresql
spring.jpa.hibernate.ddl-auto=update -> Hibernate automatically creates/updates tables based on my entities
spring.jpa.show-sql=true -> logs sql quries (debugging)


JPA needs a java class that maps to a db table. this entity is going respresent one row in fradulent_transactions table


FraudulentTransaction.java 
is is object that is going to fill our fraudulent_transaction table in postgreSQL
dbTransactionID will autoincrement

for the kafka fraud alerts 
fraud-consumer is Both consumer and a producer
it consumes from transaction topic and it proccues fraud-alerts

finished saving to db for persistence data
flow so far
1. Producer generates transaction with amount > $900
2. Consumer receives it from `transactions` topic
3. FraudDetectionService flags it as fraud (HIGH_VALUE)
4. FraudPersistenceService saves to PostgreSQL
5. FraudAlertService sends to `fraud-alerts` topic
6. Console consumer shows the alert
7. Database query shows the saved record


So far the project has been just been making sure that everything works 
now i add the monitoring service
this inclues preformance tracking , capacity tracking and debuggin
Just because something works doesn't mean it is useful, I need to add visualizations


- How many transactions per second are we processing?
- What percentage are fraudulent?
- Is the system slowing down under load?
- Are there any bottlenecks?


the tech stack used for this sprint 4
- Micrometer (metrics collection library)
- Spring Boot Actuator (production-ready features)
- Prometheus (metrics storage & querying)
- Grafana (visualization - optional)


after some confguration 
http://localhost:8081/actuator/prometheus

this is what promethues scrapes every 15seconds to collect metrics

JVM provides some metrics by we need our customsones like, the % of the transacation that are frauds.

metrics we need to tracK:
total Transaction Counters
Fraudulent transaction Counters
Clean transaction counters
detection latency timer 

using the micrometer library for types, Couter types dont go down so we can just use Counter for those 

MeterRegistry is like a catalog systems which holds all the metrics
springboot creates one MeterGresitry when the app starts, we use it to create and register the metrics, we can also use micrometers to expose the metrics to Promotheus at /actuator/prometheus


MetricService basically just increments the counter and the TransactionConsumer uses it.

It starts by adding to the total # of transactions and then starts the timer, to see how fast the fraud i detected. 
inside the try catch it checks for fraud-> if cleans increments clean and moves to finally to stop timer 
if fraud increments fraud and moves to finaly to stop timer

prometheus works fine! I am just going to ad a Grafana Dashboard for prettier visuals (makes it look allot nicer then the prometheus graph)


SPRINT 5 FINALLY THE HARDEST PART OF THE ENTIRE THING
adding the ML pipeline for checking thee fraudulent transactions

basic task breakdown for this sprint (pain for the next 3 hours)
1.Understand the ML workflow and ONNX architecture
2.Setting up isolated python env with proper dependencies
3.exploring the kaggle credit card dataset (284k transactions)
4.Traning RandomForest with imbalance handling
5.Exporting to ONNX format with schema definition
6.Integrating the .onnx model into my java consumer
7.Testing for performance and accuracy
8. Updating detection methods and tuning thresholds

- Rule-based: 70% accuracy, 30% false positive rate
- ML-based: 95%+ accuracy, <5% false positive rate

1. Python (training): Pandas, Scikit-Learn, data science tools
2. Java (production): Spring Boot, real-time inference, low latency

The solution: ONNX (Open Neural Network Exchange)

- Universal ML model format
- Train in Python, run anywhere
- Supported by Microsoft, Facebook, AWS, Google
- Industry standard for ML deployment


instead of using just the usingRULES we need to get the following workflow for the app

Transaction → FraudDetectionService
                ↓
            extractFeatures() (7 features)
                ↓
            ONNX Model (loaded at startup)
                ↓
            Neural network inference
                ↓
            return fraud probability (0.0 - 1.0)
                ↓ (if > 0.5 threshold)
            return true/false


we need have 2 phases for this sprint 
data science (python)
need to train the raw data -> clean it -> engineer features -> train and then export the ONX

phase2 is to load ONNX ->transaction -> extract same features -> inference and then prediciton

the feature engineering must match exactly between training and inference

my dataset 

Credit Card Fraud Detection from Kaggle
(real world fraud distribution and is preprocessed and well documented)
it being from kaggle also means I can benchmark my results


python libraries 
pandas, scikit-Learn, imbalance-Learn, ONNX and skl2onnx, Matplotlib/Seaborn


activate python env
source ml-env/bin/activate

deactivate


explore_data.py -> it reads my creditcard.csv file and validates if it loaded properly 
- This ensures that the data quality before training 
-counts the legitimate vs fraudulent transactions 
(proves we need a SMOTE/class weights - i cant train on imabalnce directly)

produces 4 PNG files in  data/figures/ :
class_distribution.png - fraud vs legitimate counts
time_analysis.png - transactions by hour
amount_analysis.png - amount distributions
feature_correlation.png - which features matter most


train_model.py
1 - loads data 
2 - split data (creates train 64%),validation 16% and test 20% 
3 - Apply SMOTE - balances training data 
4 - Train the model with 100 trees
5 -  Tests on validation and test set with proper metric
6 - feature improtances (shows what features matters most)
7 - Save Model - saves .pkl file for later ONNX conversion

first training:

======================================================================
✅ TRAINING COMPLETE!
======================================================================

📁 Generated files:
   - models/fraud_model.pkl (trained model)
   - models/model_metadata.pkl (metrics & config)
   - models/feature_names.txt (feature reference)
   - models/confusion_matrix.png
   - models/roc_curve.png
   - models/feature_importance.png

🎯 Model Performance Summary:
   Precision: 0.5772
   Recall:    0.8776
   F1-Score:  0.6964
   ROC-AUC:   0.9785


  
catches 88% of all the fraud exceeding the 85% target
however precision is bad 57.72 percent meaning i have a 42% fast positive rate 

Fix: increase the max_depth to 15-20 meaning more complex trees, also add class_weights='balanced_subsample'

second training:
======================================================================
✅ TRAINING COMPLETE!
======================================================================

📁 Generated files:
   - models/fraud_model.pkl (trained model)
   - models/model_metadata.pkl (metrics & config)
   - models/feature_names.txt (feature reference)
   - models/confusion_matrix.png
   - models/roc_curve.png
   - models/feature_importance.png

🎯 Model Performance Summary:
   Precision: 0.7830
   Recall:    0.8469
   F1-Score:  0.8137
   ROC-AUC:   0.9799


   the files produces that i need to use is the models/fraud_model.onnx 
   this is the java-ready model

   models/FEATURE_ORDER.md (this is the documentation of rhte java integration)

we can run it a third time on my personal 7 features from the Kaggle Data
normalized_amount = Amount / 10000.0
hour_of_day = Extracted from Time field
day_of_week = Calculated from Time
is_weekend = 1.0 if Sat/Sun, else 0.0
payment_method_encoded = Simulated (0-4), correlated with fraud patterns
merchant_hash = Random (0-999)
location_hash = Random (0-999)

trains RandomForest on these 7 features with SMOTE balancing 
Exported ONNX with 7 inputs (matching my java code)
saves to fraud_model.onnx


Sprint 5: COMPLETE ML Pipeline 

1- train_model.py
-created python script to train a random forest classifier for fraud detection
-used ceredit card fraud detection dataset
-features v1-v28 (PCA-transaformed),Amount, Time
my rule based detection was so timple, so using ML made sense, it can detect subtle fraud indicators i cant even define
- Random Forest chosen:
  - Handles non-linear relationships well
  - works with imbalaned data (fraud is rare)
  -fast inference (5-15ms)
  -interpretable
the results:
Precision: 78.3% - When we say "fraud", we're right 78% of the time
Recall: 84.7% - We catch 85% of actual frauds

2- The ONNX model export  -> fraud_model.onnx
Exported train model to ONNX format and placed it in the resources
Choosing onnx is simple because i wanted real time fraud dectction which is sub 50ms. Running python as a seperate service would add 50-200 ms network overhead

3- Transaction Model expansion
I had to add more feilds from 8 to 36 
v1-v28 (the PCA features from training data) + time (nomalized timestamp for ML)

ML model was trained on 30 features, and we can change training and the java object must match the input 
Transaction velocity: How many transactions in last hour?
Location patterns: Is this location unusual for this user?
Amount patterns: Is this amount typical for this merchant?
Time patterns: Is this an unusual time for this user?
The dataset we used already had these pre-computed and anonymized via PCA (Principal Component Analysis) for privacy.


4- TransactionGenerator upgrade to accomodate the new trained model

5- featureEngineeringService
Ml needed specific input format : 30-element float array 
normalization matters(ex $500 to 0.5)
dection service forcuses on ML inference 
feature service handles data transformation 

6. added ONNX runtime integration 
OrtEnvironment: Singleton - manages ONNX runtime lifecycle
OrtSession: Loads model once at startup (not per transaction)
Bean management: Spring handles lifecycle, prevents memory leaks

7-FraudDetectionService ML integration
Single Responsibility:
Feature extraction → FeatureEngineeringService
ML inference → FraudDetectionService
Persistence → FraudPersistenceService


the final System Architecture

TRANSACTIONGEN (creats v1-v28 with realistic distribution)

sends to kafka topic :transactions

this is where the FraudConsumer does the work
1.FeatureEngineeringService 
  -normalize(amount,time,v1-v28);
2.FraudDectionService
  -Load ONNX model (startup)
  -Create Input tensor
  -Run ML inference (~10ms)
  -Extract from sklearn Map Structure
3.FraudPersistenceService 
  -Save to postgreSQL 
4.FraudAlertService 
  -publich to fraud-alerts topi
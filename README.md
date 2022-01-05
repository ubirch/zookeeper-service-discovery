# Ubirch Zookeeper Service Discovery

This repository is a proof of concept of using Zookeeper as a service discovery mechanism for microservices.

## Flow of information

The following image represents a configuration sequence for a service provider and a service consumer by means of using Zookeeper.

![Sequence](assets/sequence.png)

## Project structure

The project is organized into three modules:

* `common`: it represents a collection of common tools. In particular, it provides, an abstraction on how to get started via Apache Curator.
* `service`: it represents the service provider that offers an endpoint to create "cats". "Cats" created here are not fancy or anything. 
* `discovery`: it represents the service consumer that calls on the service provider's endpoint to create cats.

## How to run

Download Zookeeper from https://zookeeper.apache.org/. At the time of writing this document, the version used was: 3.7.0. Follow the instructions here to get started with Zookeeper. https://zookeeper.apache.org/doc/current/zookeeperStarted.html.

Compile the demo
```bash
mvn clean package
```

Run the service provider
```bash
java -cp service/target/service-0.0.1.jar com.ubirch.service.App
```

Run the service consumer
```bash
java -cp discovery/target/discovery-0.0.1.jar com.ubirch.discovery.App
```


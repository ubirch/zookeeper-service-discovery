# Ubirch Zookeeper Service Discovery and Leadership Election

This repository is a proof of concept of using Zookeeper as a service discovery mechanism for microservices and leadership election. 

Zookeeper is a highly reliable distributed service that enables microservices to control configurations, naming, and coordination of processes. You can learn more about it here:  https://zookeeper.apache.org/doc/current/zookeeperOver.html and if you have some time for a video, here: https://youtu.be/Vv4HpLfqAz4.

`Service discovery` is a required characteristic in distributed systems. Microservices need to learn how to observe with which systems they can talk based on their needs, or availability. Service discovery is the process that is used for services to register their presence, and available controls; and for services to use registered services. A very noble aspect of this discovery is that consumer can be informed about changes in the provider, whether they are not present, or if their configuration changed, etc.

Another very equally important concept in distributed services is `leadership among microservices`. That's to say that among the instances of one particular microservice, there is a leader instance, which is in charge of executing a particular set of tasks, which are not meant for all instances to execute.

## Project structure

The project is organized into three modules:

* `common`: it represents a collection of common tools. In particular, it provides, an abstraction on how to get started via [Apache Curator](https://curator.apache.org/).
* `service`: it represents the service provider that offers an endpoint "create" to create "cats". "Cats" created here are not fancy or anything, they just represent "something" that is processed. This service has a special function that will be only activated when the corresponding instance has gained leadership over its kinds. This `special function` is the creation of `yellow cats`, otherwise, `red cats` are created.
* `discovery`: it represents the service consumer that calls on the service provider's endpoint to create cats.

## Flow of information for a service discovery

The following image represents a configuration sequence for a service provider and a service consumer by means of using Zookeeper.

![Sequence](assets/sequence.png)

## Flow of information for leadership

![Leadership](assets/leadership.png)

## How to run

Download Zookeeper from https://zookeeper.apache.org/. At the time of writing this document, the version used was: 3.7.0. Follow the instructions here to get started with Zookeeper. https://zookeeper.apache.org/doc/current/zookeeperStarted.html.

Have Zookeeper running.

`bin/zkServer.sh start`

> for extra help in visualizing zookeeper, you can use [Pretty Zoo](https://github.com/vran-dev/PrettyZoo)

Compile the code
```bash
mvn clean package
```

Run as many service providers as desired. Two or three are nice to see what happens in a more organized way. Change the port accordingly.

```bash
java -cp service/target/service-0.0.1.jar com.ubirch.service.App 8081
```

Run the service consumer - the one that will call the service provider. - Many instance can also be started.
```bash
java -cp discovery/target/discovery-0.0.1.jar com.ubirch.discovery.App
```

## How to see it in action

### Service Discovery

If you have all running in the sequence presented above, you will see that the service consumer will start outputting "cats".

![Cats Creation](assets/cat_creation.png)

However, if you stop the service provider, you shall see that the discovery service detects these changes.
![Cat service gone](assets/cat_service_gone.png)

### Leadership selection

By default, the first application that gets registered on Zookeeper will be the leader of the pack. By running two instances of the cats' creator, you will see how it is possible for the apps to take leadership, and relinquish when exiting the pack. 

Taking leadership

![Taking leadership](assets/taking_leadership.png)

Relinquishing leadership

![Relinquishing leadership](assets/relinquish_leadership.png)

Creating Yellow Cats changes instance as the leadership changes

![Leadership on app with port 8081](assets/leadership_on_8081.png)

![Leadership on app with port 8082](assets/leadership_on_8082.png)

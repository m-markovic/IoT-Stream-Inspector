# IoT-Stream-Inspector

Requires Java JDK 8 and maven 3.3. Refer to the following links to install these:
  * http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
  * https://maven.apache.org/download.cgi

## Prerequisite

This project depends on version 0.9.7 of C-Sparql which is published for Maven at the time of writing (see https://github.com/streamreasoning/CSPARQL-engine/issues/14), so you need to compile and install this locally:

```
cd ..
git clone https://github.com/streamreasoning/CSPARQL-engine.git
cd CSPARQL-engine
mvn install
cd ../IoT-Stream-Inspector
```

## Configuration

All data and configuration must reside in the ```config/iotstreams``` directory. To get started, copy the example configuration:
```
cp -r config-example/ config
```

Now add/remove/update files to reflect your configuration. The structure is like this:
```
config/
  |-- iotstreams/
  |     |-- <any name>  Set up one C-SPARQL query along with the associated provenance inference
  |     |     |-- csparql-query.rq  Query for C-SPARQL
  |     |     |-- init.ttl          Ontology to initialize models with
  |     |     |-- coldstart/
  |     |     |     |-- <any name>.rq  SPARQL update to execute when no previous provenance is present 
  |     |     |     |-- <any name>.rq  ... any number of these ... 
  |     |     |-- warm/
  |     |           |-- <any name>.rq  SPARQL update to execute when previous provenance is present 
  |     |           |-- <any name>.rq  ... any number of these ...
  |     |
  |     |-- <any name>  ... any number of these ...
```

## Building

```
mvn clean package
```

## Running

```
mvn package && java -jar target/iotstreams-jar-with-dependencies.jar
```

## Creating an Eclipse project

If you would like to edit this project in Eclipse, we recommend you create a project file before starting Eclipse, like this:
```
mvn eclipse:clean eclipse:eclipse
```

## Using live data

TODO: Revise and expand

To run the IotStreams engine with live data you will need to
  * Change ```csparql.properties```, setting ```esper.externaltime.enabled``` to ```false```.
  * Write Java code that constructs an instance of ```uk.ac.abdn.iotstreams.csparql.IotStreamsEngine``` (call this ```engine```)
  * Write Java code that encodes live data as SSN in a Jena ```Model``` (call this ```model```) and adds the data like this: ```engine.apply(ZonedDateTime.now()).accept(model)```. 

## Run static analyses

To see if the updated code has any issues according to PMD or Findbugs, run
```
mvn site
```
then open ```target/site/project-reports.html```



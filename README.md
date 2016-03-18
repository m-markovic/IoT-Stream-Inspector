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

## Building

```
mvn clean package
```

## Running the example application

```
mvn package && java -jar target/iotstreams-jar-with-dependencies.jar
```

## Creating an Eclipse project

If you would like to edit this project in Eclipse, we recommend you create a project file before starting Eclipse, like this:
```
mvn eclipse:clean eclipse:eclipse
```

## Making your own configuration

### Live or recorded data?

In the project root, the file ```csparql.properties``` contains the setting
```
esper.externaltime.enabled=true
```
When the value is ```true```, C-SPARQL will make windows based on timestamps
in the data. When the values is ```false```, C-SPARQL will make windows
based on the system's current time when data was put on the stream.

Keep the values as ```true``` if you intend to use recorded data.
Set the value to ```false``` if you wish to stream live data.

### Queries, updates and ontologies

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

## Provide your own data from Java

To provide recorded data, make sure 
```esper.externaltime.enabled=true
```
(see above) then follow this pattern:
```
final IotStreamsEngine engine = IotStreamsEngine.forRecordedData(m -> m.write(System.out, "N3"));
//For each data point:
final ZonedDateTime t = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]");
engine.apply(t).accept(parseN3("<http://example> <http://answer> 42"));
```
The full example can be seen in ```src/main/java/uk/ac/abdn/iotstreams/ExampleMain.java```

TODO: Live data example

## Run static analyses

To see if the updated code has any issues according to PMD or Findbugs, run
```
mvn site
```
then open ```target/site/project-reports.html```



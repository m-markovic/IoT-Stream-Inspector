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
cp -r config-example/ config
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
in the data. When the value is ```false```, C-SPARQL will make windows
based on the system's current time when data was put on the stream.
It seems that C-SPARQL will ignore provided timestamps when the value is false.

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
  |     |-- <any name>  Set up one C-SPARQL query along with the associated  inference
  |     |     |-- csparql-query.rq  Query for C-SPARQL
  |     |     |-- init.ttl          Ontology to initialize models with
  |     |     |-- coldstart/
  |     |     |     |-- <any name>.rq  SPARQL update to execute when no previous infefence has been made 
  |     |     |     |-- <any name>.rq  ... any number of these ... 
  |     |     |-- warm/
  |     |           |-- <any name>.rq  SPARQL update to execute when previous infefence has been made  
  |     |           |-- <any name>.rq  ... any number of these ...
  |     |
  |     |-- <any name>  ... any number of these ...
```

**Note**: Blank nodes are not allowed.
This limitation applies to the data input from Java (see below)
as well as the output from C-SPARQL queries.

## Provide your own data from Java

Your code should first create an instance ```engine``` of ```uk.ac.abdn.iotstreams.csparql.IotStreamsEngine```. This will read the configuration in ```config/iotstreams``` and set itself up accordingly.

For every set of data to put on the stream, your code must encode
that data in a Jena Model ```m``` and then either call
```
engine.accept(m);
```
if the triples in ```m``` should be associated with current system time,
or
```
engine.apply(...timestamp...).accept(model)
```
to set a specific time stamp.

*Remark*: Your program may construct any number of instances of 
```uk.ac.abdn.iotstreams.csparql.IotStreamsEngine```
(although this has not been tested).
The instances will use the same configuration files but but will be
completely independt wrt. the data your program provides to each of them.

Below are examples of use with recorded or live data.
The full examples can be seen in ```src/main/java/uk/ac/abdn/iotstreams/ExampleMain.java```

### Recorded (non-live) data

When using recorded data, you will likely want to provide
the original time stamps and make C-SPARQL use these for
making time windows. First make sure 
```
esper.externaltime.enabled=true
```
in ``` csparql.properties ```
(see above) then follow this pattern:
```
final IotStreamsEngine engine = IotStreamsEngine.forRecordedData(m -> m.write(System.out, "N3"));
//For each data point:
final ZonedDateTime t = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]");
engine.apply(t).accept(parseN3("<http://example> <http://answer> 42"));
```

### Live data

When using live data, you will likely want to use current time
and allow C-SPARQL to use this for
making time windows. First make sure 
```
esper.externaltime.enabled=false
```
in ``` csparql.properties ```
(see above) then follow this pattern:
```
final IotStreamsEngine engine = IotStreamsEngine.forLiveData(m -> m.write(System.out, "N3"));
//Whenever you receive a reading:
Model m = ModelFactory.createDefaultModel();
m.add(...data as triples...);//As many of these as you need...
engine.accept(m);
```

### Handling the inferred triples

The parameter passed to ```IotStreamsEngine.forLiveData```/```IotStreamsEngine.forRecordedData``` is a callback function that is called whenever
new triples are inferred. In the above examples, we used
```
m -> m.write(System.out, "N3")
```
which is a handy lambda notation for implementing a ```Consumer<Model>```.

You may also implement a ```Consumer<Model>``` in a more classic way:
```
class MyAction implements Consumer<Model> {
    public void accept(Model inferredTriples) {
        ... do stufff with inferredTriples, which is a Jena Model ...
    }
}
```
You can then pass ```MyAction``` objects like this:
```
MyAction action = new MyAction();
IotStreamsEngine engine = IotStreamsEngine.forRecordedData(action);
```

## Technical notes

### Logging

This project uses [SLF4J](http://www.slf4j.org/) for logging.
The logs will be printed on the terminal (```stderr```).
To change this, you need to update ```pom.xml```.
Find the below section and change it according to the
[SLF4J instructions](http://www.slf4j.org/manual.html#swapping).
```
        <!--  Defines where slf4j logs should be directed -->
        <dependency>
          <groupId>org.slf4j</groupId>
          <artifactId>slf4j-simple</artifactId>
          <version>1.7.16</version>
        </dependency>
```

### Run static analyses

To see if the updated code has any issues according to PMD or Findbugs, run
```
mvn site
```
then open ```target/site/project-reports.html```



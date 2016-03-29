# The IoT-Streams Inspector code

As part of the FoodSafety project we developed a more general system
to simplify inference over streams of IoT data
modelled in the semantic web framework.
The system is written in Java and it builds on Apache Jena and C-SPARQL.
The code is available from
> https://github.com/m-markovic/IoT-Stream-Inspector/

## What it does

The following diagram illustrates the data flow
of the IoT-Streams Inspector:
![Data flow](https://github.com/m-markovic/IoT-Stream-Inspector/raw/master/docs/run-diagram.png "Data flow")

The project-specific code continually receives data from 
the IoT devices and passes this data on to an ```IotStreamsEngine``` 
in the form of a Apache Jena model.
The engine encodes the data for a C-SPARQL stream.
C-SPARQL continually runs the project-specific
streaming SPARQL queries.
If the new data causes one of these to generate a result,
C-SPARQL passes that result on to an ```IotStreamsFormatter```.
The ```IotStreamsFormatter``` performs the project-specific inference,
defined by a number of SPARQL update queries.
If the SPARQL update queries caused new triples inferred,
these new triples will be supplied to the project-specific Java code
where they may be stored persistently and/or acted upon.

Here's a Java example of how you would create
an instance of ```IotStreamsEngine```:

```java
IotStreamsEngine engine = 
    IotStreamsEngine.forLiveData(
        m -> m.write(System.out, "N3")
    );
```
Your code specifies a callback function to handle inferred triples.
The inferred triples will be provided as an Apache Jena ```Model```.
In the example, inferred triples will be continually dumped as Notation3
in the terminal running your code.

Whenever your code receives data from an IoT device,
use the ```accept``` method to pass it on to the ```IotStreamsEngine```:

```java
Model model = <Jena model of the data>;
engine.accept(model);
```

Successful inference will cause the callback above
(```m -> m.write(System.out, "N3")```)
to be activated.

The above Java code is provided in a readily executable context
in the GitHub project, in the file ```ExampleMain.java```.

## Setting it up

The call to ```IotStreamsEngine.forLiveData()``` above will
cause the IoT-Streams Inspector to automatically configure itself
from the current working directory.

Each of your streaming SPARQL queries must be provided in a subdirectory
along with
  * The ontology to use for the inference
  * "Coldstart" SPARQL update queries for inferring the first set of triples
  * "Warm" SPARQL update queries for inferring a new set of triples, when a set of triples have previously been inferred.

The configuration format is illustrated in the below diagram.
More detailed documentation is available on GitHub along
with a full example configuration.

![Setting up your inference](https://github.com/m-markovic/IoT-Stream-Inspector/raw/master/docs/config-diagram.png "Setting up your inference")

## Coldstart inference and warm inference explained

At its core, the inference of the Iot-Streams Inspector maintains
an inferred state per C-SPARQL query. 
When the query yields a new set of results (corresponding to a time window),
the associated SPARQL update queries will be executed
in a graph that contains
  * The ontology
  * The new window of query results
  * The previous state (if one exists)

If any new triples are added by the SPARQL update queries,
these will make up the new state.

When your C-SPARQL query yields its first set of results,
there will be no previous state.
This is called a "coldstart" and the associated set of SPARQL update
queries will be executed. If no new triples are added to the graph,
the next window of results will also be handled by the "coldstart"
SPARQL update queries.
When the updates finally add new triple to a graph,
these triples will become the first inferred state for the
C-SPARQL query, and all subsequent results sets yielded
by the query will be handled by the "warm" SPARQL update queries.
Ever time new triples are added by SPARQL qupte queries - coldstart
or warm - the triples will be passed to the project-specific Java
for persistent storage and/or appropriate action.


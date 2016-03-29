package uk.ac.abdn.iotstreams.csparql;

import java.time.ZonedDateTime;
import java.util.function.Consumer;
import java.util.function.Function;

import uk.ac.abdn.iotstreams.util.IotStreamsException;
import uk.ac.abdn.iotstreams.util.Logging;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;
import eu.larkc.csparql.common.config.Config;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;

/**
 * 
 * @author nhc
 *
 * An IotStreamsEngine is a specific CsparqlEngine with methods
 * and queries for this project.
 */
public final class IotStreamsEngine
    extends CsparqlEngineImpl 
    implements 
        Function<ZonedDateTime, Consumer<Model>>,
        Consumer<Model> {
    
    /** This engine's sole stream */
    private final RdfStream rdfStream = new RdfStream("http://iotstreams");
    
    /** Object to which all inferences will be passed */
    private final Consumer<Model> inferredTripleConsumer;
    
    /** Number of quadruples put on stream so far */
    private long numQuads = 0;
    
    /**
     * Verifies that C-SPARQL is configured for using recorded data,
     * then returns an IotStreamsEngine.
     * @param inferredTripleConsumer Object to which all inferences will be passed
     * @return A fresh instance of IotStreamsEngine
     */
    public static IotStreamsEngine forRecordedData(final Consumer<Model> inferredTripleConsumer) {
        if (Config.INSTANCE.isEsperUsingExternalTimestamp()) {
            return new IotStreamsEngine(inferredTripleConsumer);
        } else {
            throw IotStreamsException.configurationError(
                    "To use recorded data, csparql.properties must contain the line "
                    + "'esper.externaltime.enabled=true'");
        }
    }
    
    /**
     * Verifies that C-SPARQL is configured for using live data,
     * then returns an IotStreamsEngine.
     * @param inferredDataConsumer Object to which all inferences will be passed
     * @return A fresh instance of IotStreamsEngine
     */
    public static IotStreamsEngine forLiveData(final Consumer<Model> inferredDataConsumer) {
        if (Config.INSTANCE.isEsperUsingExternalTimestamp()) {
            throw IotStreamsException.configurationError(
                    "When using live data, csparql.properties must not contain the line "
                    + "'esper.externaltime.enabled=true'");
        } else {
            return new IotStreamsEngine(inferredDataConsumer);
        }
    }
    
    /**
     * Initializes this engine.
     * @param inferredTripleConsumer Object to which all inferences will be passed
     */
    private IotStreamsEngine(final Consumer<Model> inferredTripleConsumer) {
        this.inferredTripleConsumer = inferredTripleConsumer;
        this.initialize();
        this.registerStream(this.rdfStream);
        new Configurator(this, this.inferredTripleConsumer);
    }
    
    /**
     * Logs a status message
     */
    public void log() {
        Logging.info(String.format("%d quadruples put on stream", this.numQuads));
    }
    
    /**
     * Utility method for specifying a timestamp to assoaciate with a Jena Model.
     * Usage: engine.apply(myTimeStamp).accept(myModel)
     */
    @Override
    public Consumer<Model> apply(final ZonedDateTime t) {
        return m -> this.add(t, m);
    }
    
    /**
     * Add a Jena Model for the current time (i.e. NOW)
     */
    @Override
    public void accept(final Model m) {
        this.add(ZonedDateTime.now(), m);
    }
    

    /**
     * Adds all triples in the given model to C-SPARQL
     * @param t Every triple in m will be passed with this timestamp
     * @param m A Model containing the triples to add
     */
    private void add(final ZonedDateTime t, final Model m) {
        final long timestamp = t.toInstant().toEpochMilli();
        final StmtIterator it = m.listStatements();
        while (it.hasNext()) {
            final Statement triple = it.nextStatement();
            final RDFNode o = triple.getObject();
            if (o.isAnon()) {
                IotStreamsException.internalError(String.format("Blank node in %s", m.toString()));
            }
            this.rdfStream.put(new RdfQuadruple(
                    triple.getSubject().getURI(),
                    triple.getPredicate().getURI(),
                    o.isResource() ? 
                            o.asResource().getURI() : 
                                String.format(
                                        "\"%s\"^^%s", 
                                        o.asLiteral().getLexicalForm(),
                                        o.asLiteral().getDatatypeURI()),
                    timestamp));
            this.numQuads += 1;
        }
    }
}

package uk.ac.abdn.iotstreams.csparql;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Observable;
import java.util.Optional;
import java.util.function.Consumer;

import uk.ac.abdn.iotstreams.util.IotStreamsException;
import uk.ac.abdn.iotstreams.util.Logging;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;
import com.hp.hpl.jena.update.UpdateAction;

import eu.larkc.csparql.common.RDFTable;
import eu.larkc.csparql.common.RDFTuple;
import eu.larkc.csparql.core.ResultFormatter;

/**
 * 
 * @author nhc
 *
 * An IotStreamsFormatter listens to one C-SPARQL query.
 * When the query yields a result set (i.e. triples for one window),
 * the IotStreamsFormatter executes the registered SPARQL updates
 * and handles results.
 */
class IotStreamsFormatter extends ResultFormatter {
    /** Name picked up from directory - used for logging */
    private final String queryName;
    
    /** The SPARQL update queries to execute */
    private EnumMap<Stage, HashMap<String, String>> sparqlUpdateQueries =
            new EnumMap<Stage, HashMap<String, String>>(Stage.class);

    /** Model containing the ontology/static knowledge */
    private Optional<OntModel> staticKnowledge = Optional.empty();

    /** Model storing the received triples along with the ontology */
    private Optional<OntModel> m = Optional.empty();

    /** Model containing the latest inferred triples */
    private Optional<Model> lastInference = Optional.empty();

    /** Final resting place for inferred triples */
    private final Consumer<Model> inferredTripleConsumer;

    /**
     * Registers the query name and prepares for configuration by
     * addSparql() and setOntology()
     * @param queryName Name picked up from directory - used for logging
     * @param inferredTripleConsumer All inferred triples will be passed to this object
     */
    IotStreamsFormatter(final String queryName, final Consumer<Model> inferredTripleConsumer) {
        this.queryName = queryName;
        this.inferredTripleConsumer = inferredTripleConsumer;
        //Initialize SPARQL update query collections
        for (Stage s : Stage.values()) {
            this.sparqlUpdateQueries.put(s, new HashMap<String, String>());
        }
    }
    
    /**
     * Called when C-Sparql yields a set of results from the assoaciated C-SPARQL query.
     * Adds all triples to the internal model, then runs infer().
     */
    @Override
    public synchronized void update(final Observable ignored, final Object rdfTableUntyped) {
        final RDFTable rdfTable = (RDFTable) rdfTableUntyped;
        this.m = Optional.of(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF));
        this.m.get().add(this.staticKnowledge.get());
        rdfTable.stream()
            .map(this::convert)
            .forEach(s -> this.m.get().add(s));
        this.infer();
    }
    
    /**
     * Converts a in an RdfTuple to a Jena Statement.
     * @param t must have URIs as element 0 and 1, and a URI or a Literal as element 2
     * @return The Statement representing elements 0,1,2 of t
     */
    private Statement convert(final RDFTuple t) {
        RDFNode o;
        final String[] oSplit = t.get(2).split("\\^\\^"); //Split if this is a typed literal
        if (oSplit.length == 2) {
            o = this.m.get().asRDFNode(NodeFactoryExtra.createLiteralNode(
                    oSplit[0].substring(1, oSplit[0].length() - 2), 
                    null, 
                    oSplit[1]));
        } else {
            o = this.m.get().createResource(t.get(2));            
        }
        try { //Compose and return the Statement
            return this.m.get().createStatement(
                    this.m.get().createResource(t.get(0)), 
                    this.m.get().createProperty(t.get(1)),
                    o);
        } catch (final Exception e) {
            throw IotStreamsException.internalError(String.format("Problem converting %s", t.get(2)));
        }
    }

    /**
     * Runs coldstart or warm SPARQL queries, updating Jena models as appropriate.
     */
    private void infer() {
        final Model workingCopy = ModelFactory.createDefaultModel();
        final long s = workingCopy.size();
        workingCopy.add(this.m.get());
        if (this.lastInference.isPresent()) {
            //Use inferred triples from latest successful inference
            workingCopy.add(this.lastInference.get());
            this.sparqlUpdateQueries.get(Stage.WARM)
                .forEach((name, query) -> update(name, query, workingCopy));
            workingCopy.remove(this.m.get());
            workingCopy.remove(this.lastInference.get());
            if (workingCopy.size() > s) { //we inferred something
                this.lastInference = Optional.of(workingCopy);
                this.inferredTripleConsumer.accept(workingCopy);
            }
        } else { //First run
            this.sparqlUpdateQueries.get(Stage.COLDSTART)
                .forEach((name, query) -> update(name, query, workingCopy));
            workingCopy.remove(this.m.get());
            if (workingCopy.size() > s) { //we inferred something
                this.lastInference = Optional.of(workingCopy);
                this.inferredTripleConsumer.accept(workingCopy);
            } else { //No inference - error
                Logging.warn(String.format("The coldstart SPARQL for %s did not infer anything", this.queryName));
            }
        }
    }
    
    /**
     * Parse&execute a SPARQL update query, logging stats about the execution.
     * @param name Name of the query, for logging
     * @param query The SPARQL text
     * @param provmod The model to update
     */
    private void update(final String name, final String query, final Model provmod) {
        final long beforeSize = provmod.size();
        final Instant before = Instant.now();
        UpdateAction.parseExecute(query, provmod);
        final Instant after = Instant.now();
        final long afterSize = provmod.size();
        Logging.info(String.format(
                "Query %s update %s: %d ms ; %d triples generated",
                this.queryName,
                name, 
                Duration.between(before, after).toMillis(),
                afterSize - beforeSize));
    }

    /**
     * Adds a SPARQL update query to the given stage.
     * @param stage Must be "coldstart" or "warm" to decide in which case the query will be executed.
     * @param name name of this update
     * @param content The text of the query.
     */
    public void addSparql(final String stage, final String name, final String content) {
        this.sparqlUpdateQueries.get(Stage.valueOf(stage.toUpperCase())).put(
                String.format("%s/%s", stage, name), 
                content);
    }

    /**
     * Loads the given ontology into the internal Jena model.
     * @param ontology Ontology, in TTL
     */
    public void setOntology(final String ontology) {
        this.staticKnowledge = Optional.of(ModelFactory.createOntologyModel());
        staticKnowledge.get().read(new ByteArrayInputStream(ontology.getBytes(StandardCharsets.ISO_8859_1)), null, "TTL");
    }
    
    /**
     * Stages in which SPARQL update queries are executed
     */
    private enum Stage {
      /** Coldstart: Nothing has been inferred yet */
      COLDSTART,
      /** Warm: Some triples have already been inferred */
      WARM;
    };
}
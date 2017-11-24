package kunalherkal.poc;

import com.complexible.common.rdf.query.resultio.TextTableQueryResultWriter;
import com.complexible.stardog.api.*;
import com.complexible.stardog.api.admin.AdminConnection;
import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
import org.openrdf.model.IRI;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.rio.RDFFormat;

import java.io.FileInputStream;
import java.io.IOException;

import static com.complexible.common.rdf.model.Values.iri;
import static com.complexible.common.rdf.model.Values.literal;

public class StardogClient {
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String SERVER_URL = "http://localhost:5820/";
    private static final String DB_NAME = "myNewDB";
    private static final String NS = "http://api.stardog.com/";

    private static final IRI IronMan = iri(NS, "ironMan");
    private static final IRI Thor = iri(NS, "thor");


    public static void main(String[] args) throws IOException {
        createAdminConnection();
        ConnectionPool connectionPool = createConnectionPool();

        addIntoDBUsingRDFFile(connectionPool);
        addIntoDBUsingAPI(connectionPool);

        connectionPool.shutdown();
    }

    private static void addIntoDBUsingAPI(ConnectionPool connectionPool) {
        Connection connection = connectionPool.obtain();
        connection.begin();

        connection.add().statement(IronMan, RDF.TYPE, FOAF.PERSON)
                .statement(IronMan, FOAF.NAME, literal("Anthony Stark"))
                .statement(IronMan, FOAF.TITLE, literal("Iron Man"))
                .statement(IronMan, FOAF.GIVEN_NAME, literal("Anthony"))
                .statement(IronMan, FOAF.SURNAME, literal("Stark"))
                .statement(IronMan, FOAF.KNOWS, Thor);
        connection.commit();
        connection.close();
        connectionPool.release(connection);
    }

    private static void addIntoDBUsingRDFFile(ConnectionPool connectionPool) throws IOException {
        Connection connection = connectionPool.obtain();
        connection.begin();
        connection.add().io()
                .format(RDFFormat.N3)
                .stream(new FileInputStream("src/main/resources/marvel.rdf"));
        connection.commit();

        SelectQuery selectQuery = connection.select("PREFIX foaf:<http://xmlns.com/foaf/0.1/> select * { ?s rdf:type foaf:Person }");
        TupleQueryResult tupleQueryResult = selectQuery.execute();
        QueryResultIO.writeTuple(tupleQueryResult, TextTableQueryResultWriter.FORMAT, System.out);
        connection.close();
        connectionPool.release(connection);
    }

    private static ConnectionPool createConnectionPool() {
        ConnectionConfiguration connectionConfiguration = ConnectionConfiguration.to(DB_NAME)
                .server(SERVER_URL)
                .credentials(USERNAME, PASSWORD);
        ConnectionPoolConfig connectionPoolConfig = ConnectionPoolConfig.using(connectionConfiguration);

        return connectionPoolConfig.create();
    }


    private static void createAdminConnection() {
        AdminConnection adminConnection = AdminConnectionConfiguration.toServer(SERVER_URL)
                .credentials(USERNAME, PASSWORD)
                .connect();

        System.out.println("Printing databases:");
        adminConnection.list().forEach(item -> System.out.println(item));

        if (adminConnection.list().contains(DB_NAME)) {
            adminConnection.drop(DB_NAME);
        }

        adminConnection.disk(DB_NAME).create();
        System.out.println("Printing databases after adding database:");
        adminConnection.list().forEach(item -> System.out.println(item));
    }
}

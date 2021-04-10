package at.enactmentengine.serverless.object;


import com.mongodb.Block;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;

public class DatabaseAccess {
    private static final long workflowExecutionId = System.currentTimeMillis();
    private static MongoClient mongoClient;
    private static DatabaseAccess databaseAccess;
    private static List<Document> entries = new ArrayList<>();

    Block<Document> printBlock = new Block<Document>() {
        @Override
        public void apply(final Document document) {
            System.out.println(document.toJson());
        }
    };

    private DatabaseAccess() {

        //LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        //Logger rootLogger = loggerContext.getLogger("com.mongodb.driver");
        //rootLogger.setLevel(Level.OFF);

        final String host = "10.0.0.62";
        final int port = 27017;
        MongoCredential sim = MongoCredential.createCredential("user", "AFCL", "pw".toCharArray());
        mongoClient = MongoClients.create
                (MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(host, port))))
                        .credential(sim)
                        .build());
    }

    public static MongoClient getConnection() {
        if (mongoClient == null) {
            databaseAccess = new DatabaseAccess();
        }
        return mongoClient;
    }

    public static void saveLog(Event event, String functionId, Long RTT, boolean success, int loopCounter, long startTime, Type type) {
        Document log = new Document("workflow_id", workflowExecutionId)
                .append("function_id", functionId) // TODO store region and provider as well?
                .append("Event", event.toString())
                .append("RTT", RTT)
                .append("success", success)
                .append("loopCounter", loopCounter)
                .append("startTime", new Timestamp(startTime))
                .append("endTime", new Timestamp(startTime + RTT)) // TODO handle NULL value
                .append("type", type.toString());

        entries.add(log);
    }

    public static long getLastEndDateOverall() {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        return entries.stream()
                .filter(d -> d.getLong("workflow_id") == workflowExecutionId)
                .max(Comparator.comparing(d -> d.getDate("endTime")))
                .get()
                .getDate("endTime")
                .getTime();
    }

    public static long getLastEndDateOutOfLoop() {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        return entries.stream()
                .filter(d -> d.getLong("workflow_id") == workflowExecutionId &&
                        d.getInteger("loopCounter") == -1)
                .max(Comparator.comparing(d -> d.getDate("endTime")))
                .get()
                .getDate("endTime")
                .getTime();
    }

    public static long getLastEndDateOutOfLoopStored() {
        MongoClient client = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("AFCL");
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection("logs");
        return dbCollection.find(and(eq("workflow_id", workflowExecutionId), eq("loopCounter", -1)))
                .sort(descending("endTime"))
                .limit(1)
                .first()
                .getDate("endTime")
                .getTime();
    }

    public static long getLastEndDateInLoop() {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        return entries.stream()
                .filter(d -> d.getLong("workflow_id") == workflowExecutionId &&
                        d.getInteger("loopCounter") != -1)
                .max(Comparator.comparing(d -> d.getDate("endTime")))
                .get()
                .getDate("endTime")
                .getTime();
    }

    public static long getLastEndDateInLoopStored() {
        MongoClient client = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("AFCL");
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection("logs");
        return dbCollection.find(and(eq("workflow_id", workflowExecutionId), not(eq("loopCounter", -1))))
                .sort(descending("endTime"))
                .limit(1)
                .first()
                .getDate("endTime")
                .getTime();
    }

    public static void addAllEntries() {
        MongoClient client = getConnection();
        MongoDatabase mongoDatabase = mongoClient.getDatabase("AFCL");
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection("logs");
        dbCollection.insertMany(entries);
    }

    public static void close() {
        mongoClient.close();
    }

}

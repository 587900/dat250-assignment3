package no.hvl.dat250.h587900.assignment3;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static java.util.Collections.singletonList;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting up...");
        System.out.println();

        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase db = client.getDatabase("dat250-assignment3");
        MongoCollection<Document> collection = db.getCollection("collection");

        clearCollection(collection);
        System.out.println("----- Performing CRUD operations -----");
        insertDocument(collection);
        System.out.print("Query: "); System.out.println(queryDocument(collection));
        updateDocument(collection);
        deleteDocument(collection);
        bulkWrite(collection);
        System.out.println("----- Finished CRUD operations -----");

        System.out.println();

        clearCollection(collection);
        setupData(collection);
        System.out.println("----- Performing mapReduce and aggregation commands -----");
        System.out.print("mapReduceTotalPricePerCustomer: "); mapReduceTotalPricePerCustomer(collection);
        System.out.print("aggregateTotalPricePerCustomer: "); aggregateTotalPricePerCustomer(collection);
        System.out.print("mapReduceAvgQuantity: "); mapReduceAvgQuantity(collection);
        System.out.print("aggregateAvgQuantity: "); aggregateAvgQuantity(collection);
        System.out.println("----- Finished mapReduce and aggregation commands -----");

        client.close();

        System.out.println();

        System.out.println("Farewell! Closing...");
    }

    private static void clearCollection(MongoCollection<Document> collection) {
        collection.deleteMany(new Document());
    }

    private static void insertDocument(MongoCollection<Document> collection) {
        Document canvas = new Document("item", "canvas")
                .append("qty", 100)
                .append("tags", singletonList("cotton"));

        Document size = new Document("h", 28)
                .append("w", 35.5)
                .append("uom", "cm");

        canvas.put("size", size);
        collection.insertOne(canvas);
    }

    private static Document queryDocument(MongoCollection<Document> collection) {
        MongoCursor<Document> result = collection.find(eq("item", "canvas")).iterator();
        if (!result.hasNext()) return null;
        Document t = result.next();
        result.close();
        return t;
    }

    private static void updateDocument(MongoCollection<Document> collection) {
        collection.updateOne(eq("item", "canvas"), combine(set("size.uom", "in"), set("size.w", 14.2), set("size.h", 11.2)));
    }

    private static void deleteDocument(MongoCollection<Document> collection) {
        collection.deleteOne(eq("item", "canvas"));
    }

    private static void bulkWrite(MongoCollection<Document> collection) {
        List<WriteModel<Document>> models = new ArrayList<WriteModel<Document>>(); // for some reason the mongodb dependency forces language level 5 ??
        models.add(new InsertOneModel<Document>(new Document("item", "pen").append("qty", 50).append("tags", singletonList("wood"))));
        models.add(new InsertOneModel<Document>(new Document("item", "paper").append("qty", 200).append("tags", singletonList("dreams"))));
        models.add(new UpdateOneModel<Document>(eq("item", "paper"), set("qty", 250)));
        collection.bulkWrite(models);
    }

    private static void mapReduceTotalPricePerCustomer(MongoCollection<Document> collection) {
        String map = "function() { emit(this.cust_id, this.price); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        print(collection.mapReduce(map, reduce));
    }

    private static void aggregateTotalPricePerCustomer(MongoCollection<Document> collection) {
        print(collection.aggregate(Arrays.asList(Aggregates.group("$cust_id", Accumulators.sum("value", "$price")))));
    }

    private static void mapReduceAvgQuantity(MongoCollection<Document> collection) {
        // Finalizer has to be included in the 'reduce' function because there is no native 'finalize' support in this java library
        String map = "function() { for (var idx = 0; idx < this.items.length; idx++) { var key = this.items[idx].sku; var value = { count: 1, qty: this.items[idx].qty }; emit(key, value); } }";
        String reduce = "function(keySKU, countObjVals) { reducedVal = { count: 0, qty: 0 }; for (var idx = 0; idx < countObjVals.length; idx++) { reducedVal.count += countObjVals[idx].count; reducedVal.qty += countObjVals[idx].qty } reducedVal.avg = reducedVal.qty/reducedVal.count; return reducedVal; }";
        print(collection.mapReduce(map, reduce));
    }

    private static void aggregateAvgQuantity(MongoCollection<Document> collection) {
        List<Bson> aggregations = new ArrayList<Bson>();
        aggregations.add(Aggregates.match(gte("ord_date", cd("2020-03-01"))));
        aggregations.add(Aggregates.unwind("$items"));
        aggregations.add(Aggregates.group("$items.sku", Accumulators.sum("qty", "$items.qty"), Accumulators.addToSet("orders_ids", "$_id")));
        aggregations.add(Aggregates.project(Projections.fields(
                //Projections.include("orders_ids"),
                Projections.computed("value.count", new Document("$size", "$orders_ids")),
                Projections.computed("value.qty", "$qty"),
                Projections.computed("value.avg", new Document("$divide", Arrays.asList("$qty", new Document("$size", "$orders_ids"))))
                )));
        print(collection.aggregate(aggregations));
    }

    // copied data from the mongodb example site for aggregations
    private static void setupData(MongoCollection<Document> collection) {
        List<WriteModel<Document>> models = new ArrayList<WriteModel<Document>>();
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 1, cust_id: \"Ant O. Knee\", ord_date: new Date(\""+c("2020-03-01")+"\"), price: 25, items: [ { sku: \"oranges\", qty: 5, price: 2.5 }, { sku: \"apples\", qty: 5, price: 2.5 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 2, cust_id: \"Ant O. Knee\", ord_date: new Date(\""+c("2020-03-08")+"\"), price: 70, items: [ { sku: \"oranges\", qty: 8, price: 2.5 }, { sku: \"chocolates\", qty: 5, price: 10 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 3, cust_id: \"Busby Bee\", ord_date: new Date(\""+c("2020-03-08")+"\"), price: 50, items: [ { sku: \"oranges\", qty: 10, price: 2.5 }, { sku: \"pears\", qty: 10, price: 2.5 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 4, cust_id: \"Busby Bee\", ord_date: new Date(\""+c("2020-03-18")+"\"), price: 25, items: [ { sku: \"oranges\", qty: 10, price: 2.5 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 5, cust_id: \"Busby Bee\", ord_date: new Date(\""+c("2020-03-19")+"\"), price: 50, items: [ { sku: \"chocolates\", qty: 5, price: 10 } ], status: \"A\"}")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 6, cust_id: \"Cam Elot\", ord_date: new Date(\""+c("2020-03-19")+"\"), price: 35, items: [ { sku: \"carrots\", qty: 10, price: 1.0 }, { sku: \"apples\", qty: 10, price: 2.5 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 7, cust_id: \"Cam Elot\", ord_date: new Date(\""+c("2020-03-20")+"\"), price: 25, items: [ { sku: \"oranges\", qty: 10, price: 2.5 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 8, cust_id: \"Don Quis\", ord_date: new Date(\""+c("2020-03-20")+"\"), price: 75, items: [ { sku: \"chocolates\", qty: 5, price: 10 }, { sku: \"apples\", qty: 10, price: 2.5 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 9, cust_id: \"Don Quis\", ord_date: new Date(\""+c("2020-03-20")+"\"), price: 55, items: [ { sku: \"carrots\", qty: 5, price: 1.0 }, { sku: \"apples\", qty: 10, price: 2.5 }, { sku: \"oranges\", qty: 10, price: 2.5 } ], status: \"A\" }")));
        models.add(new InsertOneModel<Document>(Document.parse("{ _id: 10, cust_id: \"Don Quis\", ord_date: new Date(\""+c("2020-03-23")+"\"), price: 25, items: [ { sku: \"oranges\", qty: 10, price: 2.5 } ], status: \"A\" }")));
        collection.bulkWrite(models);
    }

    // convert yyyy-MM-dd to EEE MMM dd yyyy HH:mm:ss z
    private static final SimpleDateFormat INPUT_DFORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat OUTPUT_DFORMAT = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z", Locale.ENGLISH);
    private static String c(String s) {
        try { return OUTPUT_DFORMAT.format(INPUT_DFORMAT.parse(s)); } catch (ParseException e) { throw new RuntimeException(e); }
    }
    private static Date cd(String s) {
        try { return INPUT_DFORMAT.parse(s); } catch (ParseException e) { throw new RuntimeException(e); }
    }

    // prints all documents
    private static void print(Iterable<Document> documents) {
        for (Document document : documents) {
            System.out.print(document);
            System.out.print(" ");
        }
        System.out.println();
    }

}
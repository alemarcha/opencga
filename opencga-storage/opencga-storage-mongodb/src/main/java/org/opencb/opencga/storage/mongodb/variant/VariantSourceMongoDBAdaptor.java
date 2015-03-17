package org.opencb.opencga.storage.mongodb.variant;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import java.net.UnknownHostException;
import java.util.*;

import com.mongodb.WriteResult;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.stats.VariantGlobalStats;
import org.opencb.biodata.models.variant.stats.VariantSourceStats;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.datastore.mongodb.MongoDBCollection;
import org.opencb.datastore.mongodb.MongoDBConfiguration;
import org.opencb.datastore.mongodb.MongoDataStore;
import org.opencb.datastore.mongodb.MongoDataStoreManager;
import org.opencb.opencga.storage.core.StudyConfiguration;
import org.opencb.opencga.storage.mongodb.utils.MongoCredentials;
import org.opencb.opencga.storage.core.variant.adaptors.VariantSourceDBAdaptor;

/**
 *
 * @author Cristina Yenyxe Gonzalez Garcia <cyenyxe@ebi.ac.uk>
 */
public class VariantSourceMongoDBAdaptor implements VariantSourceDBAdaptor {

    private static final Map<String, List> samplesInSources = new HashMap<>();

    private final MongoDataStoreManager mongoManager;
    private final MongoDataStore db;
    private final DBObjectToVariantSourceConverter variantSourceConverter;
    private final String collectionName;

    private final Map<Integer, StudyConfiguration> studyConfigurationMap = new HashMap();
    private final DBObjectToStudyConfigurationConverter studyConfigurationConverter = new DBObjectToStudyConfigurationConverter();


    public VariantSourceMongoDBAdaptor(MongoCredentials credentials, String collectionName) throws UnknownHostException {
        // Mongo configuration
        mongoManager = new MongoDataStoreManager(credentials.getMongoHost(), credentials.getMongoPort());
        MongoDBConfiguration mongoDBConfiguration = MongoDBConfiguration.builder()
                .add("username", credentials.getUsername())
                .add("password", credentials.getPassword() != null ? new String(credentials.getPassword()) : null).build();
        db = mongoManager.get(credentials.getMongoDbName(), mongoDBConfiguration);
        this.collectionName = collectionName;
        variantSourceConverter = new DBObjectToVariantSourceConverter();
    }

    @Override
    public QueryResult<StudyConfiguration> getStudyConfiguration(int studyId, QueryOptions options) {
        long start = System.currentTimeMillis();
        StudyConfiguration studyConfiguration;
        if (!studyConfigurationMap.containsKey(studyId)) {
            MongoDBCollection coll = db.getCollection(collectionName);

            BasicDBObject query = new BasicDBObject("studyId", studyId);
            if (options.containsKey("fileId")) {
                query.put(DBObjectToStudyConfigurationConverter.FIELD_FILE_IDS, options.getInt("fileId"));
            }
            QueryResult<StudyConfiguration> queryResult = coll.find(query, null, studyConfigurationConverter, options);
            if (queryResult.getResult().isEmpty()) {
                studyConfiguration = null;
            } else {
                studyConfiguration = queryResult.first();
            }
            studyConfigurationMap.put(studyId, studyConfiguration);
        } else {
            studyConfiguration = studyConfigurationMap.get(studyId);
        }

        return new QueryResult<>("getStudyConfiguration", ((int) (System.currentTimeMillis() - start)), 1, 1, null, null, Collections.singletonList(studyConfiguration));
    }

    @Override
    public QueryResult countSources() {
        MongoDBCollection coll = db.getCollection(collectionName);
        return coll.count();
    }

    @Override
    public QueryResult<VariantSource> getAllSources(QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);
        QueryBuilder qb = QueryBuilder.start();
        parseQueryOptions(options, qb);

        return coll.find(qb.get(), null, variantSourceConverter, options);
    }

    @Override
    public QueryResult getAllSourcesByStudyId(String studyId, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);
        QueryBuilder qb = QueryBuilder.start();
        options.put("studyId", studyId);
        parseQueryOptions(options, qb);

        return coll.find(qb.get(), null, variantSourceConverter, options);
    }

    @Override
    public QueryResult getAllSourcesByStudyIds(List<String> studyIds, QueryOptions options) {
        MongoDBCollection coll = db.getCollection(collectionName);
        QueryBuilder qb = QueryBuilder.start();
//        getStudyIdFilter(studyIds, qb);
        options.put("studyId", studyIds);
        parseQueryOptions(options, qb);

        return coll.find(qb.get(), null, variantSourceConverter, options);
    }

    @Override
    public QueryResult getSamplesBySource(String fileId, QueryOptions options) {
        if (samplesInSources.size() != (long) countSources().getResult().get(0)) {
            synchronized (StudyMongoDBAdaptor.class) {
                if (samplesInSources.size() != (long) countSources().getResult().get(0)) {
                    QueryResult queryResult = populateSamplesInSources();
                    populateSamplesQueryResult(fileId, queryResult);
                    return queryResult;
                }
            }
        }

        QueryResult queryResult = new QueryResult();
        populateSamplesQueryResult(fileId, queryResult);
        return queryResult;
    }

    @Override
    public QueryResult getSamplesBySources(List<String> fileIds, QueryOptions options) {
        if (samplesInSources.size() != (long) countSources().getResult().get(0)) {
            synchronized (StudyMongoDBAdaptor.class) {
                if (samplesInSources.size() != (long) countSources().getResult().get(0)) {
                    QueryResult queryResult = populateSamplesInSources();
                    populateSamplesQueryResult(fileIds, queryResult);
                    return queryResult;
                }
            }
        }

        QueryResult queryResult = new QueryResult();
        populateSamplesQueryResult(fileIds, queryResult);
        return queryResult;
    }

    @Override
    public QueryResult getSourceDownloadUrlByName(String filename) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<QueryResult> getSourceDownloadUrlByName(List<String> filenames) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public QueryResult getSourceDownloadUrlById(String fileId, String studyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void parseQueryOptions(QueryOptions options, QueryBuilder builder) {

        if(options.containsKey("studyId")) {
            andIs(DBObjectToVariantSourceConverter.STUDYID_FIELD, options.get("studyId"), builder);
        }
        if(options.containsKey("studyName")) {
            andIs(DBObjectToVariantSourceConverter.STUDYNAME_FIELD, options.get("studyId"), builder);
        }
        if(options.containsKey("fileId")) {
            andIs(DBObjectToVariantSourceConverter.FILEID_FIELD, options.get("fileId"), builder);
        }
        if(options.containsKey("fileName")) {
            andIs(DBObjectToVariantSourceConverter.FILENAME_FIELD, options.get("fileName"), builder);
        }


    }

    private QueryBuilder andIs(String fieldName, Object object, QueryBuilder builder) {
        if(object == null) {
            return builder;
        } else if (object instanceof Collection) {
            return builder.and(fieldName).in(object);
        } else {
            return builder.and(fieldName).is(object);
        }
    }
//
//    private QueryBuilder getStudyIdFilter(String id, QueryBuilder builder) {
//        return builder.and(DBObjectToVariantSourceConverter.STUDYID_FIELD).is(id);
//    }
//
//    private QueryBuilder getStudyIdFilter(List<String> ids, QueryBuilder builder) {
//        return builder.and(DBObjectToVariantSourceConverter.STUDYID_FIELD).in(ids);
//    }

    /**
     * Populates the dictionary relating sources and samples.
     *
     * @return The QueryResult with information of how long the query took
     */
    private QueryResult populateSamplesInSources() {
        MongoDBCollection coll = db.getCollection(collectionName);
        DBObject projection = new BasicDBObject(DBObjectToVariantSourceConverter.FILEID_FIELD, true)
                .append(DBObjectToVariantSourceConverter.SAMPLES_FIELD, true);
        QueryResult queryResult = coll.find((DBObject)null, projection, null);

        List<DBObject> result = queryResult.getResult();
        for (DBObject dbo : result) {
            String key = dbo.get(DBObjectToVariantSourceConverter.FILEID_FIELD).toString();
            DBObject value = (DBObject) dbo.get(DBObjectToVariantSourceConverter.SAMPLES_FIELD);
            samplesInSources.put(key, new ArrayList(value.toMap().keySet()));
        }

        return queryResult;
    }

    private void populateSamplesQueryResult(String fileId, QueryResult queryResult) {
        List<List> samples = new ArrayList<>(1);
        List<String> samplesInSource = samplesInSources.get(fileId);

        if (samplesInSource == null || samplesInSource.isEmpty()) {
            queryResult.setWarningMsg("Source " + fileId + " not found");
            queryResult.setNumTotalResults(0);
        } else {
            samples.add(samplesInSource);
            queryResult.setResult(samples);
            queryResult.setNumTotalResults(1);
        }
    }

    private void populateSamplesQueryResult(List<String> fileIds, QueryResult queryResult) {
        List<List> samples = new ArrayList<>(fileIds.size());

        for (String fileId : fileIds) {
            List<String> samplesInSource = samplesInSources.get(fileId);

            if (samplesInSource == null || samplesInSource.isEmpty()) {
                // Samples not found
                samples.add(new ArrayList<>());
                if (queryResult.getWarningMsg() == null) {
                    queryResult.setWarningMsg("Source " + fileId + " not found");
                } else {
                    queryResult.setWarningMsg(queryResult.getWarningMsg().concat("\nSource " + fileId + " not found"));
                }
//                queryResult.setNumTotalResults(0);
            } else {
                // Add new list of samples
                samples.add(samplesInSource);
//                queryResult.setNumTotalResults(1);
            }
        }

        queryResult.setResult(samples);
        queryResult.setNumTotalResults(fileIds.size());
    }


    @Override
    public QueryResult updateSourceStats(VariantSourceStats variantSourceStats, QueryOptions queryOptions) {
        MongoDBCollection coll = db.getCollection(collectionName);

        VariantGlobalStats global = variantSourceStats.getFileStats();
        DBObject globalStats = new BasicDBObject(DBObjectToVariantSourceConverter.NUMSAMPLES_FIELD, global.getSamplesCount())
                .append(DBObjectToVariantSourceConverter.NUMVARIANTS_FIELD, global.getVariantsCount())
                .append(DBObjectToVariantSourceConverter.NUMSNPS_FIELD, global.getSnpsCount())
                .append(DBObjectToVariantSourceConverter.NUMINDELS_FIELD, global.getIndelsCount())
                .append(DBObjectToVariantSourceConverter.NUMPASSFILTERS_FIELD, global.getPassCount())
                .append(DBObjectToVariantSourceConverter.NUMTRANSITIONS_FIELD, global.getTransitionsCount())
                .append(DBObjectToVariantSourceConverter.NUMTRANSVERSIONS_FIELD, global.getTransversionsCount())
                .append(DBObjectToVariantSourceConverter.MEANQUALITY_FIELD, (double) global.getMeanQuality());

        DBObject find = new BasicDBObject(DBObjectToVariantSourceConverter.FILEID_FIELD, variantSourceStats.getFileId())
                .append(DBObjectToVariantSourceConverter.STUDYID_FIELD, variantSourceStats.getStudyId());
        DBObject update = new BasicDBObject("$set", new BasicDBObject(DBObjectToVariantSourceConverter.STATS_FIELD, globalStats));

        return coll.update(find, update, null);
    }


}

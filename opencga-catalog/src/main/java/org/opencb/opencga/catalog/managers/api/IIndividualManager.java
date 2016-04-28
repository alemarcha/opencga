package org.opencb.opencga.catalog.managers.api;

import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.models.AnnotationSet;
import org.opencb.opencga.catalog.models.Individual;

import java.util.Map;

/**
 * Created by hpccoll1 on 19/06/15.
 */
public interface IIndividualManager extends ResourceManager<Long, Individual> {

    QueryResult<Individual> create(long studyId, String name, String family, long fatherId, long motherId, Individual.Gender gender,
                                   QueryOptions options, String sessionId) throws CatalogException;

    QueryResult<Individual> readAll(long studyId, Query query, QueryOptions options, String sessionId) throws CatalogException;

    QueryResult<AnnotationSet> annotate(long individualId, String annotationSetId, long variableSetId, Map<String, Object> annotations,
                                        Map<String, Object> attributes, String sessionId) throws CatalogException;

    QueryResult<AnnotationSet> updateAnnotation(long individualId, String annotationSetId, Map<String, Object> newAnnotations,
                                                String sessionId) throws CatalogException;

    QueryResult<AnnotationSet> deleteAnnotation(long individualId, String annotationId, String sessionId) throws CatalogException;

}

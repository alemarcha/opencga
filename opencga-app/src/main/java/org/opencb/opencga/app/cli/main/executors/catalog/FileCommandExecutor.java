/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.app.cli.main.executors.catalog;


import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.*;
import org.opencb.opencga.app.cli.main.executors.OpencgaCommandExecutor;
import org.opencb.opencga.app.cli.main.executors.catalog.commons.AclCommandExecutor;
import org.opencb.opencga.app.cli.main.options.FileCommandOptions;
import org.opencb.opencga.catalog.db.api.FileDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.core.models.File;
import org.opencb.opencga.core.models.FileTree;
import org.opencb.opencga.core.models.acls.permissions.FileAclEntry;
import org.opencb.opencga.core.common.UriUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by imedina on 03/06/16.
 */
public class FileCommandExecutor extends OpencgaCommandExecutor {

    private FileCommandOptions filesCommandOptions;
    private AclCommandExecutor<File, FileAclEntry> aclCommandExecutor;

    public FileCommandExecutor(FileCommandOptions filesCommandOptions) {
        super(filesCommandOptions.commonCommandOptions);
        this.filesCommandOptions = filesCommandOptions;
        this.aclCommandExecutor = new AclCommandExecutor<>();
    }


    @Override
    public void execute() throws Exception {
        logger.debug("Executing files command line");

        String subCommandString = getParsedSubCommand(filesCommandOptions.jCommander);
        QueryResponse queryResponse = null;
        switch (subCommandString) {
//            case "copy":
//                queryResponse = copy();
//                break;
            case "create-folder":
                queryResponse = createFolder();
                break;
            case "info":
                queryResponse = info();
                break;
            case "download":
                queryResponse = download();
                break;
            case "grep":
                queryResponse = grep();
                break;
            case "search":
                queryResponse = search();
                break;
            case "list":
                queryResponse = list();
                break;
            case "tree":
                queryResponse = tree();
                break;
//            case "index":
//                queryResponse = index();
//                break;
            case "content":
                queryResponse = content();
                break;
//            case "fetch":
//                queryResponse = fetch();
//                break;
            case "update":
                queryResponse = update();
                break;
            case "upload":
                queryResponse = upload();
                break;
            case "delete":
                queryResponse = delete();
                break;
            case "link":
                queryResponse = link();
                break;
            case "relink":
                queryResponse = relink();
                break;
            case "unlink":
                queryResponse = unlink();
                break;
            case "refresh":
                queryResponse = refresh();
                break;
            case "group-by":
                queryResponse = groupBy();
                break;
//            case "variants":
//                queryResponse = variants();
//                break;
            case "acl":
                queryResponse = aclCommandExecutor.acls(filesCommandOptions.aclsCommandOptions, openCGAClient.getFileClient());
                break;
            case "acl-update":
                queryResponse = updateAcl();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }

        createOutput(queryResponse);
    }

    private QueryResponse createFolder() throws CatalogException, IOException {
        logger.debug("Creating a new folder");

        ObjectMap params = new ObjectMap();
        if (filesCommandOptions.createFolderCommandOptions.parents){
            params.put("parents",filesCommandOptions.createFolderCommandOptions.parents);
        }
        return openCGAClient.getFileClient().createFolder(resolveStudy(filesCommandOptions.createFolderCommandOptions.study),
                filesCommandOptions.createFolderCommandOptions.folder, params);
    }


    private QueryResponse<File> info() throws CatalogException, IOException {
        logger.debug("Getting file information");

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.STUDY.key(), resolveStudy(filesCommandOptions.infoCommandOptions.study));
        queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, filesCommandOptions.infoCommandOptions.dataModelOptions.include);
        queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, filesCommandOptions.infoCommandOptions.dataModelOptions.exclude);
        queryOptions.put("lazy", !filesCommandOptions.infoCommandOptions.noLazy);
        return openCGAClient.getFileClient().get(filesCommandOptions.infoCommandOptions.files, queryOptions);
    }

    private QueryResponse download() throws CatalogException, IOException {
        logger.debug("Downloading file");

        ObjectMap params = new ObjectMap();
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.STUDY.key(), resolveStudy(filesCommandOptions.downloadCommandOptions.study));
        return openCGAClient.getFileClient().download(filesCommandOptions.downloadCommandOptions.file, params);
    }

    private QueryResponse grep() throws CatalogException, IOException {
        logger.debug("Grep command: File content");

        ObjectMap params = new ObjectMap();
        params.put("ignoreCase", filesCommandOptions.grepCommandOptions.ignoreCase);
        params.put("multi", filesCommandOptions.grepCommandOptions.multi);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.STUDY.key(), resolveStudy(filesCommandOptions.grepCommandOptions.study));
        return openCGAClient.getFileClient().grep(filesCommandOptions.grepCommandOptions.file,
                filesCommandOptions.grepCommandOptions.pattern, params);
    }

    private QueryResponse search() throws CatalogException, IOException {
        logger.debug("Searching files");

        //FIXME check and put the correct format for type and bioformat. See StudiesCommandExecutor search param type. Is better
        Query query = new Query();
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.STUDY.key(), resolveStudy(filesCommandOptions.searchCommandOptions.study));
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.NAME.key(), filesCommandOptions.searchCommandOptions.name);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.PATH.key(), filesCommandOptions.searchCommandOptions.path);
        query.putIfNotNull(FileDBAdaptor.QueryParams.TYPE.key(), filesCommandOptions.searchCommandOptions.type);
        query.putIfNotNull(FileDBAdaptor.QueryParams.BIOFORMAT.key(), filesCommandOptions.searchCommandOptions.bioformat);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.FORMAT.key(), filesCommandOptions.searchCommandOptions.format);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.STATUS.key(), filesCommandOptions.searchCommandOptions.status);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.DIRECTORY.key(), filesCommandOptions.searchCommandOptions.folder);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.OWNER_ID.key(), filesCommandOptions.searchCommandOptions.ownerId);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.CREATION_DATE.key(), filesCommandOptions.searchCommandOptions.creationDate);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.MODIFICATION_DATE.key(), filesCommandOptions.groupByCommandOptions.modificationDate);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.DESCRIPTION.key(), filesCommandOptions.searchCommandOptions.description);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.SIZE.key(), filesCommandOptions.searchCommandOptions.size);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.SAMPLES.key(), filesCommandOptions.searchCommandOptions.samples);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.JOB_ID.key(), filesCommandOptions.searchCommandOptions.jobId);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.ATTRIBUTES.key(), filesCommandOptions.searchCommandOptions.attributes);
        query.putIfNotEmpty(FileDBAdaptor.QueryParams.NATTRIBUTES.key(), filesCommandOptions.searchCommandOptions.nattributes);
        query.putAll(filesCommandOptions.searchCommandOptions.commonOptions.params);

        if (filesCommandOptions.searchCommandOptions.numericOptions.count) {
            return openCGAClient.getFileClient().count(query);
        } else {
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, filesCommandOptions.searchCommandOptions.dataModelOptions.include);
            queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, filesCommandOptions.searchCommandOptions.dataModelOptions.exclude);
            queryOptions.put(QueryOptions.LIMIT, filesCommandOptions.searchCommandOptions.numericOptions.limit);
            queryOptions.put(QueryOptions.SKIP, filesCommandOptions.searchCommandOptions.numericOptions.skip);
            queryOptions.put("lazy", !filesCommandOptions.infoCommandOptions.noLazy);

            return openCGAClient.getFileClient().search(query, queryOptions);
        }
    }

    private QueryResponse<File> list() throws CatalogException, IOException {
        logger.debug("Listing files in folder");

        ObjectMap params = new ObjectMap();
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.STUDY.key(), resolveStudy(filesCommandOptions.listCommandOptions.study));
        params.putIfNotEmpty(QueryOptions.INCLUDE, filesCommandOptions.listCommandOptions.dataModelOptions.include);
        params.putIfNotEmpty(QueryOptions.EXCLUDE, filesCommandOptions.listCommandOptions.dataModelOptions.exclude);
        params.put(QueryOptions.LIMIT, filesCommandOptions.listCommandOptions.numericOptions.limit);
        params.put(QueryOptions.SKIP, filesCommandOptions.listCommandOptions.numericOptions.skip);
        params.put("count", filesCommandOptions.listCommandOptions.numericOptions.count);

        String folder = ".";
        if (StringUtils.isNotEmpty(filesCommandOptions.listCommandOptions.folderId)) {
            folder = filesCommandOptions.listCommandOptions.folderId;
        }
        return openCGAClient.getFileClient().list(folder, params);
    }

//    private QueryResponse<Job> index() throws CatalogException, IOException {
//        logger.debug("Indexing variant(s)");
//
//        String fileIds = filesCommandOptions.indexCommandOptions.file;
//
//        ObjectMap params = new ObjectMap();
////        o.putIfNotNull("studyId", filesCommandOptions.indexCommandOptions.study);
//        params.putIfNotNull("outDir", filesCommandOptions.indexCommandOptions.outdir);
//        params.putIfNotNull("transform", filesCommandOptions.indexCommandOptions.transform);
//        params.putIfNotNull("load", filesCommandOptions.indexCommandOptions.load);
//        params.putIfNotNull("includeExtraFields", filesCommandOptions.indexCommandOptions.extraFields);
//        params.putIfNotNull("aggregated", filesCommandOptions.indexCommandOptions.aggregated);
//        params.putIfNotNull("calculateStats", filesCommandOptions.indexCommandOptions.calculateStats);
//        params.putIfNotNull("annotate", filesCommandOptions.indexCommandOptions.annotate);
//        params.putIfNotNull("overwrite", filesCommandOptions.indexCommandOptions.overwriteAnnotations);
//        params.putIfNotNull(FileDBAdaptor.QueryParams.STUDY.key(), filesCommandOptions.indexCommandOptions.study);
//        params.putIfNotNull(VariantStorageEngine.Options.RESUME.key(), filesCommandOptions.indexCommandOptions.resume);
//        params.putAll(filesCommandOptions.commonCommandOptions.params);
//
//        return openCGAClient.getFileClient().index(fileIds, params);
//    }

    private QueryResponse<FileTree> tree() throws CatalogException, IOException {
        logger.debug("Obtain a tree view of the files and folders within a folder");

        ObjectMap params = new ObjectMap();
        params.putIfNotNull(FileDBAdaptor.QueryParams.STUDY.key(), resolveStudy(filesCommandOptions.treeCommandOptions.study));
        params.putIfNotNull("maxDepth", filesCommandOptions.treeCommandOptions.maxDepth);
        params.putIfNotEmpty(QueryOptions.INCLUDE, filesCommandOptions.treeCommandOptions.dataModelOptions.include);
        params.putIfNotEmpty(QueryOptions.EXCLUDE, filesCommandOptions.treeCommandOptions.dataModelOptions.exclude);
        params.putIfNotEmpty(QueryOptions.LIMIT, filesCommandOptions.treeCommandOptions.limit);
        return openCGAClient.getFileClient().tree(filesCommandOptions.treeCommandOptions.folderId, params);
    }

    private QueryResponse content() throws CatalogException, IOException {
        ObjectMap objectMap = new ObjectMap();
        objectMap.putIfNotNull(FileDBAdaptor.QueryParams.STUDY.key(), filesCommandOptions.contentCommandOptions.study);
        objectMap.put("start", filesCommandOptions.contentCommandOptions.start);
        objectMap.put(QueryOptions.LIMIT, filesCommandOptions.contentCommandOptions.limit);
        return openCGAClient.getFileClient().content(filesCommandOptions.contentCommandOptions.file, objectMap);
    }

    private QueryResponse fetch() throws CatalogException {
        logger.debug("File Fetch. [DEPRECATED]  Use .../files/{fileId}/[variants|alignments] or " +
                ".../studies/{studyId}/[variants|alignments] instead");
        return null;
    }

    private QueryResponse update() throws CatalogException, IOException {
        logger.debug("updating file");

        ObjectMap params = new ObjectMap();
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.FORMAT.key(), filesCommandOptions.updateCommandOptions.format);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.BIOFORMAT.key(), filesCommandOptions.updateCommandOptions.bioformat);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.DESCRIPTION.key(), filesCommandOptions.updateCommandOptions.description);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.ATTRIBUTES.key(), filesCommandOptions.updateCommandOptions.attributes);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.STATS.key(), filesCommandOptions.updateCommandOptions.stats);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.SAMPLES.key(), filesCommandOptions.updateCommandOptions.sampleIds);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.JOB_ID.key(), filesCommandOptions.updateCommandOptions.jobId);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.PATH.key(), filesCommandOptions.updateCommandOptions.path);
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.NAME.key(), filesCommandOptions.updateCommandOptions.name);
        return openCGAClient.getFileClient().update(filesCommandOptions.updateCommandOptions.file,
                resolveStudy(filesCommandOptions.updateCommandOptions.study), params);
    }

    private QueryResponse<File> upload() throws CatalogException, IOException {
        logger.debug("uploading file");

        ObjectMap params = new ObjectMap()
                .append("fileFormat", filesCommandOptions.uploadCommandOptions.fileFormat)
                .append("bioformat", filesCommandOptions.uploadCommandOptions.bioformat)
                .append("parents", filesCommandOptions.uploadCommandOptions.parents);

        if (filesCommandOptions.uploadCommandOptions.catalogPath != null) {
            params.append("relativeFilePath", filesCommandOptions.uploadCommandOptions.catalogPath);
        }

        if (filesCommandOptions.uploadCommandOptions.description != null) {
            params.append("description", filesCommandOptions.uploadCommandOptions.description);
        }

        if (filesCommandOptions.uploadCommandOptions.fileName != null) {
            params.append("fileName", filesCommandOptions.uploadCommandOptions.fileName);
        }

        return openCGAClient.getFileClient().upload(filesCommandOptions.uploadCommandOptions.study,
                filesCommandOptions.uploadCommandOptions.inputFile, params);
    }

    private QueryResponse<File> delete() throws CatalogException, IOException {
        logger.debug("Deleting file");

        ObjectMap objectMap = new ObjectMap()
                .append("deleteExternal", filesCommandOptions.deleteCommandOptions.deleteExternal)
                .append("skipTrash", filesCommandOptions.deleteCommandOptions.skipTrash);
        objectMap.putIfNotNull(FileDBAdaptor.QueryParams.STUDY.key(), filesCommandOptions.deleteCommandOptions.study);

        return openCGAClient.getFileClient().delete(filesCommandOptions.deleteCommandOptions.file, objectMap);
    }

    private QueryResponse<File> link() throws CatalogException, IOException, URISyntaxException {
        logger.debug("Linking the file or folder into catalog.");

        ObjectMap objectMap = new ObjectMap()
                .append(FileDBAdaptor.QueryParams.DESCRIPTION.key(), filesCommandOptions.linkCommandOptions.description)
                .append("parents", filesCommandOptions.linkCommandOptions.parents);

        CatalogManager catalogManager = null;
        try {
            catalogManager = new CatalogManager(configuration);
        } catch (CatalogException e) {
            logger.error("Catalog manager could not be initialized. Is the configuration OK?");
        }
        if (!catalogManager.existsCatalogDB()) {
            logger.error("The database could not be found. Are you running this from the server?");
            return null;
        }

        List<QueryResult<File>> linkQueryResultList = new ArrayList<>(filesCommandOptions.linkCommandOptions.inputs.size());

        for (String input : filesCommandOptions.linkCommandOptions.inputs) {
            URI uri = UriUtils.createUri(input);
            logger.debug("uri: {}", uri.toString());

            String userId1 = catalogManager.getUserManager().getUserId(sessionId);
            long studyId = catalogManager.getStudyManager().getId(userId1, filesCommandOptions.linkCommandOptions.study);
            linkQueryResultList.add(catalogManager.getFileManager().link(uri, filesCommandOptions.linkCommandOptions.path, studyId,
                    objectMap, sessionId));
        }

        return new QueryResponse<>(new QueryOptions(), linkQueryResultList);
    }

    private QueryResponse relink() throws CatalogException, IOException {
        logger.debug("Change file location. Provided file must be either STAGED or an external file. [DEPRECATED]");

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.STUDY.key(), filesCommandOptions.relinkCommandOptions.study);
        queryOptions.put("calculateChecksum", filesCommandOptions.relinkCommandOptions.calculateChecksum);
        return  openCGAClient.getFileClient().relink(filesCommandOptions.relinkCommandOptions.file,
                filesCommandOptions.relinkCommandOptions.uri, queryOptions);
    }

    private QueryResponse<File> unlink() throws CatalogException, IOException {
        logger.debug("Unlink an external file from catalog");

        // LOCAL EXECUTION
//        CatalogManager catalogManager = null;
//        try {
//            catalogManager = new CatalogManager(catalogConfiguration);
//        } catch (CatalogException e) {
//            logger.error("Catalog manager could not be initialized. Is the configuration OK?");
//        }
//        if (!catalogManager.existsCatalogDB()) {
//            logger.error("The database could not be found. Are you running this from the server?");
//            return;
//        }
//        QueryResult<File> unlinkQueryResult = catalogManager.unlink(filesCommandOptions.unlinkCommandOptions.id, new QueryOptions(),
//                sessionId);
//
//        QueryResponse<File> unlink = new QueryResponse<>(new QueryOptions(), Arrays.asList(unlinkQueryResult));
        ObjectMap params = new ObjectMap();
        params.putIfNotEmpty(FileDBAdaptor.QueryParams.STUDY.key(), filesCommandOptions.unlinkCommandOptions.study);
        return openCGAClient.getFileClient().unlink(filesCommandOptions.unlinkCommandOptions.file, params);
    }

    private QueryResponse refresh() throws CatalogException, IOException {
        logger.debug("Refreshing metadata from the selected file or folder. Print updated files.");

        ObjectMap params = new ObjectMap();
        params.putIfNotNull(FileDBAdaptor.QueryParams.STUDY.key(), filesCommandOptions.refreshCommandOptions.study);
        return openCGAClient.getFileClient().refresh(filesCommandOptions.refreshCommandOptions.file, params);
    }


    private QueryResponse groupBy() throws CatalogException, IOException {
        logger.debug("Grouping files by several fields");

        QueryOptions queryOptions = new QueryOptions();
//        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.ID.key(), filesCommandOptions.groupByCommandOptions.id);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.NAME.key(), filesCommandOptions.groupByCommandOptions.name);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.PATH.key(), filesCommandOptions.groupByCommandOptions.path);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.TYPE.key(), filesCommandOptions.groupByCommandOptions.type);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.BIOFORMAT.key(), filesCommandOptions.groupByCommandOptions.bioformat);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.FORMAT.key(), filesCommandOptions.groupByCommandOptions.format);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.STATUS.key(), filesCommandOptions.groupByCommandOptions.status);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.DIRECTORY.key(), filesCommandOptions.groupByCommandOptions.directory);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.OWNER_ID.key(), filesCommandOptions.groupByCommandOptions.ownerId);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.CREATION_DATE.key(), filesCommandOptions.groupByCommandOptions.creationDate);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.MODIFICATION_DATE.key(),
                filesCommandOptions.groupByCommandOptions.modificationDate);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.DESCRIPTION.key(), filesCommandOptions.groupByCommandOptions.description);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.SIZE.key(), filesCommandOptions.groupByCommandOptions.size);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.SAMPLES.key(), filesCommandOptions.groupByCommandOptions.sampleIds);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.JOB_ID.key(), filesCommandOptions.groupByCommandOptions.job);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.ATTRIBUTES.key(), filesCommandOptions.groupByCommandOptions.attributes);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.NATTRIBUTES.key(), filesCommandOptions.groupByCommandOptions.nattributes);
        return openCGAClient.getFileClient().groupBy(resolveStudy(filesCommandOptions.groupByCommandOptions.study),
                filesCommandOptions.groupByCommandOptions.fields, queryOptions);
    }

    private QueryResponse<FileAclEntry> updateAcl() throws IOException, CatalogException {
        FileCommandOptions.FileAclCommandOptions.AclsUpdateCommandOptions commandOptions = filesCommandOptions.aclsUpdateCommandOptions;

        ObjectMap queryParams = new ObjectMap();
        queryParams.putIfNotNull("study", commandOptions.study);

        ObjectMap bodyParams = new ObjectMap();
        bodyParams.putIfNotNull("permissions", commandOptions.permissions);
        bodyParams.putIfNotNull("action", commandOptions.action);
        bodyParams.putIfNotNull("file", commandOptions.id);
        bodyParams.putIfNotNull("sample", commandOptions.sample);

        return openCGAClient.getFileClient().updateAcl(commandOptions.memberId, queryParams, bodyParams);
    }

}

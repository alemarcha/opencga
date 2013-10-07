package org.opencb.opencga.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.StringUtils;
import org.opencb.opencga.account.beans.Job;
import org.opencb.opencga.lib.analysis.AnalysisJobExecuter;
import org.opencb.variant.lib.core.formats.VariantInfo;
import org.opencb.variant.lib.core.sqlite.WSSqliteManager;

@Path("/account/{accountId}/analysis/job/{jobId}")
public class JobAnalysisWSServer extends GenericWSServer {
	private String accountId;
	private String projectId;
	private String jobId;

	public JobAnalysisWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest,
			@DefaultValue("") @PathParam("accountId") String accountId,
			@DefaultValue("default") @QueryParam("projectId") String projectId,
			@DefaultValue("") @PathParam("jobId") String jobId) throws IOException {
		super(uriInfo, httpServletRequest);

		this.accountId = accountId;
		this.projectId = projectId;
		this.jobId = jobId;
	}

	@GET
	@Path("/result.{format}")
	public Response getResultFile(@PathParam("format") String format) {
		try {
			String res = cloudSessionManager.getJobResult(accountId, jobId, sessionId);
			return createOkResponse(res);
		} catch (Exception e) {
			logger.error(e.toString());
			return createErrorResponse(e.getMessage());
		}
	}

	@GET
	@Path("/table")
	public Response table(@DefaultValue("") @QueryParam("filename") String filename,
			@DefaultValue("") @QueryParam("start") String start, @DefaultValue("") @QueryParam("limit") String limit,
			@DefaultValue("") @QueryParam("colNames") String colNames,
			@DefaultValue("") @QueryParam("colVisibility") String colVisibility,
			@DefaultValue("") @QueryParam("callback") String callback,
			@QueryParam("sort") @DefaultValue("false") String sort) {

		try {
			String res = cloudSessionManager.getFileTableFromJob(accountId, jobId, filename, start, limit, colNames,
					colVisibility, callback, sort, sessionId);
			return createOkResponse(res, MediaType.valueOf("text/javascript"));
		} catch (Exception e) {
			logger.error(e.toString());
			return createErrorResponse(e.getMessage());
		}
	}

	@GET
	@Path("/poll")
	public Response pollJobFile(@DefaultValue("") @QueryParam("filename") String filename,
			@DefaultValue("true") @QueryParam("zip") String zip) {

		try {
			DataInputStream is = cloudSessionManager.getFileFromJob(accountId, jobId, filename, zip, sessionId);
			String name = null;
			if (zip.compareTo("true") != 0) {// PAKO zip != true
				name = filename;
			} else {
				name = filename + ".zip";
			}
			return createOkResponse(is, MediaType.APPLICATION_OCTET_STREAM_TYPE, name);
		} catch (Exception e) {
			logger.error(e.toString());
			return createErrorResponse(e.getMessage());
		}
	}

	@GET
	@Path("/status")
	public Response getJobStatus() {
		try {
			String res = cloudSessionManager.checkJobStatus(accountId, jobId, sessionId);
			return createOkResponse(res);
		} catch (Exception e) {
			logger.error(e.toString());
			return createErrorResponse(e.getMessage());
		}
	}

	@GET
	@Path("/delete")
	public Response deleteJob() {
		try {
			cloudSessionManager.deleteJob(accountId, projectId, jobId, sessionId);
			return createOkResponse("OK");
		} catch (Exception e) {
			logger.error(e.toString());
			return createErrorResponse(e.getMessage());
		}
	}

	@GET
	@Path("/download")
	public Response downloadJob() {
		try {
			InputStream is = cloudSessionManager.getJobZipped(accountId, jobId, sessionId);
			return createOkResponse(is, MediaType.valueOf("application/zip"), jobId + ".zip");
		} catch (Exception e) {
			logger.error(e.toString());
			return createErrorResponse(e.getMessage());
		}
	}

	@GET
	@Path("/result.js")
	public Response getResult() {
		try {

			// AnalysisJobExecuter aje = new AnalysisJobExecuter(analysis);
			// cloudSessionManager.get
			Job job = cloudSessionManager.getJob(accountId, jobId, sessionId);
			AnalysisJobExecuter aje = new AnalysisJobExecuter(job.getToolName());
			InputStream is = aje.getResultInputStream();
			cloudSessionManager.incJobVisites(accountId, jobId, sessionId);
			return createOkResponse(is, MediaType.valueOf("text/javascript"), "result.js");

			// String resultToUse = aje.getResult();
			// String jobObj = cloudSessionManager.getJobObject(accountId,
			// jobId);
			// StringBuilder sb = new StringBuilder();
			// String c = "\"";
			// sb.append("{");
			// sb.append(c + "result" + c + ":"+ c + resultJson + c + ",");
			// sb.append(c + "resultToUse" + c + ":"+ c + resultToUse + c +
			// ",");
			// sb.append(c + "job" + c + ":" + c + jobObj + c);
			// sb.append("}");
			// return createOkResponse(sb.toString());
		} catch (Exception e) {
			logger.error(e.toString());
			return createErrorResponse("can not get result json.");
		}
	}

    //VARIANT EXPLORER WS
    @POST
    @Path("/variants")
    // @Consumes(MediaType.MULTIPART_FORM_DATA)
    // @Produces(MediaType.APPLICATION_JSON)
    public Response getVariantInfo(@DefaultValue("") @QueryParam("filename") String filename, MultivaluedMap<String, String> postParams) {


        HashMap<String, String> map = new LinkedHashMap<>();


        for (Map.Entry<String, List<String>> entry : postParams.entrySet()) {
            map.put(entry.getKey(), StringUtils.join(entry.getValue(), ","));
        }

        System.out.println(map);


        java.nio.file.Path dataPath = cloudSessionManager.getJobFolderPath(accountId, projectId, org.opencb.opencga.common.StringUtils.parseObjectId(filename));

        System.out.println("dataPath = " + dataPath.toString());

        map.put("db_name", dataPath.toString());
        List<VariantInfo> list = WSSqliteManager.getRecords(map);


        String res = null;
        try {
            res = jsonObjectMapper.writeValueAsString(list);

        } catch (JsonProcessingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return createOkResponse(res);
    }



}

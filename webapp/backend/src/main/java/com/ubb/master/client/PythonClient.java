package com.ubb.master.client;

import com.ubb.master.generated.api.model.BenchmarkHistory;
import com.ubb.master.generated.api.model.BenchmarkRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/api/benchmark")
@RegisterRestClient(configKey = "python-api")
@ApplicationScoped
public interface PythonClient {

    @POST
    @Path("/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    com.ubb.master.model.BenchmarkHistory startBenchmark(BenchmarkRequest request);

    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    List<BenchmarkHistory> getBenchmarkHistory();

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    String getBenchmarkStatus();
}
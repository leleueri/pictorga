package org.github.leleueri.resources;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.github.leleueri.services.OrganizerService;
import org.github.leleueri.services.OrganizerState;
import org.github.leleueri.services.ProcessState;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path("/organizer")
public class OrganizerResource {

    @Inject
    private OrganizerService orgService;

    @Inject
    private Vertx vertx;
    
    @POST
    @Path("/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response start() {
        switch (orgService.startOrganize()) {
            case STARTED:
                return Response.accepted().build();
            case ONGOING:
                return Response.status(Response.Status.CONFLICT).build();
            default:
                return Response.noContent().build();
        }
    }
    
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/state")
    public Multi<ProcessState> state() {
        return vertx.periodicStream(5000).toMulti().map(l -> orgService.getProcessingState());
    }
}
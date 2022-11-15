package org.acme;

import java.sql.Connection;
import java.sql.Statement;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {
    @Inject
    DataSource dataSource;
    @Inject
    TransactionManager transactionManager;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String hello(String message) throws Exception {
        DummyXAResource xaResource = new DummyXAResource("crash".equals(message));
        transactionManager.getTransaction().enlistResource(xaResource);

        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.execute(
                        "insert into audit_log (message) values ('" + message + "')");
            }
        }

        if ("rollback".equals(message)) {
            transactionManager.setRollbackOnly();
            return "rollback message";
        } else {
            return "insert " + message + " ok";
        }
    }
}
package com.airhacks.breakr;

import static com.airhacks.rulz.jaxrsclient.HttpMatchers.successful;
import com.airhacks.rulz.jaxrsclient.JAXRSClientProvider;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author airhacks.com
 */
public class BreakrIT {

    @Rule
    public JAXRSClientProvider provider = JAXRSClientProvider.buildWithURI("http://localhost:8080/breakr-st/resources/tests");

    @Test
    public void timeoutWithReset() {
        WebTarget target = provider.target();
        Response response = target.
                path("slow").
                path("5").
                request().
                get(Response.class);
        assertThat(response, successful());
        String result = response.readEntity(String.class);
        assertTrue(result.isEmpty());

        //reset of unrelated service shouldn't have any effect
        target.path("brittle").
                request().
                delete();

        result = target.
                path("slow").
                path("1").
                request().
                get(String.class);
        assertTrue(result.isEmpty());

        //reset of the related service should close the circuit
        target.path("slow").
                request().
                delete();

        result = target.
                path("slow").
                path("2").
                request().
                get(String.class);
        assertThat(result, startsWith("Slow"));
    }

    @Test
    public void brittle() {
        WebTarget target = provider.target();
        Response response = target.
                path("brittle").
                path("5").
                request().
                get(Response.class);
        assertThat(response, successful());
        String result = response.readEntity(String.class);
        assertTrue(result.isEmpty());

        //reset unrelated circuit
        target.path("slow").
                request().
                delete();

        result = target.
                path("brittle").
                path("1").
                request().
                get(String.class);

        assertTrue(result.isEmpty());

        //reset circuit
        target.path("brittle").
                request().
                delete();

        result = target.
                path("brittle").
                path("1").
                request().
                get(String.class);

        assertThat(result, is("-"));

    }

}

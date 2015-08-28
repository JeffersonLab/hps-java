package org.hps.runweb;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.hps.rundb.RunManager;
import org.hps.rundb.RunSummary;

/**
 * Setup state for JSP that shows run summary.
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
public class RunSummaryServlet extends HttpServlet {

    /**
     * JSP target page.
     */
    private static final String JSP_TARGET = "/runSummary.jsp";

    /**
     * Attribute in the request which will have the run summary object.
     */
    private static final String RUN_SUMMARY_ATTRIBUTE = "RunSummary";

    /**
     * The data source with the database connection.
     */
    private final DataSource dataSource;

    /**
     * Create a new runs servlet.
     * <p>
     * This will initialize the data source with the db connection.
     */
    public RunSummaryServlet() {
        this.dataSource = DatabaseUtilities.getDataSource();
    }

    /**
     * Setup servlet state by loading the run summaries and then forward to the JSP page for display.
     */
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {
        if (!request.getParameterMap().containsKey("run")) {
            throw new RuntimeException("Missing required run parameter.");
        }
        final Integer run = Integer.parseInt(request.getParameterValues("run")[0]);
        final RunSummary runSummary = this.getRunSummary(run);
        request.setAttribute(RUN_SUMMARY_ATTRIBUTE, runSummary);
        final RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(JSP_TARGET);
        dispatcher.forward(request, response);
    }

    /**
     * Get a run summary for the given run number.
     *
     * @param run the run number
     * @return the run summary
     */
    private RunSummary getRunSummary(final Integer run) {
        final RunManager runManager = new RunManager();
        RunSummary runSummary = null;
        try (Connection connection = this.dataSource.getConnection()) {
            runManager.setConnection(connection);
            runManager.setRun(run);
            runSummary = runManager.getRunSummary();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return runSummary;
    }
}

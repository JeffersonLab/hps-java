package org.hps.runweb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.hps.rundb.RunDatabaseDaoFactory;
import org.hps.rundb.RunSummary;
import org.hps.rundb.RunSummaryDao;

/**
 * Loads the list of {@link org.hps.rundb.RunSummary} objects to setup state for the <code>runTable.jsp</code> page.
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
public final class RunsServlet extends HttpServlet {

    /**
     * The JSP page to which the servlet will forward the request.
     */
    private static final String JSP_TARGET = "/runTable.jsp";

    /**
     * Attribute for list of run summaries that will set on the request object.
     */
    private static final String RUN_SUMMARIES_ATTRIBUTE = "RunSummaries";

    /**
     * The data source with the database connection.
     */
    private final DataSource dataSource;

    /**
     * Create a new runs servlet.
     * <p>
     * This will initialize the data source with the db connection.
     */
    public RunsServlet() {
        this.dataSource = DatabaseUtilities.getDataSource();
    }

    /**
     * Setup servlet state by loading the run summaries and then forward to the JSP page for display.
     */
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {

        final List<RunSummary> runSummaries = this.getRunSummaries();
        request.setAttribute(RUN_SUMMARIES_ATTRIBUTE, runSummaries);
        final RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(JSP_TARGET);
        dispatcher.forward(request, response);
    }

    /**
     * Read the full list of run summaries from the db.
     *
     * @return the list of run summaries
     */
    private List<RunSummary> getRunSummaries() {
        List<RunSummary> runSummaries = new ArrayList<RunSummary>();
        Connection connection = null;
        try {
            connection = this.dataSource.getConnection();
            final RunSummaryDao runSummaryDao = new RunDatabaseDaoFactory(connection).createRunSummaryDao();

            // This does a shallow read of all run summaries but does not load their complex state.
            runSummaries = runSummaryDao.getRunSummaries();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return runSummaries;
    }
}

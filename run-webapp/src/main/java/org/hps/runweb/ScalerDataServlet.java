package org.hps.runweb;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.hps.record.scalers.ScalerData;
import org.hps.run.database.RunDatabaseDaoFactory;
import org.hps.run.database.ScalerDataDao;

/**
 * Setup session state for JSP that shows a run's scaler data.
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
public class ScalerDataServlet extends HttpServlet {

    /**
     * JSP target page.
     */
    private static final String JSP_TARGET = "/runScalers.jsp";

    /**
     * Attribute in the request which will have the run summary object.
     */
    private static final String SCALAR_DATA_ATTRIBUTE = "ScalerDataList";

    /**
     * The data source with the database connection.
     */
    private final DataSource dataSource;

    /**
     * Create a new runs servlet.
     * <p>
     * This will initialize the data source with the db connection.
     */
    public ScalerDataServlet() {
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
        List<ScalerData> scalerDataList = null;
        try (Connection connection = this.dataSource.getConnection()) {
            final ScalerDataDao scalarDataDao = new RunDatabaseDaoFactory(connection).createScalerDataDao();
            scalerDataList = scalarDataDao.getScalerData(run);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        request.setAttribute(SCALAR_DATA_ATTRIBUTE, scalerDataList);
        final RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(JSP_TARGET);
        dispatcher.forward(request, response);
    }

}

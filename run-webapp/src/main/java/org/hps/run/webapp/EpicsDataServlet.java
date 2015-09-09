package org.hps.run.webapp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.hps.record.epics.EpicsData;
import org.hps.run.database.EpicsDataDao;
import org.hps.run.database.EpicsType;
import org.hps.run.database.EpicsVariable;
import org.hps.run.database.EpicsVariableDao;
import org.hps.run.database.RunDatabaseDaoFactory;
import org.hps.run.database.RunManager;

/**
 * Setup session state for JSP that shows a run's EPICS data.
 *
 * @author Jeremy McCormick, SLAC
 */
@SuppressWarnings("serial")
public class EpicsDataServlet extends HttpServlet {

    private final DataSource dataSource;

    public EpicsDataServlet() {
        this.dataSource = DatabaseUtilities.getDataSource();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
    IOException {
        if (!request.getParameterMap().containsKey("run")) {
            throw new RuntimeException("Missing required run parameter.");
        }
        final Integer run = Integer.parseInt(request.getParameterValues("run")[0]);
        EpicsDataDao epicsDataDao = null;
        Connection connection = null;
        List<EpicsData> epicsDataList = null;

        EpicsType epicsType = EpicsType.EPICS_1S;
        if (request.getParameterMap().containsKey("epicsBankType")) {
            epicsType = EpicsType.valueOf(request.getParameter("epicsBankType"));
        }

        List<EpicsVariable> epicsVariables = null;

        try {
            connection = dataSource.getConnection();

            final RunDatabaseDaoFactory dbFactory = new RunManager(connection).createDaoFactory();

            epicsDataDao = dbFactory.createEpicsDataDao();
            epicsDataList = epicsDataDao.getEpicsData(epicsType, run);

            final EpicsVariableDao epicsVariableDao = dbFactory.createEpicsVariableDao();
            epicsVariables = epicsVariableDao.getEpicsVariables(epicsType);

        } catch (final SQLException e) {
            throw new IllegalStateException("Failed to setup data source connection.", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        request.setAttribute("EpicsDataList", epicsDataList);
        request.setAttribute("EpicsType", epicsType);
        request.setAttribute("EpicsVariables", epicsVariables);
        final RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/runEpics.jsp");
        dispatcher.forward(request, response);
    }
}

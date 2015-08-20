package org.hps.run.web;

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
import org.hps.record.run.EpicsDataDao;
import org.hps.record.run.EpicsDataDaoImpl;

/**
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

        String epicsBankType = "2s";
        if (request.getParameterMap().containsKey("epicsBankType")) {
            epicsBankType = request.getParameter("epicsBankType");
            if (!epicsBankType.equals("2s") && !epicsBankType.equals("20s")) {
                throw new IllegalArgumentException("bad epics bank type: " + epicsBankType);
            }
        }

        // List<String> variableNames = null;
        try {
            connection = dataSource.getConnection();
            epicsDataDao = new EpicsDataDaoImpl(connection);
            epicsDataList = epicsDataDao.getEpicsData(run);
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
        request.setAttribute("EpicsBankType", epicsBankType);
        final RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/runEpics.jsp");
        dispatcher.forward(request, response);
    }
}

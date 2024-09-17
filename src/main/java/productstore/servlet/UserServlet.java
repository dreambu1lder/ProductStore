package productstore.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import productstore.dao.impl.UserDaoImpl;
import productstore.service.UserService;
import productstore.service.apierror.ApiErrorResponse;
import productstore.service.apierror.UserNotFoundException;
import productstore.service.impl.UserServiceImpl;
import productstore.servlet.dto.input.UserInputDTO;
import productstore.servlet.dto.output.UserOutputDTO;
import productstore.servlet.util.PaginationUtils;

import java.io.IOException;
import java.util.List;


@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {

    private final UserService userService = new UserServiceImpl(new UserDaoImpl());
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {

                int pageNumber = PaginationUtils.getPageNumber(req);
                int pageSize = PaginationUtils.getPageSize(req);

                List<UserOutputDTO> users = userService.getUsersWithPagination(pageNumber, pageSize);
                writeResponse(resp, HttpServletResponse.SC_OK, users);
            } else {
                long id = parseId(pathInfo.substring(1));
                UserOutputDTO user = userService.getUserById(id);
                writeResponse(resp, HttpServletResponse.SC_OK, user);
            }
        } catch (UserNotFoundException e) {
            handleException(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (NumberFormatException e) {
            handleException(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID format");
        } catch (Exception e) {
            handleException(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (!req.getReader().ready()) {
                handleException(resp, HttpServletResponse.SC_BAD_REQUEST, "Request body is required");
                return;
            }

            UserInputDTO userInputDTO = gson.fromJson(req.getReader(), UserInputDTO.class);
            UserOutputDTO createdUser = userService.createUser(userInputDTO);
            writeResponse(resp, HttpServletResponse.SC_CREATED, createdUser);
        } catch (JsonSyntaxException e) {
            handleException(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            handleException(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (!req.getReader().ready()) {
                handleException(resp, HttpServletResponse.SC_BAD_REQUEST, "Request body is required");
                return;
            }

            UserInputDTO userInputDTO = gson.fromJson(req.getReader(), UserInputDTO.class);
            userService.updateUser(userInputDTO);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (UserNotFoundException e) {
            handleException(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (JsonSyntaxException e) {
            handleException(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            handleException(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleException(resp, HttpServletResponse.SC_BAD_REQUEST, "User ID is required");
                return;
            }

            long id = parseId(pathInfo.substring(1));
            userService.deleteUser(id);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (UserNotFoundException e) {
            handleException(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (NumberFormatException e) {
            handleException(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID format");
        } catch (Exception e) {
            handleException(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid user ID format");
        }
    }

    private void handleException(HttpServletResponse resp, int statusCode, String message) throws IOException {
        writeResponse(resp, statusCode, new ApiErrorResponse(message, statusCode));
    }

    private void writeResponse(HttpServletResponse resp, int statusCode, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(statusCode);
        resp.getWriter().write(gson.toJson(data));
    }
}

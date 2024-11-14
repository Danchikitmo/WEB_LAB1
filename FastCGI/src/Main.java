import com.fastcgi.FCGIInterface;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {

        var fcgiInterface = new FCGIInterface();

        while (fcgiInterface.FCGIaccept() >= 0) {

            final String HTTP_RESPONSE = """
        HTTP/1.1 200 OK
        Content-Type: application/json
        Content-Length: %d
        
        %s
        """;
            final String HTTP_ERROR = """
        HTTP/1.1 400 Bad Request
        Content-Type: application/json
        Content-Length: %d
        
        %s
        """;
            final String RESULT_JSON = """
        {
            "hit": "%s",
            "exec_time": "%s"
        }
        """;
            final String ERROR_JSON = """
        {
            "reason": "%s"
        }
        """;
            final String HTTP_SERVER_ERROR = """
        HTTP/1.1 500 Internal Server Error
        Content-Type: application/json
        Content-Length: %d
        
        %s
        """;

            var query = System.getProperties().getProperty("QUERY_STRING");
//            FCGIInterface.request.params.getProperty("QUERY_STRING");

            var request = readRequestBody(query);

            try {
                validateParams(request);
            } catch (ValidationException e) {
                String errorResponse = String.format(ERROR_JSON, e.getMessage());
                var response = String.format(HTTP_ERROR, errorResponse.getBytes(StandardCharsets.UTF_8).length, errorResponse);
                System.out.println(response);
                continue;
            }

            long startTime = System.nanoTime();

            String rField = request.get("R_field");
            rField = rField.replace(',', '.');

            float rValue;
            try {
                rValue = Float.parseFloat(rField);
            } catch (NumberFormatException e) {
                String errorResponse = String.format(ERROR_JSON, "R_field is invalid or contains an invalid format");
                var response = String.format(HTTP_ERROR, errorResponse.getBytes(StandardCharsets.UTF_8).length, errorResponse);
                System.out.println(response);
                continue;
            }

            String result;
            try {
                result = Script.defenitionHit(
                        Integer.parseInt(request.get("x_field")),
                        Integer.parseInt(request.get("y_field")),
                        rValue
                );
            } catch (NumberFormatException e) {
                String errorResponse = String.format(ERROR_JSON, "Invalid number format");
                var response = String.format(HTTP_ERROR, errorResponse.getBytes(StandardCharsets.UTF_8).length, errorResponse);
                System.out.println(response);
                continue;
            }

            long endTime = System.nanoTime();
            String executionTime = String.valueOf(endTime - startTime);

            var json = String.format(RESULT_JSON, result, executionTime);
            var response = String.format(HTTP_RESPONSE, json.getBytes(StandardCharsets.UTF_8).length + 2, json);
            System.out.println(response);
        }
    }

    private static Map<String, String> readRequestBody(String request) {
        var queryPairs = new HashMap<String, String>();
        var pairs = request.split("&");
        for (var pair : pairs) {
            var idx = pair.indexOf("=");
            queryPairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return queryPairs;
    }

    // Валидация параметров запроса
    private static void validateParams(Map<String, String> params) throws ValidationException {

        var x = params.get("x_field");
        if (x == null || x.isEmpty()) {
            throw new ValidationException("x_field is empty");
        }

        x = x.replace(',', '.');

        try {
            var xx = Integer.parseInt(x);
            if (xx < -3 || xx > 5) {
                throw new ValidationException("x_field has forbidden value");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("x_field is not a number");
        }

        var y = params.get("y_field");
        if (y == null || y.isEmpty()) {
            throw new ValidationException("y_field is empty");
        }

        y = y.replace(',', '.');

        try {
            var yy = Float.parseFloat(y);
            if (yy < -3 || yy > 5) {
                throw new ValidationException("y_field has forbidden value");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("y_field is not a number");
        }

        // Проверка и обработка для R_field
        var r = params.get("R_field");
        if (r == null || r.isEmpty()) {
            throw new ValidationException("R_field is empty");
        }

        r = r.replace(',', '.');

        try {
            var rr = Float.parseFloat(r);
            if (!(rr == 1 || rr == 1.5 || rr == 2 || rr == 2.5 || rr == 3)) {
                throw new ValidationException("R_field has forbidden value");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("R_field is not a number");
        }
    }
}

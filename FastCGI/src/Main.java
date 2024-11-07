import com.fastcgi.FCGIInterface;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException, ValidationException {

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

            var request = readRequestBody(query);
            validateParams(request);
            long startTime = System.nanoTime();

            String rField = request.get("R_field");
            if (rField == null || rField.isEmpty()) {
                // Обработка ошибки, если R_field отсутствует или пуст
                String errorResponse = String.format(ERROR_JSON, "R_field is missing or empty");
                var response = String.format(HTTP_ERROR, errorResponse.getBytes(StandardCharsets.UTF_8).length, errorResponse);
                System.out.println(response);
                continue; // Переход к следующему запросу
            }

            rField = rField.replace(',', '.');

            float rValue;
            try {
                rValue = Float.parseFloat(rField);
                // Далее используйте rValue
            } catch (NumberFormatException e) {
                // Обработка ошибки
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
                // Обработка ошибки, если парсинг не удался
                String errorResponse = String.format(ERROR_JSON, "Invalid number format");
                var response = String.format(HTTP_ERROR, errorResponse.getBytes(StandardCharsets.UTF_8).length, errorResponse);
                System.out.println(response);
                continue; // Переход к следующему запросу
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

    private static void validateParams(Map<String, String> params) throws ValidationException {
        var x = params.get("x_field");
        if (x == null || x.isEmpty()) {
            throw new ValidationException("x is invalid");
        }

        try {
            var xx = Integer.parseInt(x);
            if (xx < -3 || xx > 5) {
                throw new ValidationException("x has forbidden value");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("x is not a number");
        }

        var y = params.get("y_field");
        if (y == null || y.isEmpty()) {
            throw new ValidationException("y is invalid");
        }

        try {
            var yy = Float.parseFloat(y);
            if (yy < -3 || yy > 5) {
                throw new ValidationException("y has forbidden value");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("y is not a number");
        }

        var r = params.get("R_field");
        if (r == null || r.isEmpty()) {
            throw new ValidationException("R_field is invalid");
        }

        if (r.contains(",")) {
            throw new ValidationException("R_field cannot contain a comma.");
        }

        try {
            var rr = Float.parseFloat(r);
            if (rr < 1 || rr > 3) {
                throw new ValidationException("R_field has forbidden value");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("R_field is not a number");
        }
    }
}

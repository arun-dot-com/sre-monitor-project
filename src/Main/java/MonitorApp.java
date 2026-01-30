import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong; // Necessary for thread-safe counting
import java.util.Map;

public class MonitorApp {
    private static final Map<String, Integer> monitorTargets = new ConcurrentHashMap<>();
    // Global counter for total pings performed
    private static final AtomicLong totalChecks = new AtomicLong(0);

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new UIHandler());
        server.createContext("/metrics", new MetricsHandler());
        server.createContext("/add", new AddTargetHandler());

        System.out.println(">>> Monitoring App is LIVE on http://localhost:8080");
        System.out.println(">>> Use the web UI to add links, then check /metrics for Prometheus data.");
        
        server.setExecutor(null); 
        server.start();
    }

    /*static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder response = new StringBuilder();
            response.append("<html><body style='font-family: Arial, sans-serif; margin: 40px;'>")
                    .append("<h2>Service Health Monitor</h2>")
                    .append("<p>Enter a URL to monitor its uptime.</p>")
                    .append("<form action='/add' method='get'>")
                    .append("<input type='text' name='url' placeholder='https://google.com' style='padding: 5px; width: 300px;'> ")
                    .append("<input type='submit' value='Add Service' style='padding: 5px 15px;'>")
                    .append("</form>")
                    .append("<h3>Currently Tracking:</h3><ul>");
            
            if (monitorTargets.isEmpty()) {
                response.append("<li>No URLs added yet.</li>");
            } else {
                for (String url : monitorTargets.keySet()) {
                    response.append("<li>").append(url).append("</li>");
                }
            }
            
            response.append("</ul><hr><p><a href='/metrics'>View Prometheus Metrics Raw Data</a></p>")
                    .append("</body></html>");
            
            sendResponse(exchange, response.toString(), "text/html");
        }
    }*/
   static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><title>DevOps Health Monitor</title>")
                .append("<style>")
                .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #1a1a2e; color: #e94560; margin: 0; display: flex; justify-content: center; align-items: center; min-height: 100vh; }")
                .append(".container { background: #16213e; padding: 2rem; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); width: 90%; max-width: 600px; }")
                .append("h2 { color: #fff; background: #e94560; padding: 15px; border-radius: 8px; text-align: center; margin-top: 0; letter-spacing: 2px; }")
                .append("form { display: flex; gap: 10px; margin-bottom: 25px; }")
                .append("input[type='text'] { flex-grow: 1; padding: 12px; border-radius: 5px; border: 1px solid #0f3460; background: #0f3460; color: white; outline: none; }")
                .append("input[type='submit'] { background: #e94560; color: white; border: none; padding: 12px 25px; border-radius: 5px; cursor: pointer; font-weight: bold; transition: 0.3s; }")
                .append("input[type='submit']:hover { background: #c62a48; transform: translateY(-1px); }")
                .append("ul { list-style: none; padding: 0; }")
                // Added overflow-wrap and better alignment for the list items
                .append("li { background: #1a1a2e; margin-bottom: 12px; padding: 15px; border-radius: 8px; color: #4ecca3; display: flex; justify-content: space-between; align-items: center; border-left: 5px solid #e94560; transition: 0.2s; box-shadow: 2px 2px 10px rgba(0,0,0,0.2); }")
                .append("li:hover { background: #1f1f3d; }")
                // This class handles the long URL text
                .append(".url-text { overflow-wrap: break-word; word-break: break-all; max-width: 75%; font-size: 0.95rem; line-height: 1.4; }")
                .append(".status-tag { background: #0f3460; color: #e94560; font-size: 0.7rem; padding: 4px 8px; border-radius: 4px; font-weight: bold; white-space: nowrap; margin-left: 10px; border: 1px solid #e94560; }")
                .append("a { color: #add8e6; text-decoration: none; font-size: 0.85rem; display: block; text-align: center; margin-top: 25px; opacity: 0.7; transition: 0.3s; }")
                .append("a:hover { opacity: 1; color: #e94560; }")
                .append("</style></head><body>")
                .append("<div class='container'>")
                .append("<h2>SRE COMMAND CENTER</h2>")
                .append("<form action='/add' method='get'>")
                .append("<input type='text' name='url' placeholder='Enter target URL (e.g. google.com)'>")
                .append("<input type='submit' value='ADD'>")
                .append("</form>")
                .append("<strong style='color: #fff; font-size: 0.9rem; text-transform: uppercase;'>Monitoring Assets</strong><ul>");

            if (monitorTargets.isEmpty()) {
                html.append("<li style='border-left-color: #555; color: #888;'>No active probes deployed.</li>");
            } else {
                for (String url : monitorTargets.keySet()) {
                    html.append("<li>")
                        .append("<span class='url-text'>").append(url).append("</span>")
                        .append("<span class='status-tag'>TRACKING</span>")
                        .append("</li>");
                }
            }

            html.append("</ul><a href='/metrics'>&rarr; OPEN PROMETHEUS SCRAPE ENDPOINT</a>")
                .append("</div></body></html>");

            sendResponse(exchange, html.toString(), "text/html");
        }
    }

    static class AddTargetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("url=")) {
                String targetUrl = query.split("=")[1].replace("%3A", ":").replace("%2F", "/");
                if (!targetUrl.startsWith("http")) {
                    targetUrl = "http://" + targetUrl;
                }
                monitorTargets.put(targetUrl, 0);
                System.out.println("Added new target: " + targetUrl);
            }
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(303, -1);
        }
    }

    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder metrics = new StringBuilder();
            for (String target : monitorTargets.keySet()) {
                // Perform the "Deep Scan"
                SiteResult result = performDeepCheck(target);
                
                // Increment global counter for every ping performed
                totalChecks.incrementAndGet();
                
                // Update the basic status for the map
                monitorTargets.put(target, result.upStatus);
                
                // 1. Up/Down Status
                metrics.append("# HELP service_up 1=Up, 0=Down\n")
                       .append("service_up{url=\"").append(target).append("\"} ")
                       .append(result.upStatus).append("\n");

                // 2. Latency (Response Time)
                metrics.append("# HELP response_time_ms Time in milliseconds for request\n")
                       .append("response_time_ms{url=\"").append(target).append("\"} ")
                       .append(result.responseTime).append("\n");

                // 3. HTTP Status Code
                metrics.append("# HELP http_status_code Actual HTTP response code\n")
                       .append("http_status_code{url=\"").append(target).append("\"} ")
                       .append(result.statusCode).append("\n");

                // 4. Content Length
                metrics.append("# HELP content_size_bytes Page size in bytes\n")
                       .append("content_size_bytes{url=\"").append(target).append("\"} ")
                       .append(result.contentLength).append("\n");
                
                //5. number of pings
                metrics.append("# HELP monitor_checks_total Total number of pings performed by this system\n")
                   .append("monitor_checks_total ").append(totalChecks.get()).append("\n");

            }

            
            sendResponse(exchange, metrics.toString(), "text/plain");
        }
    }

    static class SiteResult {
        int upStatus;
        long responseTime;
        int statusCode;
        long contentLength;

        SiteResult(int upStatus, long responseTime, int statusCode, long contentLength) {
            this.upStatus = upStatus;
            this.responseTime = responseTime;
            this.statusCode = statusCode;
            this.contentLength = contentLength;
        }
    }

    private static SiteResult performDeepCheck(String urlString) {
        long startTime = System.currentTimeMillis();
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            
            int code = connection.getResponseCode();
            long endTime = System.currentTimeMillis();
            long size = connection.getContentLengthLong();
            
            if (size <= 0 && code == 200) {
                try (java.io.InputStream is = connection.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytes = 0;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        totalBytes += bytesRead;
                    }
                    size = totalBytes;
                }
            }
            
            int up = (code >= 200 && code < 400) ? 1 : 0;
            return new SiteResult(up, (endTime - startTime), code, (size < 0 ? 0 : size));
            
        } catch (Exception e) {
            return new SiteResult(0, -1, 0, 0);
        }
    }

    private static void sendResponse(HttpExchange exchange, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
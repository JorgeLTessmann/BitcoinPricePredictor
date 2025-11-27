import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

public class BinanceBTCPrice {
    private static final String BINANCE_API = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";
    private static final String CSV_FILE = "btc_prices.csv";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private static long lastRequestTime = 0;
    private static final long REQUEST_COOLDOWN = 1000;

    public static void main(String[] args) throws IOException {
        initializeFiles();
        loadInitialData(); 
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        
        server.createContext("/btcprice", exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            try {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRequestTime < REQUEST_COOLDOWN && cache.containsKey("price")) {
                    sendResponse(exchange, cache.get("price"));
                    return;
                }
                
                String price = getCurrentPrice();
                String response = "{\"price\":\"" + price + "\",\"cached\":false}";
                cache.put("price", response);
                lastRequestTime = currentTime;
                sendResponse(exchange, response);
            } catch (Exception e) {
                sendError(exchange, e);
            }
        });
        
        server.createContext("/history", exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            try {
                String history = getPriceHistory();
                sendResponse(exchange, history);
            } catch (Exception e) {
                sendError(exchange, e);
            }
        });
        
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("✅ Servidor rodando em http://localhost:8000");
    }

    // *** Métodos auxiliares ***
    private static void loadInitialData() {
        try {
            String price = getCurrentPrice();
            System.out.println("⚡ Preço inicial carregado: " + price);
        } catch (Exception e) {
            System.err.println("❌ Erro ao carregar dados iniciais: " + e.getMessage());
        }
    }

    private static String getCurrentPrice() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BINANCE_API))
                .timeout(Duration.ofSeconds(5))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String price = response.body().split("\"price\":\"")[1].split("\"")[0];
        savePrice(price);
        return price;
    }

    private static void savePrice(String price) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        synchronized (BinanceBTCPrice.class) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE, true))) {
                writer.println(timestamp + "," + price);
            }
        }
    }

    private static String getPriceHistory() throws IOException {
        List<String> lines = Collections.emptyList();
        synchronized (BinanceBTCPrice.class) {
            lines = Files.readAllLines(Paths.get(CSV_FILE));
        }
        
        if (lines.size() <= 1) return "[]";
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            json.append("{\"timestamp\":\"").append(parts[0]).append("\",\"price\":").append(parts[1]).append("}");
            if (i < lines.size() - 1) json.append(",");
        }
        return json.append("]").toString();
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static void sendError(HttpExchange exchange, Exception e) throws IOException {
        String error = "{\"error\":\"" + e.getMessage() + "\"}";
        exchange.sendResponseHeaders(500, error.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(error.getBytes());
        }
    }

    private static void initializeFiles() throws IOException {
        if (!Files.exists(Paths.get(CSV_FILE))) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
                writer.println("timestamp,price");
            }
        }
    }

}

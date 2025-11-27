import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class bancodedados {
    private static final String DB_DIRECTORY = "data";
    private static final String USERS_DB_FILE = DB_DIRECTORY + "/users_db.csv";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ConcurrentHashMap<String, String> sessionCache = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> recentRegistrations = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws IOException {
        initializeFiles();
        startDisplayThread();

        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);

        server.createContext("/register", exchange -> {
            if (setCorsHeaders(exchange))
                return;

            exchange.getResponseHeaders().add("Content-Type", "application/json");

            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendError(exchange, new Exception("Método não permitido"), 405);
                    return;
                }

                String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines()
                        .collect(Collectors.joining("\n"));
                Map<String, String> params = parseFormData(requestBody);

                String email = params.get("email");
                String password = params.get("password");
                String username = params.get("username");

                if (email == null || email.isEmpty() || password == null || password.isEmpty() || username == null
                        || username.isEmpty()) {
                    sendError(exchange, new Exception("Email, nome de usuário e senha são obrigatórios"), 400);
                    return;
                }

                if (!isValidEmail(email)) {
                    sendError(exchange, new Exception("Formato de e-mail inválido"), 400);
                    return;
                }

                if (userExists(email) || usernameExists(username)) {
                    sendError(exchange, new Exception("Email ou nome de usuário já em uso"), 409);
                    return;
                }

                String hashedPassword = hashPassword(password);
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

                registerUser(email, hashedPassword, username, timestamp);

                recentRegistrations.offer(email);

                String response = "{\"success\":true,\"message\":\"Usuário registrado com sucesso!\"}";
                sendResponse(exchange, response, 201);

            } catch (Exception e) {
                System.err.println("[ERROR - /register] " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, e, 500);
            }
        });

        server.createContext("/login", exchange -> {
            if (setCorsHeaders(exchange))
                return;

            exchange.getResponseHeaders().add("Content-Type", "application/json");

            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendError(exchange, new Exception("Método não permitido"), 405);
                    return;
                }

                String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines()
                        .collect(Collectors.joining("\n"));
                Map<String, String> params = parseFormData(requestBody);

                String email = params.get("email");
                String password = params.get("password");

                if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                    sendError(exchange, new Exception("Email e senha são obrigatórios"), 400);
                    return;
                }

                String hashedPassword = hashPassword(password);
                boolean authenticated = authenticateUser(email, hashedPassword);

                if (authenticated) {
                    String token = generateAuthToken(email);
                    sessionCache.put(token, email);
                    String response = "{\"success\":true,\"message\":\"Login bem-sucedido!\",\"token\":\"" + token
                            + "\"}";
                    sendResponse(exchange, response, 200);
                } else {
                    sendError(exchange, new Exception("Credenciais inválidas"), 401);
                }

            } catch (Exception e) {
                System.err.println("[ERROR - /login] " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, e, 500);
            }
        });

        server.createContext("/profile", exchange -> {
            if (setCorsHeaders(exchange))
                return;

            exchange.getResponseHeaders().add("Content-Type", "application/json");

            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendError(exchange, new Exception("Método não permitido"), 405);
                    return;
                }

                String authToken = exchange.getRequestHeaders().getFirst("Authorization");
                if (authToken == null || !sessionCache.containsKey(authToken)) {
                    sendError(exchange, new Exception("Não autorizado"), 401);
                    return;
                }

                String email = sessionCache.get(authToken);
                Map<String, String> userData = getUserData(email);

                if (userData != null) {
                    String username = userData.get("username");

                    String response = String.format("{\"success\":true,\"email\":\"%s\",\"username\":\"%s\"}", email, username); 
                    sendResponse(exchange, response, 200);
                } else {
                    sendError(exchange, new Exception("Perfil não encontrado"), 404);
                }

            } catch (Exception e) {
                System.err.println("[ERROR - /profile] " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, e, 500);
            }
        });

        server.createContext("/update-profile", exchange -> {
            if (setCorsHeaders(exchange))
                return;

            exchange.getResponseHeaders().add("Content-Type", "application/json");

            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendError(exchange, new Exception("Método não permitido"), 405);
                    return;
                }

                String authToken = exchange.getRequestHeaders().getFirst("Authorization");
                if (authToken == null || !sessionCache.containsKey(authToken)) {
                    sendError(exchange, new Exception("Não autorizado"), 401);
                    return;
                }

                String email = sessionCache.get(authToken);

                Map<String, String> formFields = new HashMap<>();
                // Map<String, Path> uploadedFiles = new HashMap<>(); // Removido

                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

                if (contentType == null || !contentType.startsWith("application/x-www-form-urlencoded")) {

                     String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines()
                            .collect(Collectors.joining("\n"));
                     formFields = parseFormData(requestBody);
                } else {
                    String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines()
                            .collect(Collectors.joining("\n"));
                    formFields = parseFormData(requestBody);
                }
                
                String newUsername = formFields.get("username");

                updateUserRecord(email, newUsername);

                String response = "{\"success\":true,\"message\":\"Perfil atualizado com sucesso!\"}"; // Atualizado
                sendResponse(exchange, response, 200);

            } catch (Exception e) {
                System.err.println("[ERROR - /update-profile] Erro ao atualizar perfil: " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, e, 500);
            }
        });

        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Servidor iniciado na porta 8001");
    }

    private static void initializeFiles() throws IOException {
        Path dbDirPath = Paths.get(DB_DIRECTORY);
        if (!Files.exists(dbDirPath)) {
            Files.createDirectories(dbDirPath);
        }

        Path usersDbPath = Paths.get(USERS_DB_FILE);
        if (!Files.exists(usersDbPath)) {
            Files.write(usersDbPath, "email,password,username,created_at,role\n".getBytes(), StandardOpenOption.CREATE);
            System.out.println("[INIT] Arquivo de banco de dados criado: " + usersDbPath.toAbsolutePath());
        }
    }

    private static void startDisplayThread() {
        new Thread(() -> {
            while (true) {
                try {
                    String registeredEmail = recentRegistrations.poll();
                    if (registeredEmail != null) {
                        System.out.println("Novo usuário registrado: " + registeredEmail);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Display thread interrupted.");
                    break;
                }
            }
        }).start();
    }

    private static boolean setCorsHeaders(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    }

    private static boolean userExists(String email) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(USERS_DB_FILE))) {
            return lines.anyMatch(line -> line.startsWith(email + ","));
        }
    }
    
    private static boolean usernameExists(String username) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(USERS_DB_FILE))) {
            return lines.skip(1).anyMatch(line -> {
                String[] parts = line.split(",", -1);
                return parts.length > 2 && parts[2].equalsIgnoreCase(username); 
            });
        }
    }

    private static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(password.getBytes());
        return bytesToHex(encodedhash);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static boolean authenticateUser(String email, String hashedPassword) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(USERS_DB_FILE))) {
            return lines.anyMatch(line -> {
                String[] parts = line.split(",", -1);
                return parts.length >= 2 && parts[0].equals(email) && parts[1].equals(hashedPassword);
            });
        }
    }

    private static String generateAuthToken(String email) throws NoSuchAlgorithmException {
        String data = email + System.currentTimeMillis();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes());
        return bytesToHex(hash);
    }

    /**
     * @param email 
     * @return 
     * @throws IOException
     */
    private static Map<String, String> getUserData(String email) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(USERS_DB_FILE))) {
            Optional<String> userLine = lines
                    .filter(line -> line.startsWith(email + ","))
                    .findFirst();

            if (userLine.isPresent()) {
                String[] parts = userLine.get().split(",", -1);
                Map<String, String> userData = new HashMap<>();
                userData.put("email", parts[0]);
                userData.put("hashedPassword", parts[1]);
                userData.put("username", parts.length > 2 ? parts[2] : "");
                userData.put("timestamp", parts.length > 3 ? parts[3] : ""); 
                userData.put("role", parts.length > 4 ? parts[4] : "default");
                return userData;
            }
        }
        return null;
    }

    /**
   
     * @param email        
     * @param hashedPassword 
     * @param username      
     * @param timestamp     
     * @throws IOException
     */
    private static void registerUser(String email, String hashedPassword, String username, String timestamp) throws IOException {
        String userRecord = String.join(",", email, hashedPassword, username, timestamp, "default");

        Files.write(Paths.get(USERS_DB_FILE), (userRecord + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
    }

    /**
     * @param emailToUpdate 
     * @param newUsername  
     * @throws IOException
     */
    private static void updateUserRecord(String emailToUpdate, String newUsername) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(USERS_DB_FILE));
        List<String> newLines = new ArrayList<>();
        boolean updated = false;

        for (String line : lines) {
            String[] parts = line.split(",", -1);
            if (parts.length > 0 && parts[0].equals(emailToUpdate)) {
                Map<String, String> userData = getUserData(emailToUpdate);
                String currentUsername = userData.get("username");

                String currentTimestamp = userData.get("timestamp");
                String currentRole = userData.get("role");

                String finalUsername = (newUsername != null && !newUsername.trim().isEmpty()) ? newUsername : currentUsername;
           

                newLines.add(String.join(",",
                        parts[0], 
                        parts[1], 
                        finalUsername, 
    
                        currentTimestamp, 
                        currentRole 
                ));
                updated = true;
            } else {
                newLines.add(line);
            }
        }

        if (updated) {
            Files.write(Paths.get(USERS_DB_FILE), newLines, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("[DEBUG - updateUserRecord] User DB file updated for " + emailToUpdate);
        } else {
            System.out.println("[DEBUG - updateUserRecord] User " + emailToUpdate + " not found or no update needed.");
        }
    }

    private static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        if (formData == null || formData.isEmpty()) {
            return params;
        }

        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = idx < pair.length() - 1
                        ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                        : "";
                params.put(key, value);
            }
        }
        return params;
    }



    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void sendError(HttpExchange exchange, Exception e, int statusCode) throws IOException {
        String error = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        exchange.sendResponseHeaders(statusCode, error.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(error.getBytes(StandardCharsets.UTF_8));
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);

            String requestPath = exchange.getRequestURI().getPath();
            Path filePath;

            String baseDir = System.getProperty("user.dir");


            System.out.println("[DEBUG - StaticFileHandler] Request path: " + requestPath);


            if (requestPath.equals("/")) {
                filePath = Paths.get(baseDir, "index.html");
            } else if (requestPath.startsWith("/images/")) { 
                filePath = Paths.get(baseDir, "images", requestPath.substring("/images/".length()));
            }

            else {
                filePath = Paths.get(baseDir, requestPath.substring(1));
            }

            if (!filePath.normalize().startsWith(Paths.get(baseDir).normalize())) {
                sendResponse(exchange, "403 (Forbidden)", 403);
                System.err.println("[SECURITY] Attempt to access file outside base directory: " + filePath.normalize());
                return;
            }

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {

                    if (filePath.toString().endsWith(".html"))
                        contentType = "text/html";
                    else if (filePath.toString().endsWith(".css"))
                        contentType = "text/css";
                    else if (filePath.toString().endsWith(".js"))
                        contentType = "application/javascript";
                    else if (filePath.toString().endsWith(".png"))
                        contentType = "image/png";
                    else if (filePath.toString().endsWith(".jpg") || filePath.toString().endsWith(".jpeg"))
                        contentType = "image/jpeg";
                    else if (filePath.toString().endsWith(".gif"))
                        contentType = "image/gif";
                    else if (filePath.toString().endsWith(".webp"))
                        contentType = "image/webp";
                    else
                        contentType = "application/octet-stream";
                }
                
                System.out.println("[DEBUG - StaticFileHandler] Serving file: " + filePath.getFileName() + " with Content-Type: " + contentType);

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, 0); 
                Files.copy(filePath, exchange.getResponseBody());
            } else {
                System.out.println("[DEBUG - StaticFileHandler] File not found or is a directory: " + filePath.toAbsolutePath());
                sendResponse(exchange, "404 (Not Found)", 404);
            }
        }

        private boolean setCorsHeaders(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return true;
            }
            return false;
        }
    }
}
import utils.ConnectConfig;
import utils.DatabaseConnector;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import java.util.logging.Logger;
import org.apache.commons.lang3.SystemUtils;
import java.io.IOException;
import com.sun.net.httpserver.Headers;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import queries.*;
import java.util.List;
import entities.*;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.lang.String;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());
    private static LibraryManagementSystem library;
    private static ConnectConfig connectConfig = null;
    private static DatabaseConnector connector;

    public static void getCardIDfromURL(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {
        if (query != null) {
            String[] paras = query.split("&");
            for (String para : paras) {
                String[] key = para.split("=");
                String idx = URLDecoder.decode(key[0], "UTF-8");
                String val = URLDecoder.decode(key[1], "UTF-8");
                if (!parameters.containsKey(idx)) {
                    parameters.put(idx, val);
                }
            }
        }
    }

    private static void cardhandleGetRequest(HttpExchange exchange) throws IOException {
        // 响应头，因为是JSON通信
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        // 构建JSON响应数据，这里简化为字符串
        // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
        ApiResult result = library.showCards();
        String response = "[";
        List<Card> cards=((CardList) result.payload).getCards();
        for (int i = 0; i < cards.size(); i++) {
            Card element = cards.get(i);
            if(i>0) response=response+",";
            response=response+"{\"id\":" + element.getCardId() + ",\"name\":\"" + element.getName() + "\",\"department\":\""+element.getDepartment()+"\",\"type\":\""+element.getType()+"\"}";
        }
        response=response+"]";
        // 写
        outputStream.write(response.getBytes());
        // 流一定要close！！！小心泄漏
        outputStream.close();
    }

    private static void borrowhandleGetRequest(HttpExchange exchange) throws IOException {
        String cardID="";
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            // 获取请求中的参数
            Map<String, Object> parameters = new HashMap<>();
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            getCardIDfromURL(query, parameters);
            cardID = (String) parameters.get("cardID");
            // 现在，cardID 变量中包含了来自前端的参数 this.toQuery 的值
            // 可以在这里进行进一步的处理
        }
        // 响应头，因为是JSON通信
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        // 构建JSON响应数据，这里简化为字符串
        // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
        ApiResult result = library.showBorrowHistory(Integer.parseInt(cardID));
        List<BorrowHistories.Item> items=((BorrowHistories) result.payload).getItems();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        String response="[";
        for (int i = 0; i < items.size(); i++) {
            BorrowHistories.Item element = items.get(i);
            if(i>0) response=response+",";
            response=response+"{\"cardID\": "+element.getCardId()+",\"bookID\": "+element.getBookId()+",\"borrowTime\": \""+dateFormat.format(new Date(element.getBorrowTime()))+" \",\"returnTime\": \"";
            if(element.getReturnTime()==0L) response=response+"未归还";
            else response=response+dateFormat.format(new Date(element.getReturnTime()));
            response=response+"\"}";
        }
        response=response+"]";
        // 写
        outputStream.write(response.getBytes());
        // 流一定要close！！！小心泄漏
        outputStream.close();
    }

    private static void bookhandleGetRequest(HttpExchange exchange) throws IOException {
        // 响应头，因为是JSON通信
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // 状态码为200，也就是status ok
        exchange.sendResponseHeaders(200, 0);
        // 获取输出流，java用流对象来进行io操作
        OutputStream outputStream = exchange.getResponseBody();
        // 构建JSON响应数据，这里简化为字符串
        // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
        BookQueryConditions conditions=new BookQueryConditions();
        ApiResult result = library.queryBook(conditions);
        String response = "[";
        List<Book> Books=((BookQueryResults) result.payload).getResults();
        for (int i = 0; i < Books.size(); i++) {
            Book element = Books.get(i);
            if(i>0) response=response + ",";
            response=response + "{\"id\":" + element.getBookId() + ",\"category\":\"" + element.getCategory() + "\",\"title\":\""
                    + element.getTitle() + "\",\"press\":\"" + element.getPress() + "\",\"publish_year\":" + element.getPublishYear()
                    + ",\"author\":\"" + element.getAuthor() + "\",\"price\":\"" + element.getPrice() + "\",\"stock\":\"" + element.getStock()
                    + "\"}";
        }
        response=response+"]";
        // 写
        outputStream.write(response.getBytes());
        // 流一定要close！！！小心泄漏
        outputStream.close();
    }

    private static void cardhandlePostRequest(HttpExchange exchange) throws IOException {
        // 读取POST请求体
        InputStream requestBody = exchange.getRequestBody();
        // 用这个请求体（输入流）构造个buffered reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        // 拼字符串的
        StringBuilder requestBodyBuilder = new StringBuilder();
        // 用来读的
        String response="";
        String line;
        // 没读完，一直读，拼到string builder里
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
            JsonElement posts = JsonParser.parseString(line);
            JsonObject tmp = posts.getAsJsonObject();

            if(tmp.has("cardID") && tmp.has("name")) {
                Card card = new Card(
                    tmp.get("cardID").getAsInt(),
                    tmp.get("name").getAsString(),
                    tmp.get("department").getAsString(),
                    tmp.get("type").getAsString().equals("学生") ? Card.CardType.Student : Card.CardType.Teacher
                );
                response = library.modifyCardInfo(card).message;
            }
            else if(tmp.has("cardID")){
                response = library.removeCard(tmp.get("cardID").getAsInt()).message;
            }
            else{
                Card card = new Card();
                card.setName(tmp.get("name").getAsString());
                card.setDepartment(tmp.get("department").getAsString());
                card.setType(tmp.get("type").getAsString().equals("学生") ? Card.CardType.Student : Card.CardType.Teacher);
                response = library.registerCard(card).message;
            }
        }
        // 看看读到了啥
        // 实际处理可能会更复杂点
        System.out.println("Received POST request to create card with data: " + requestBodyBuilder.toString());

        // 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        // 响应状态码200
        exchange.sendResponseHeaders(200, 0);

        // 剩下三个和GET一样
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.close();
    }

    private static void bookhandlePostRequest(HttpExchange exchange) throws IOException, ParseException {
        // 读取POST请求体
        InputStream requestBody = exchange.getRequestBody();
        // 用这个请求体（输入流）构造个buffered reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        // 拼字符串的
        StringBuilder requestBodyBuilder = new StringBuilder();
        // 用来读的
        String response="";
        String line;
        // 没读完，一直读，拼到string builder里
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
            JsonElement Commend = JsonParser.parseString(line);
            JsonObject tmp = Commend.getAsJsonObject();
            int type = tmp.get("type").getAsInt();
            System.out.println(type);
            if(type == 1){
                Book book=new Book(
                        tmp.get("category").getAsString(),
                        tmp.get("title").getAsString(),
                        tmp.get("press").getAsString(),
                        tmp.get("publish_year").getAsInt(),
                        tmp.get("author").getAsString(),
                        tmp.get("price").getAsDouble(),
                        tmp.get("stock").getAsInt()
                );
                boolean ok = library.storeBook(book).ok;
                if(ok)response = "Create book successfully";
                else response = "Create book failed";
            }
            else if(type == 2){
                Book book = new Book();
                book.setBookId(tmp.get("bookID").getAsInt());
                book.setCategory(tmp.get("category").getAsString());
                book.setTitle(tmp.get("title").getAsString());
                book.setPress(tmp.get("press").getAsString());
                book.setPublishYear(tmp.get("publish_year").getAsInt());
                book.setAuthor(tmp.get("author").getAsString());
                book.setPrice(tmp.get("price").getAsDouble());
                response = library.modifyBookInfo(book).message;
            }
            else if(type == 3){
                int bookID = tmp.get("bookID").getAsInt();
                boolean ok = library.removeBook(bookID).ok;
                if(ok)response = "Remove successfully";
                else response = "Remove failed";
            }
            else if(type == 4){
                int bookID = tmp.get("bookID").getAsInt();
                int stock = tmp.get("stock").getAsInt();
                boolean ok = library.incBookStock(bookID,stock).ok;
                if(ok)response = "Modify stock successfully";
                else response = "Modify stock failed";
            }
            else if(type == 5){
                BookQueryConditions conditions=new BookQueryConditions();
                if(!tmp.get("category").getAsString().equals(""))
                    conditions.setCategory(tmp.get("category").getAsString());
                if(!tmp.get("title").getAsString().equals(""))
                    conditions.setTitle(tmp.get("title").getAsString());
                if(!tmp.get("press").getAsString().equals(""))
                    conditions.setPress(tmp.get("press").getAsString());
                if(!tmp.get("min_year").getAsString().equals(""))
                    conditions.setMinPublishYear(tmp.get("min_year").getAsInt());
                if(!tmp.get("max_year").getAsString().equals(""))
                    conditions.setMaxPublishYear(tmp.get("max_year").getAsInt());
                if(!tmp.get("author").getAsString().equals(""))
                    conditions.setAuthor(tmp.get("author").getAsString());
                if(!tmp.get("min_price").getAsString().equals(""))
                    conditions.setMinPrice(tmp.get("min_price").getAsDouble());
                if(!tmp.get("max_price").getAsString().equals(""))
                    conditions.setMaxPrice(tmp.get("max_price").getAsDouble());

                ApiResult result = library.queryBook(conditions);
                response = "[";
                List<Book> Books=((BookQueryResults) result.payload).getResults();
                for (int i = 0; i < Books.size(); i++) {
                    Book element = Books.get(i);
                    if(i>0) response=response+",";
                    response=response+"{\"id\":";
                    response=response+element.getBookId();
                    response=response+",\"category\":\"";
                    response=response+element.getCategory();
                    response=response+"\",\"title\":\"";
                    response=response+element.getTitle();
                    response=response+"\",\"press\":\"";
                    response=response+element.getPress();
                    response=response+"\",\"publish_year\":";
                    response=response+element.getPublishYear();
                    response=response+",\"author\":\"";
                    response=response+element.getAuthor();
                    response=response+"\",\"price\":\"";
                    response=response+element.getPrice();
                    response=response+"\",\"stock\":\"";
                    response=response+element.getStock();
                    response=response+"\"}";
                }
                response=response+"]";
            }
            else if(type == 6){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                long longValue=0;
                try{
                    Date date = sdf.parse(tmp.get("borrowTime").getAsString());
                    longValue = date.getTime();
                }catch (ParseException e) {
                    // 处理异常
                    e.printStackTrace();
                }
                Borrow borrow=new Borrow();
                borrow.setBookId(tmp.get("bookID").getAsInt());
                borrow.setCardId(tmp.get("cardID").getAsInt());
                borrow.setBorrowTime(longValue);
                borrow.setReturnTime(0);
                response=library.borrowBook(borrow).message;
            }
            else if(type == 7){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                long longValue=0;
                try{
                    Date date = sdf.parse(tmp.get("returnTime").getAsString());
                    longValue = date.getTime();
                }catch (ParseException e) {
                    // 处理异常
                    e.printStackTrace();
                }
                Borrow borrow=new Borrow();
                borrow.setBookId(tmp.get("bookID").getAsInt());
                borrow.setCardId(tmp.get("cardID").getAsInt());
                borrow.setBorrowTime(longValue);
                borrow.setReturnTime(longValue);
                response=library.returnBook(borrow).message;
            }
            else if(type == 8){
                String text = tmp.get("text").getAsString();
                String regex = "(.*,.*,.*,.*,.*,.*,.*)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(text);
                List<Book>books=new ArrayList<>();
                while (matcher.find()) {
                    String[] parts = matcher.group(0).split(",");
                    Book book=new Book();
                    book.setCategory(parts[0]);
                    book.setTitle(parts[1]);
                    book.setPress(parts[2]);
                    book.setPublishYear(Integer.parseInt(parts[3]));
                    book.setAuthor(parts[4]);
                    book.setPrice(Double.parseDouble(parts[5]));
                    book.setStock(Integer.parseInt(parts[6]));
                    books.add(book);
                }
                response=library.storeBook(books).message;
            }
        }
        // 看看读到了啥
        // 实际处理可能会更复杂点
        System.out.println("Received POST request to create card with data: " + requestBodyBuilder.toString());

        // 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        // 响应状态码200
        exchange.sendResponseHeaders(200, 0);

        // 剩下三个和GET一样
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.close();
    }

    private static void handleOptionsRequest(HttpExchange exchange) throws IOException {
        // 读取POST请求体
        InputStream requestBody = exchange.getRequestBody();
        // 用这个请求体（输入流）构造个buffered reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
        // 拼字符串的
        StringBuilder requestBodyBuilder = new StringBuilder();
        // 用来读的
        String line;
        // 没读完，一直读，拼到string builder里
        while ((line = reader.readLine()) != null) {
            requestBodyBuilder.append(line);
        }

        // 看看读到了啥
        // 实际处理可能会更复杂点

        // 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        // 响应状态码204
        exchange.sendResponseHeaders(204, 0);

        // 剩下三个和GET一样
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write("Card created successfully".getBytes());
        outputStream.close();
    }

    static class CardHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                cardhandleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                cardhandlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class BorrowHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                borrowhandleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST

            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class BookHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                bookhandleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                try {
                    bookhandlePostRequest(exchange);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    public static void main(String[] args)  throws IOException {
        try {
            connectConfig = new ConnectConfig();
            log.info("Success to parse connect config. " + connectConfig.toString());
            connector = new DatabaseConnector(connectConfig);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            library = new LibraryManagementSystemImpl(connector);
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            // 添加handler，这里就绑定到/card路由
            // 所以localhost:8000/card是会有handler来处理
            server.createContext("/card", new CardHandler());
            server.createContext("/borrow", new BorrowHandler());
            server.createContext("/book", new BookHandler());
//            server.createContext("/login", new LoginHandler());
            // 启动服务器
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 标识一下，这样才知道我的后端启动了（确信
    }


}

import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }


    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        ResultSet rs = null;
        try {
            // 插入新书
            String insertQuery = "INSERT INTO book(category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement psInsert = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);

            int index = 1;
            psInsert.setString(index++, book.getCategory());
            psInsert.setString(index++, book.getTitle());
            psInsert.setString(index++, book.getPress());
            psInsert.setInt(index++, book.getPublishYear());
            psInsert.setString(index++, book.getAuthor());
            psInsert.setDouble(index++, book.getPrice());
            psInsert.setInt(index++, book.getStock());

            int rowsInserted = psInsert.executeUpdate();
//            System.out.println("FFF");
            if (rowsInserted == 0) {
                rollback(conn);
                return new ApiResult(false, "Failed to insert the book.");
            }
            ResultSet generatedKeys = psInsert.getGeneratedKeys();
            if (generatedKeys.next()) {
                int generatedBookId = generatedKeys.getInt(1);
                book.setBookId(generatedBookId);
                commit(conn);
                return new ApiResult(true, "Book stored successfully.", book);
            } else {
                rollback(conn);
                return new ApiResult(false, "Unable to retrieve generated book_id.");
            }
        } catch (SQLException e) {
            rollback(conn);
            return new ApiResult(false, "An error occurred while storing the book: " + e.getMessage());
        }
    }


    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        String QueryStock = "SELECT stock FROM book WHERE book_id = ?";
        String UpdateStock = "UPDATE book SET stock = ? WHERE book_id = ?";
        PreparedStatement psUpdate = null;
        PreparedStatement psquery = null;
        ResultSet rs = null;
        try {
            psquery = conn.prepareStatement(QueryStock);
            psquery.setInt(1, bookId);
            rs = psquery.executeQuery();
            if(!rs.next()){
                throw new SQLException("The book does not exist.");
            }
            psUpdate = conn.prepareStatement(UpdateStock);
            int cur = rs.getInt("stock");
            int updatedStock = cur + deltaStock;
            if(updatedStock < 0){
                throw new SQLException("The book does not exist.");
            }
            psUpdate.setInt(1, updatedStock);
            psUpdate.setInt(2, bookId);
            int rows = psUpdate.executeUpdate();
            if(rows == 0) {
                throw new SQLException("An error occurred while updating the book stock.");
            }
            commit(conn);
        } catch (SQLException e) {
            rollback(conn);
            return new ApiResult(false, "An error occurred while updating the stock: " + e.getMessage());
        }
        return new ApiResult(true, "Book stock updated successfully.", bookId);
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        ResultSet rs = null;

        try{
            conn.setAutoCommit(false);
            String Inssql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement psInsert = conn.prepareStatement(Inssql, PreparedStatement.RETURN_GENERATED_KEYS);

            for(Book book : books){
                int index = 1;
                psInsert.setString(index++, book.getCategory());
                psInsert.setString(index++, book.getTitle());
                psInsert.setString(index++, book.getPress());
                psInsert.setInt(index++, book.getPublishYear());
                psInsert.setString(index++, book.getAuthor());
                psInsert.setDouble(index++, book.getPrice());
                psInsert.setInt(index++, book.getStock());
                int rowsInserted = psInsert.executeUpdate();
                if (rowsInserted == 0) {
                    throw new SQLException( "Failed to insert the book.");
                }
                ResultSet generatedKeys = psInsert.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int generatedBookId = generatedKeys.getInt(1);
                    book.setBookId(generatedBookId);
                } else {
                    throw new SQLException("Unable to retrieve generated book_id.");
                }
            }
            commit(conn);
        } catch (SQLException e){
            rollback(conn);
            return new ApiResult(false, "An error occurred while storing the book: " + e.getMessage());
        }
        return new ApiResult(true, "Book stored successfully.", books);
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        PreparedStatement psRemove = null;
        String checkQuery = "SELECT COUNT(*) FROM borrow WHERE book_id = ? AND return_time = 0";

        try{
            psRemove = conn.prepareStatement(checkQuery);
            psRemove.setInt(1, bookId);
            ResultSet rs = psRemove.executeQuery();
            if(rs.next() && rs.getInt(1) > 0){
                return new ApiResult(false, "The book does not returned.");
            }
            String sqlDelete = "DELETE FROM book WHERE book_id = ?";
            PreparedStatement psDelete = conn.prepareStatement(sqlDelete);
            psDelete.setInt(1, bookId);
            int rowsDeleted = psDelete.executeUpdate();
            if (rowsDeleted == 0) {
                return new ApiResult(false, "Failed to delete the book.");
            }
            return new ApiResult(true, "Deleted the book.");
        }catch (SQLException e){
            return new ApiResult(false, "An error occurred while removing the book: " + e.getMessage());
        }
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        String sqlUpdate = "UPDATE book SET";
        List<Object> parms = new ArrayList<>();

        if(book.getCategory() != null){
            sqlUpdate += " category = ?, ";
            parms.add(book.getCategory());
        }
        if(book.getTitle() != null){
            sqlUpdate += " title = ?, ";
            parms.add(book.getTitle());
        }
        if(book.getPress() != null){
            sqlUpdate += " press = ?, ";
            parms.add(book.getPress());
        }
        if(book.getPublishYear() != 0){
            sqlUpdate += " publish_year = ?, ";
            parms.add(book.getPublishYear());
        }
        if(book.getAuthor() != null){
            sqlUpdate += " author = ?, ";
            parms.add(book.getAuthor());
        }
        if(book.getPrice() != 0){
            sqlUpdate += " price = ?, ";
            parms.add(book.getPrice());
        }
        sqlUpdate = sqlUpdate.substring(0, sqlUpdate.length() - 2) + " WHERE book_id = ?";
        parms.add(book.getBookId());
        PreparedStatement psUpdate = null;

        try{
            if(book.getPrice() <= 0){
                return new ApiResult(false, "Failed to update the book.");
            }
            psUpdate = conn.prepareStatement(sqlUpdate);
            for(int i = 0;i < parms.size(); i++){
                psUpdate.setObject(i + 1, parms.get(i));
            }
            int rows = psUpdate.executeUpdate();
            if(rows == 0){
                return new ApiResult(false, "Failed to update the book.");
            }
            return new ApiResult(true, "Book updated successfully.", book);
        }catch (SQLException e){
            return new ApiResult(false, "An error occurred while updating the book: " + e.getMessage());
        }
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        StringBuilder sql = new StringBuilder("SELECT * FROM book WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if(conditions.getCategory() != null){
            sql.append(" AND category = ?");
            params.add(conditions.getCategory());
        }
        if(conditions.getTitle() != null){
            sql.append(" AND title like ?");
            params.add("%" + conditions.getTitle() + "%");
        }
        if(conditions.getPress() != null){
            sql.append(" AND press like ?");
            params.add("%" + conditions.getPress() + "%");
        }
        if(conditions.getMaxPublishYear() != null){
            sql.append(" AND publish_year <= ?");
            params.add(conditions.getMaxPublishYear());
        }
        if(conditions.getMinPublishYear() != null){
            sql.append(" AND publish_year >= ?");
            params.add(conditions.getMinPublishYear());
        }
        if(conditions.getAuthor() != null){
            sql.append(" AND author like ?");
            params.add("%" + conditions.getAuthor() + "%");
        }
        if(conditions.getMinPrice() != null){
            sql.append(" AND price >= ?");
            params.add(conditions.getMinPrice());
        }
        if(conditions.getMaxPrice() != null){
            sql.append(" AND price <= ?");
            params.add(conditions.getMaxPrice());
        }

        sql.append(" ORDER BY ");
        sql.append(conditions.getSortBy());
        sql.append(" ");
        sql.append(conditions.getSortOrder().toString());
        sql.append(", book_id ASC");
        PreparedStatement psQuery = null;
        ResultSet rs = null;
        List<Book> books = new ArrayList<>();

        try {
            psQuery = conn.prepareStatement(sql.toString());
            for(int i = 0;i < params.size();++i){
                psQuery.setObject(i + 1, params.get(i));
            }
            rs = psQuery.executeQuery();
            while(rs.next()){
                Book book = new Book (
                        rs.getString("category"),
                        rs.getString("title"),
                        rs.getString("press"),
                        rs.getInt("publish_year"),
                        rs.getString("author"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                );
                book.setBookId(rs.getInt("book_id"));
                books.add(book);
            }
            BookQueryResults ans = new BookQueryResults(books);
            return new ApiResult(true,"Query successfully", ans);
        } catch (SQLException e){
            return new ApiResult(false, "Query failed!" + e.getMessage());
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        String sqlBorrow = "SELECT COUNT(*) FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0 for update";
        PreparedStatement psBorrow = null;
        ResultSet rs = null;
        try{
            conn.setAutoCommit(false);
            String sql = "SELECT stock FROM book WHERE book_id = ? for update";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, borrow.getBookId());
            ResultSet rsBorrow = ps.executeQuery();
            if(!rsBorrow.next()){
                commit(conn);
                return new ApiResult(false,"Not exist book");
            } else if(rsBorrow.getInt(1) <= 0){
                commit(conn);
                return new ApiResult(false,"stock 0");
            }
            sql = "SELECT * FROM card WHERE card_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, borrow.getCardId());
            rsBorrow = ps.executeQuery();
            if(!rsBorrow.next()){
                commit(conn);
                return new ApiResult(false,"Not exist card");
            }
            psBorrow = conn.prepareStatement(sqlBorrow);
            psBorrow.setInt(1, borrow.getBookId());
            psBorrow.setInt(2, borrow.getCardId());
            rs = psBorrow.executeQuery();
            if(rs.next() && rs.getInt(1) > 0){
                throw new SQLException("未归还");
            }
            String stockUpdate = "UPDATE book SET stock = stock - 1 WHERE book_id = ? AND stock > 0";
            PreparedStatement psUpdate = null;
            psUpdate = conn.prepareStatement(stockUpdate);
            psUpdate.setInt(1, borrow.getBookId());
            int rowsUpdate = psUpdate.executeUpdate();
            if(rowsUpdate == 0){
                throw new SQLException("借书失败");
            }
            String sqlInsert = "INSERT INTO borrow (card_id, book_id, borrow_time) VALUES (?, ?, ?)";
            PreparedStatement psInsert = null;
            psInsert = conn.prepareStatement(sqlInsert);
            psInsert.setInt(1, borrow.getCardId());
            psInsert.setInt(2, borrow.getBookId());
            psInsert.setLong(3, borrow.getBorrowTime());
            int rows = psInsert.executeUpdate();
            commit(conn);
        } catch (SQLException e){
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, "An error occurred while borrowing the book: " + e.getMessage());
        }
        return new ApiResult(true, "Book borrowed successfully.", borrow);
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        String returnQuery = "UPDATE borrow SET return_time = ? WHERE book_id = ? AND card_id = ? AND return_time = 0";
        PreparedStatement psReturn = null;
        String tmpQuery = "SELECT borrow_time FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0 for update";
        PreparedStatement tQ = null;

        try{
            conn.setAutoCommit(false);
            tQ = conn.prepareStatement(tmpQuery);
            tQ.setInt(1, borrow.getBookId());
            tQ.setInt(2, borrow.getCardId());
            ResultSet rs = tQ.executeQuery();
            if(!rs.next()){
                return new ApiResult(false, "The book does not exist.");
            }
            if(borrow.getReturnTime() <= rs.getLong("borrow_time")){
                return new ApiResult(false, "The book already returned.");
            }
            psReturn = conn.prepareStatement(returnQuery);
            psReturn.setLong(1, borrow.getReturnTime());
            psReturn.setInt(2, borrow.getBookId());
            psReturn.setInt(3, borrow.getCardId());
            int rows = psReturn.executeUpdate();
            if(rows == 0){
                throw new SQLException("Failed to return the book.");
            }
            String stockUpdate = "UPDATE book SET stock = stock + 1 WHERE book_id = ?";
            PreparedStatement psUpdate = null;
            psUpdate = conn.prepareStatement(stockUpdate);
            psUpdate.setInt(1, borrow.getBookId());
            int rowsUpdate = psUpdate.executeUpdate();
            if(rowsUpdate == 0){
                throw new SQLException("Failed to return the book.");
            }
            commit(conn);
        } catch (SQLException e){
            rollback(conn);
            return new ApiResult(false, "An error occurred while returning the book: " + e.getMessage());
        }
        return new ApiResult(true, "Book returned successfully.", borrow);
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        String sqlQuery = "SELECT * FROM borrow INNER JOIN book ON borrow.book_id = book.book_id WHERE borrow.card_id = ? ORDER BY borrow.borrow_time DESC, borrow.book_id ASC";
        PreparedStatement psQuery = null;
        ResultSet rs = null;
        try{
            psQuery = conn.prepareStatement(sqlQuery);
            psQuery.setInt(1, cardId);
            rs = psQuery.executeQuery();
            List<BorrowHistories.Item> borrowHistories = new ArrayList<>();
            while(rs.next()){
                Book book = new Book (
                        rs.getString("category"),
                        rs.getString("title"),
                        rs.getString("press"),
                        rs.getInt("publish_year"),
                        rs.getString("author"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                );
                Borrow borrow = new Borrow (
                    rs.getInt("book_id"),
                    rs.getInt("card_id")
                );
                borrow.setBorrowTime(rs.getLong("borrow_time"));
                borrow.setReturnTime(rs.getLong("return_time"));
                book.setBookId(rs.getInt("book_id"));
                BorrowHistories.Item item = new BorrowHistories.Item(
                    rs.getInt("card_id"),
                    book,
                    borrow
                );
                borrowHistories.add(item);
            }
            BorrowHistories ans = new BorrowHistories(borrowHistories);
            return new ApiResult(true, "Success",ans);
        } catch(SQLException e){
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        PreparedStatement psRegister = null;
        String sqlReg = "INSERT INTO card (name, department, type) VALUES (?, ?, ?)";
        try{
            psRegister = conn.prepareStatement(sqlReg, PreparedStatement.RETURN_GENERATED_KEYS);
            psRegister.setString(1, card.getName());
            psRegister.setString(2, card.getDepartment());
            psRegister.setObject(3, card.getType().getStr());
            int rows = psRegister.executeUpdate();
            if(rows == 0){
                return new ApiResult(false, "Failed to create the card.");
            }
            ResultSet rs = psRegister.getGeneratedKeys();
            if(rs.next()){
                int cardId = rs.getInt(1);
                card.setCardId(cardId);
                return new ApiResult(true, "Card created successfully.");
            }
            else {
                return new ApiResult(false, "Failed to create the card.");
            }
        } catch (SQLException e){
            e.printStackTrace();
            return new ApiResult(false, "An error occurred while registering the card: " + e.getMessage());
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        String sql = "DELETE FROM card WHERE card_id = ?";
        String Query = "SELECT COUNT(*) FROM borrow WHERE card_id = ? AND return_time = 0";
        PreparedStatement psRemove = null;
        PreparedStatement psQuery = null;

        try{
            psQuery = conn.prepareStatement(Query);
            psQuery.setInt(1, cardId);
            ResultSet rs = psQuery.executeQuery();
            if(rs.next() && rs.getInt(1) > 0){
                return new ApiResult(false, "Card deleted unsuccessfully.");
            }
            psRemove = conn.prepareStatement(sql);
            psRemove.setInt(1, cardId);
            int row = psRemove.executeUpdate();
            if(row == 0){
                return new ApiResult(false, "Failed to borrow the card.");
            }
            return new ApiResult(true, "Card deleted successfully.");
        } catch(SQLException e){
            return new ApiResult(false, "An error occurred while removing the card: " + e.getMessage());
        }
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        String sql = "SELECT * FROM card";
        PreparedStatement psQuery = null;
        List<Card> cards = new ArrayList<>();

        try{
            psQuery = conn.prepareStatement(sql);
            ResultSet rs = psQuery.executeQuery();
            while(rs.next()){
                Card card = new Card (
                    rs.getInt("card_id"),
                    rs.getString("name"),
                    rs.getString("department"),
                    Card.CardType.values(rs.getString("type"))
                );
                cards.add(card);
            }
            cards.sort(Comparator.comparingInt(Card::getCardId));
            CardList cardList = new CardList(cards);
            return new ApiResult(true,"Query successfully", cardList);
        } catch (SQLException e){
            return new ApiResult(false, "An error occurred while retrieving the cards: " + e.getMessage());
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();
        try {
            String sql = "SELECT * FROM card WHERE card_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, card.getCardId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new Exception("借书证不存在");
            }

            String sqlUpdate = "UPDATE card SET name= ? ,department= ? ,type= ?  WHERE card_id= ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setString(1, card.getName());
            psUpdate.setString(2, card.getDepartment());
            psUpdate.setString(3, card.getType().getStr());
            psUpdate.setInt(4, card.getCardId());
            psUpdate.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "修改成功");
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

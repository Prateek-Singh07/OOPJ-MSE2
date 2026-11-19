package com.example.orders;

import java.sql.*;
import java.util.Scanner;

public class OrderApp {
    private static final Scanner INPUT = new Scanner(System.in);
    private static Connection db;

    private static String ask(String prompt) {
        System.out.print(prompt);
        return INPUT.nextLine().trim();
    }
    private static int askInt(String prompt) { return Integer.parseInt(ask(prompt)); }
    private static double askDouble(String prompt) { return Double.parseDouble(ask(prompt)); }
    private static String normalizeStatus(String status) {
        for (String s : new String[]{"Pending", "Shipped", "Delivered", "Cancelled"}) {
            if (s.equalsIgnoreCase(status.trim())) return s;
        }
        return null;
    }
    private static void show(ResultSet r) throws SQLException {
        System.out.printf("%d | %s | %s | Qty=%d | Unit=%.2f | %s%n",
            r.getInt("order_id"), r.getString("customer_name"), r.getString("product_name"),
            r.getInt("quantity"), r.getDouble("unit_price"), r.getString("status"));
    }
    private static void setup() throws SQLException {
        try (Statement st = db.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Orders (" +
                "order_id INT PRIMARY KEY, customer_name VARCHAR(100) NOT NULL, " +
                "product_name VARCHAR(100) NOT NULL, quantity INT NOT NULL, " +
                "unit_price DOUBLE NOT NULL, status VARCHAR(30) NOT NULL)");
        }
    }
    private static void add(int id, String customer, String product, int qty, double price, String status) throws SQLException {
        if (qty < 0 || price < 0 || normalizeStatus(status) == null) {
            System.out.println("Invalid quantity, price, or status"); return;
        }
        String sql = "INSERT INTO Orders(order_id,customer_name,product_name,quantity,unit_price,status) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1,id);ps.setString(2,customer);ps.setString(3,product);
            ps.setInt(4,qty);ps.setDouble(5,price);ps.setString(6,normalizeStatus(status));
            ps.executeUpdate();
            System.out.println("Inserted order " + id);
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Duplicate order ID " + id + " - skipped");
        }
    }
    // Part (a): insert at least 3 records quickly using PreparedStatement
    private static void insertThreeSamples() throws SQLException {
        add(1001,"Asha","Laptop",3,49999,"Pending");
        add(1002,"Ravi","Keyboard",5,1299,"Pending");
        add(1003,"Neha","Mouse",1,599,"Cancelled");
    }
    private static void insertCustom() throws SQLException {
        int id=askInt("Order ID: "); String customer=ask("Customer name: ");
        String product=ask("Product name: "); int qty=askInt("Quantity: ");
        double price=askDouble("Unit price: "); String status=ask("Status (Pending/Shipped/Delivered/Cancelled): ");
        add(id,customer,product,qty,price,status);
    }
    // Part (b)
    private static void findByStatus() throws SQLException {
        String status=normalizeStatus(ask("Status: "));
        if (status==null) {System.out.println("Invalid status");return;}
        String sql="SELECT * FROM Orders WHERE status=? AND quantity>2 ORDER BY unit_price DESC";
        try(PreparedStatement ps=db.prepareStatement(sql)) {
            ps.setString(1,status);
            try(ResultSet rs=ps.executeQuery()) {
                boolean found=false;
                while(rs.next()){ show(rs);found=true; }
                if(!found)System.out.println("No matching orders");
            }
        }
    }
    // Part (c)
    private static void increaseQuantity() throws SQLException {
        int id=askInt("Order ID: ");int amount=askInt("Additional quantity: ");
        if(amount<=0){System.out.println("Enter a positive quantity");return;}
        try(PreparedStatement ps=db.prepareStatement("UPDATE Orders SET quantity=quantity+? WHERE order_id=?")){
            ps.setInt(1,amount);ps.setInt(2,id);
            System.out.println(ps.executeUpdate()==1?"Quantity updated":"Order not found");
        }
    }
    // Part (d)
    private static void updateStatus() throws SQLException {
        int id=askInt("Order ID: ");String status=normalizeStatus(ask("New status: "));
        if(status==null){System.out.println("Invalid status. Use Pending, Shipped, Delivered, or Cancelled");return;}
        try(PreparedStatement ps=db.prepareStatement("UPDATE Orders SET status=? WHERE order_id=?")){
            ps.setString(1,status);ps.setInt(2,id);
            if(ps.executeUpdate()==0){System.out.println("Order not found");return;}
        }
        try(PreparedStatement ps=db.prepareStatement("SELECT * FROM Orders WHERE order_id=?")){
            ps.setInt(1,id);
            try(ResultSet rs=ps.executeQuery()){if(rs.next())show(rs);}
        }
    }
    // Part (e)
    private static void totalAndDelete() throws SQLException {
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT COALESCE(SUM(quantity*unit_price),0) total FROM Orders")){
            if(rs.next())System.out.printf("Total order value: %.2f%n",rs.getDouble("total"));
        }
        int id=askInt("Order ID to delete: ");
        try(PreparedStatement ps=db.prepareStatement("DELETE FROM Orders WHERE order_id=? AND status='Cancelled'")){
            ps.setInt(1,id);
            if(ps.executeUpdate()==1){System.out.println("Cancelled order deleted");return;}
        }
        try(PreparedStatement ps=db.prepareStatement("SELECT status FROM Orders WHERE order_id=?")){
            ps.setInt(1,id);
            try(ResultSet rs=ps.executeQuery()){
                System.out.println(rs.next()?"Cannot delete: order is not Cancelled":"Order not found");
            }
        }
    }
    private static void listAll() throws SQLException {
        try(Statement st=db.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM Orders ORDER BY order_id")){
            while(rs.next())show(rs);
        }
    }
    public static void main(String[] args) {
        String password=System.getenv("DB_PASSWORD");
        if(password==null)password=ask("MySQL root password (input visible): ");
        try(Connection connection=DriverManager.getConnection("jdbc:mysql://localhost:3306/ecommerce_db", "root", password)){
            db=connection;setup();System.out.println("Connected to ecommerce_db!");
            while(true){
                System.out.println("\n1. Insert 3 sample orders (FAST)\n2. Insert custom order\n3. Search status / qty>2 / price DESC\n4. Add quantity\n5. Update status\n6. Total value + delete Cancelled\n7. Show all orders\n8. Exit");
                try {
                    int choice=askInt("Enter choice: ");
                    switch(choice){
                        case 1 -> insertThreeSamples();case 2 -> insertCustom();case 3 -> findByStatus();
                        case 4 -> increaseQuantity();case 5 -> updateStatus();case 6 -> totalAndDelete();
                        case 7 -> listAll();case 8 -> {System.out.println("Goodbye");return;}
                        default -> System.out.println("Invalid choice");
                    }
                } catch (SQLException | NumberFormatException e){System.out.println("Error: "+e.getMessage());}
            }
        } catch(SQLException e){System.out.println("Database error: "+e.getMessage());}
    }
}

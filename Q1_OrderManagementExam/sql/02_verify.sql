USE ecommerce_db;
SELECT * FROM Orders ORDER BY order_id;
SELECT COALESCE(SUM(quantity*unit_price),0) AS total_order_value FROM Orders;

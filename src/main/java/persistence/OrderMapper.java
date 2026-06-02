package persistence;

import exceptions.DatabaseException;
import model.BomLine;
import model.CarportSpec;
import model.Contact;
import model.Order;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderMapper {

    private final ConnectionPool pool;

    public OrderMapper(ConnectionPool pool) {
        this.pool = pool;
    }

    public int createRequest(Integer userId, Contact contact, CarportSpec spec) throws DatabaseException {
        String insOrder = "INSERT INTO orders (user_id, roof_type, width_cm, length_cm, with_shed, " +
                "shed_width_cm, shed_length_cm, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'REQUESTED')";
        Connection c = null;
        try {
            c = pool.getConnection();
            c.setAutoCommit(false);
            int orderId;
            try (PreparedStatement ps = c.prepareStatement(insOrder, Statement.RETURN_GENERATED_KEYS)) {
                if (userId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, userId);
                ps.setString(2, spec.roofType);
                ps.setInt(3, spec.widthCm);
                ps.setInt(4, spec.lengthCm);
                ps.setBoolean(5, spec.withShed);
                ps.setInt(6, spec.shedWidthCm);
                ps.setInt(7, spec.shedLengthCm);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); orderId = keys.getInt(1); }
            }
            if (contact != null && contact.hasMinimum()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO order_contacts (order_id, first_name, last_name, address, " +
                        "postal_code, city, email, phone, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, orderId);
                    ps.setString(2, contact.firstName);
                    ps.setString(3, contact.lastName);
                    ps.setString(4, contact.address);
                    ps.setString(5, contact.postalCode);
                    ps.setString(6, contact.city);
                    ps.setString(7, contact.email);
                    ps.setString(8, contact.phone);
                    ps.setString(9, contact.message);
                    ps.executeUpdate();
                }
            }
            c.commit();
            return orderId;
        } catch (SQLException ex) {
            rollback(c);
            throw new DatabaseException(ex, "Kunne ikke oprette forespørgsel");
        } finally { closeQuietly(c); }
    }

    public List<Order> findAll() throws DatabaseException {
        return query(baseSelect() + " ORDER BY o.order_date DESC", null);
    }

    public List<Order> findByUser(int userId) throws DatabaseException {
        return query(baseSelect() + " WHERE o.user_id = ? ORDER BY o.order_date DESC", userId);
    }

    public Optional<Order> findById(int orderId) throws DatabaseException {
        List<Order> r = query(baseSelect() + " WHERE o.order_id = ?", orderId);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    private String baseSelect() {
        return "SELECT o.*, u.username, " +
               "u.first_name AS u_first, u.last_name AS u_last, u.phonenumber AS u_phone, u.email AS u_email, " +
               "u.address AS u_address, u.postal_code AS u_postal, u.city AS u_city, " +
               "oc.first_name, oc.last_name, oc.address, oc.postal_code, " +
               "oc.city, oc.email, oc.phone, oc.message " +
               "FROM orders o LEFT JOIN users u ON o.user_id = u.user_id " +
               "LEFT JOIN order_contacts oc ON oc.order_id = o.order_id";
    }

    public void updateStatus(int orderId, String status) throws DatabaseException {
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE orders SET status = ? WHERE order_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        } catch (SQLException ex) { throw new DatabaseException(ex, "Kunne ikke opdatere status"); }
    }

    public void saveQuote(int orderId, CarportSpec spec, BigDecimal total, String note) throws DatabaseException {
        String sql = "UPDATE orders SET roof_type=?, width_cm=?, length_cm=?, with_shed=?, shed_width_cm=?, " +
                "shed_length_cm=?, total_price=?, admin_note=?, status='QUOTED', sketch_released=TRUE WHERE order_id=?";
        try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, spec.roofType);
            ps.setInt(2, spec.widthCm);
            ps.setInt(3, spec.lengthCm);
            ps.setBoolean(4, spec.withShed);
            ps.setInt(5, spec.shedWidthCm);
            ps.setInt(6, spec.shedLengthCm);
            ps.setBigDecimal(7, total);
            ps.setString(8, note);
            ps.setInt(9, orderId);
            ps.executeUpdate();
        } catch (SQLException ex) { throw new DatabaseException(ex, "Kunne ikke gemme tilbud"); }
    }

    public void markPaidWithLines(int orderId, List<BomLine> lines) throws DatabaseException {
        Connection c = null;
        try {
            c = pool.getConnection();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE orders SET paid = TRUE, status = 'PAID' WHERE order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
            String insQty = "INSERT INTO order_quantity (material_id, quantity) VALUES (?, ?)";
            String insLine = "INSERT INTO order_lines (order_id, quantity_id, order_price) VALUES (?, ?, ?)";
            for (BomLine l : lines) {
                if (l.materialId < 0) continue;
                int quantityId;
                try (PreparedStatement ps = c.prepareStatement(insQty, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, l.materialId);
                    ps.setInt(2, l.quantity);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); quantityId = keys.getInt(1); }
                }
                try (PreparedStatement ps = c.prepareStatement(insLine)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, quantityId);
                    ps.setBigDecimal(3, l.lineTotal);
                    ps.executeUpdate();
                }
            }
            c.commit();
        } catch (SQLException ex) {
            rollback(c);
            throw new DatabaseException(ex, "Betaling kunne ikke gennemføres");
        } finally { closeQuietly(c); }
    }

    public List<BomLine> findLines(int orderId) throws DatabaseException {
        String sql = "SELECT m.material_id, m.material_type, m.material_name, oq.quantity, ol.order_price " +
                "FROM order_lines ol JOIN order_quantity oq ON ol.quantity_id = oq.quantity_id " +
                "JOIN materials m ON oq.material_id = m.material_id WHERE ol.order_id = ? ORDER BY ol.orderline_id";
        List<BomLine> lines = new ArrayList<>();
        try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("quantity");
                    BigDecimal lp = rs.getBigDecimal("order_price");
                    BigDecimal unit = (qty > 0 && lp != null)
                            ? lp.divide(BigDecimal.valueOf(qty), 2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    lines.add(new BomLine(rs.getInt("material_id"), rs.getString("material_type"),
                            rs.getString("material_name"), 0, qty, "stk", "", unit));
                }
            }
        } catch (SQLException ex) { throw new DatabaseException(ex, "Kunne ikke hente styklisten"); }
        return lines;
    }

    private List<Order> query(String sql, Integer param) throws DatabaseException {
        List<Order> orders = new ArrayList<>();
        try (Connection c = pool.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) orders.add(map(rs)); }
        } catch (SQLException ex) { throw new DatabaseException(ex, "Kunne ikke hente ordrer"); }
        return orders;
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.orderId = rs.getInt("order_id");
        int uid = rs.getInt("user_id");
        o.userId = rs.wasNull() ? null : uid;
        o.username = rs.getString("username");
        o.orderDate = rs.getTimestamp("order_date");
        o.spec = new CarportSpec(rs.getString("roof_type"), rs.getInt("width_cm"), rs.getInt("length_cm"),
                rs.getBoolean("with_shed"), rs.getInt("shed_width_cm"), rs.getInt("shed_length_cm"));
        o.status = rs.getString("status");
        o.totalPrice = rs.getBigDecimal("total_price");
        o.adminNote = rs.getString("admin_note");
        o.paid = rs.getBoolean("paid");
        o.sketchReleased = rs.getBoolean("sketch_released");

        Contact ct = new Contact();
        String guestFirst = rs.getString("first_name");
        if (guestFirst != null || rs.getString("phone") != null) {
            ct.firstName = guestFirst;
            ct.lastName = rs.getString("last_name");
            ct.address = rs.getString("address");
            ct.postalCode = rs.getString("postal_code");
            ct.city = rs.getString("city");
            ct.email = rs.getString("email");
            ct.phone = rs.getString("phone");
            ct.message = rs.getString("message");
        } else {
            ct.firstName = rs.getString("u_first");
            ct.lastName = rs.getString("u_last");
            ct.address = rs.getString("u_address");
            ct.postalCode = rs.getString("u_postal");
            ct.city = rs.getString("u_city");
            ct.email = rs.getString("u_email");
            ct.phone = rs.getString("u_phone");
        }
        o.contact = ct;
        return o;
    }

    private void rollback(Connection c) { if (c != null) try { c.rollback(); } catch (SQLException ignore) {} }
    private void closeQuietly(Connection c) { if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignore) {} }
}

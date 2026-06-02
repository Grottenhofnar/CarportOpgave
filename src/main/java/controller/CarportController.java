package controller;

import exceptions.DatabaseException;
import io.javalin.http.Context;
import model.*;
import org.mindrot.jbcrypt.BCrypt;
import persistence.ConnectionPool;
import persistence.MaterialMapper;
import persistence.OrderMapper;
import service.BomCalculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CarportController {

    private final ConnectionPool pool;
    private final MaterialMapper materialMapper;
    private final OrderMapper orderMapper;

    public CarportController(ConnectionPool pool) {
        this.pool = pool;
        this.materialMapper = new MaterialMapper(pool);
        this.orderMapper = new OrderMapper(pool);
    }

    public void preview(Context ctx) {
        try {
            CarportSpec spec = ctx.bodyAsClass(CarportSpec.class);
            spec.validate();
            ctx.json(new BomCalculator(materialMapper.getMaterialsByName()).calculate(spec));
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (DatabaseException e) {
            ctx.status(500).result("Databasefejl");
        }
    }

    public void createRequest(Context ctx) {
        try {
            Map<?, ?> b = ctx.bodyAsClass(Map.class);
            Integer userId = ctx.sessionAttribute("userId");

            CarportSpec spec = new CarportSpec(str(b.get("roofType")),
                    intOf(b.get("widthCm")), intOf(b.get("lengthCm")), boolOf(b.get("withShed")),
                    intOf(b.get("shedWidthCm")), intOf(b.get("shedLengthCm")));
            spec.validate();

            Contact contact = null;
            String tempPassword = null;
            boolean accountCreated = false;

            if (userId == null) {
                contact = new Contact();
                contact.firstName = str(b.get("firstName"));
                contact.lastName = str(b.get("lastName"));
                contact.address = str(b.get("address"));
                contact.postalCode = str(b.get("postalCode"));
                contact.city = str(b.get("city"));
                contact.email = str(b.get("email"));
                contact.phone = str(b.get("phone"));
                contact.message = str(b.get("message"));
                if (!contact.hasMinimum()) {
                    ctx.status(400).result("Udfyld mindst fornavn og telefon, så vi kan kontakte dig");
                    return;
                }

                if (contact.email != null && !contact.email.isBlank()) {
                    try (var conn = pool.getConnection()) {
                        var check = conn.prepareStatement("SELECT 1 FROM users WHERE email = ?");
                        check.setString(1, contact.email);
                        if (check.executeQuery().next()) {

                            ctx.status(409).json(Map.of(
                                    "error", "email_exists",
                                    "message", "Denne email er allerede en registreret bruger. Log ind for at se din forespørgsel."));
                            return;
                        }
                        tempPassword = generatePassword();
                        String hash = BCrypt.hashpw(tempPassword, BCrypt.gensalt());
                        var ins = conn.prepareStatement(
                                "INSERT INTO users (username, password, first_name, last_name, address, " +
                                "postal_code, city, email, phonenumber, role) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'customer')",
                                java.sql.Statement.RETURN_GENERATED_KEYS);
                        ins.setString(1, contact.email);
                        ins.setString(2, hash);
                        ins.setString(3, contact.firstName);
                        ins.setString(4, contact.lastName);
                        ins.setString(5, contact.address);
                        ins.setString(6, contact.postalCode);
                        ins.setString(7, contact.city);
                        ins.setString(8, contact.email);
                        ins.setString(9, contact.phone);
                        ins.executeUpdate();
                        var keys = ins.getGeneratedKeys();
                        if (keys.next()) { userId = keys.getInt(1); accountCreated = true; }
                    }
                }
            }

            int orderId = orderMapper.createRequest(accountCreated ? userId : (userId), accountCreated ? null : contact, spec);

            Map<String, Object> out = new java.util.HashMap<>();
            out.put("orderId", orderId);
            out.put("status", "REQUESTED");
            out.put("accountCreated", accountCreated);
            if (accountCreated) {
                out.put("email", contact.email);
                out.put("tempPassword", tempPassword);
            }
            ctx.status(201).json(out);
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (DatabaseException e) {
            ctx.status(500).result("Kunne ikke gemme forespørgslen");
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Kunne ikke gemme forespørgslen");
        }
    }

    private static String generatePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public void myOrders(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try {
            ctx.json(orderMapper.findByUser(userId).stream().map(this::summary).collect(Collectors.toList()));
        } catch (DatabaseException e) { ctx.status(500).result("Databasefejl"); }
    }

    public void pay(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try {
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            Optional<Order> opt = orderMapper.findById(orderId);
            if (opt.isEmpty() || opt.get().userId == null || opt.get().userId != userId) {
                ctx.status(403).result("Ingen adgang"); return;
            }
            Order o = opt.get();
            if (!"QUOTED".equals(o.status) && !"IN_PROGRESS".equals(o.status)) {
                ctx.status(409).result("Ordren kan ikke betales"); return;
            }
            BillOfMaterials bom = new BomCalculator(materialMapper.getMaterialsByName()).calculate(o.spec);
            orderMapper.markPaidWithLines(orderId, bom.lines);
            ctx.json(Map.of("orderId", orderId, "status", "PAID"));
        } catch (DatabaseException e) { ctx.status(500).result("Betaling fejlede"); }
    }

    public void bom(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try {
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            boolean admin = "admin".equalsIgnoreCase(ctx.sessionAttribute("role"));
            Optional<Order> opt = orderMapper.findById(orderId);
            if (opt.isEmpty() || (!admin && (opt.get().userId == null || opt.get().userId != userId))) {
                ctx.status(403).result("Ingen adgang"); return;
            }
            Order o = opt.get();
            if (!o.paid) { ctx.status(402).result("Styklisten frigives når ordren er betalt"); return; }
            ctx.json(Map.of("lines", orderMapper.findLines(orderId),
                    "total", o.totalPrice == null ? BigDecimal.ZERO : o.totalPrice));
        } catch (DatabaseException e) { ctx.status(500).result("Databasefejl"); }
    }

    public void orderPdf(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try {
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            boolean admin = "admin".equalsIgnoreCase(ctx.sessionAttribute("role"));
            Optional<Order> opt = orderMapper.findById(orderId);
            if (opt.isEmpty() || (!admin && (opt.get().userId == null || opt.get().userId != userId))) {
                ctx.status(403).result("Ingen adgang"); return;
            }
            Order o = opt.get();
            if (!o.paid) { ctx.status(402).result("PDF'en frigives når ordren er betalt"); return; }
            BillOfMaterials bom = new BomCalculator(materialMapper.getMaterialsByName()).calculate(o.spec);
            byte[] pdf = new service.PdfGenerator().build(o, bom);
            ctx.contentType("application/pdf");
            ctx.header("Content-Disposition", "attachment; filename=\"carport-ordre-" + orderId + ".pdf\"");
            ctx.result(pdf);
        } catch (DatabaseException e) {
            ctx.status(500).result("Databasefejl");
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Kunne ikke lave PDF");
        }
    }

    public void dims(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try {
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            boolean admin = "admin".equalsIgnoreCase(ctx.sessionAttribute("role"));
            Optional<Order> opt = orderMapper.findById(orderId);
            if (opt.isEmpty() || (!admin && (opt.get().userId == null || opt.get().userId != userId))) {
                ctx.status(403).result("Ingen adgang"); return;
            }
            if (!admin && !opt.get().sketchReleased) {
                ctx.status(403).result("Tegningen er ikke frigivet endnu"); return;
            }
            ctx.json(summary(opt.get()));
        } catch (DatabaseException e) { ctx.status(500).result("Databasefejl"); }
    }

    public void requireAdmin(Context ctx) {
        if (!"admin".equalsIgnoreCase(ctx.sessionAttribute("role"))) {
            ctx.status(403).result("Kun for administratorer");
            ctx.skipRemainingHandlers();
        }
    }

    public void adminAllOrders(Context ctx) {
        try {
            ctx.json(orderMapper.findAll().stream().map(this::summary).collect(Collectors.toList()));
        } catch (DatabaseException e) { ctx.status(500).result("Databasefejl"); }
    }

    public void adminDetail(Context ctx) {
        try {
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            Optional<Order> opt = orderMapper.findById(orderId);
            if (opt.isEmpty()) { ctx.status(404).result("Ordre findes ikke"); return; }
            Order o = opt.get();
            if ("REQUESTED".equals(o.status)) { orderMapper.updateStatus(orderId, "IN_PROGRESS"); o.status = "IN_PROGRESS"; }
            BillOfMaterials bom = new BomCalculator(materialMapper.getMaterialsByName()).calculate(o.spec);
            ctx.json(Map.of("order", summary(o), "bom", bom));
        } catch (DatabaseException e) { ctx.status(500).result("Databasefejl"); }
    }

    public void adminQuote(Context ctx) {
        try {
            int orderId = Integer.parseInt(ctx.pathParam("id"));
            Map<?, ?> b = ctx.bodyAsClass(Map.class);
            CarportSpec spec = new CarportSpec(str(b.get("roofType")),
                    intOf(b.get("widthCm")), intOf(b.get("lengthCm")), boolOf(b.get("withShed")),
                    intOf(b.get("shedWidthCm")), intOf(b.get("shedLengthCm")));
            spec.validate();
            String note = ctx.queryParam("note");

            java.math.BigDecimal salePrice;
            Object sp = b.get("salePrice");
            if (sp == null) { ctx.status(400).result("Mangler salgspris"); return; }
            try { salePrice = new java.math.BigDecimal(sp.toString()); }
            catch (NumberFormatException e) { ctx.status(400).result("Ugyldig salgspris"); return; }
            orderMapper.saveQuote(orderId, spec, salePrice, note);
            ctx.json(Map.of("orderId", orderId, "status", "QUOTED", "total", salePrice));
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        } catch (DatabaseException e) { ctx.status(500).result("Databasefejl"); }
    }

    public void handleLogin(Context ctx) {
        try {
            Map<?, ?> b = ctx.bodyAsClass(Map.class);
            String email = str(b.get("email"));
            String password = str(b.get("password"));
            try (var conn = pool.getConnection()) {
                var stmt = conn.prepareStatement(
                        "SELECT user_id, password, role, first_name, last_name, address, " +
                        "postal_code, city, email, phonenumber FROM users WHERE email = ?");
                stmt.setString(1, email);
                var rs = stmt.executeQuery();
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                    ctx.sessionAttribute("userId", rs.getInt("user_id"));
                    ctx.sessionAttribute("email", rs.getString("email"));
                    ctx.sessionAttribute("role", rs.getString("role"));
                    Map<String, Object> out = new java.util.HashMap<>();
                    out.put("role", rs.getString("role") == null ? "customer" : rs.getString("role"));
                    out.put("firstName", n(rs.getString("first_name")));
                    out.put("lastName", n(rs.getString("last_name")));
                    out.put("address", n(rs.getString("address")));
                    out.put("postalCode", n(rs.getString("postal_code")));
                    out.put("city", n(rs.getString("city")));
                    out.put("email", n(rs.getString("email")));
                    out.put("phone", n(rs.getString("phonenumber")));
                    ctx.json(out);
                } else {
                    ctx.status(401).result("Forkert email eller adgangskode");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(400).result("Invalid request");
        }
    }

    public void handleSignup(Context ctx) {
        try {
            Map<?, ?> b = ctx.bodyAsClass(Map.class);
            String email = str(b.get("email"));
            String password = str(b.get("password"));
            if (email.isBlank() || password.isBlank()) { ctx.status(400).result("Udfyld email og adgangskode"); return; }
            try (var conn = pool.getConnection()) {
                var check = conn.prepareStatement("SELECT 1 FROM users WHERE email = ?");
                check.setString(1, email);
                if (check.executeQuery().next()) { ctx.status(409).result("Der findes allerede en bruger med den email"); return; }
                String hash = BCrypt.hashpw(password, BCrypt.gensalt());

                var ins = conn.prepareStatement(
                        "INSERT INTO users (username, password, first_name, last_name, address, " +
                        "postal_code, city, email, phonenumber, role) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'customer')");
                ins.setString(1, email);
                ins.setString(2, hash);
                ins.setString(3, str(b.get("firstName")));
                ins.setString(4, str(b.get("lastName")));
                ins.setString(5, str(b.get("address")));
                ins.setString(6, str(b.get("postalCode")));
                ins.setString(7, str(b.get("city")));
                ins.setString(8, email);
                ins.setString(9, str(b.get("phone")));
                ins.executeUpdate();
                ctx.status(201).result("Bruger oprettet");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(400).result("Invalid request");
        }
    }

    public void profile(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try (var conn = pool.getConnection()) {
            var ps = conn.prepareStatement(
                    "SELECT first_name, last_name, address, postal_code, city, email, phonenumber " +
                    "FROM users WHERE user_id = ?");
            ps.setInt(1, userId);
            var rs = ps.executeQuery();
            if (!rs.next()) { ctx.status(404).result("Bruger findes ikke"); return; }
            ctx.json(profileMap(rs));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Databasefejl");
        }
    }

    public void updateProfile(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try {
            Map<?, ?> b = ctx.bodyAsClass(Map.class);
            String email = str(b.get("email"));
            if (email.isBlank()) { ctx.status(400).result("Email skal udfyldes"); return; }
            try (var conn = pool.getConnection()) {

                var check = conn.prepareStatement("SELECT 1 FROM users WHERE email = ? AND user_id <> ?");
                check.setString(1, email);
                check.setInt(2, userId);
                if (check.executeQuery().next()) { ctx.status(409).result("Emailen er allerede i brug"); return; }

                var ps = conn.prepareStatement(
                        "UPDATE users SET first_name=?, last_name=?, address=?, postal_code=?, " +
                        "city=?, email=?, phonenumber=? WHERE user_id=?");
                ps.setString(1, str(b.get("firstName")));
                ps.setString(2, str(b.get("lastName")));
                ps.setString(3, str(b.get("address")));
                ps.setString(4, str(b.get("postalCode")));
                ps.setString(5, str(b.get("city")));
                ps.setString(6, email);
                ps.setString(7, str(b.get("phone")));
                ps.setInt(8, userId);
                ps.executeUpdate();
                ctx.sessionAttribute("email", email);

                var rs2 = conn.prepareStatement(
                        "SELECT first_name, last_name, address, postal_code, city, email, phonenumber " +
                        "FROM users WHERE user_id = ?");
                rs2.setInt(1, userId);
                var rs = rs2.executeQuery();
                rs.next();
                ctx.json(profileMap(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Kunne ikke gemme oplysninger");
        }
    }

    private Map<String, Object> profileMap(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> p = new java.util.HashMap<>();
        p.put("firstName", n(rs.getString("first_name")));
        p.put("lastName", n(rs.getString("last_name")));
        p.put("address", n(rs.getString("address")));
        p.put("postalCode", n(rs.getString("postal_code")));
        p.put("city", n(rs.getString("city")));
        p.put("email", n(rs.getString("email")));
        p.put("phone", n(rs.getString("phonenumber")));
        return p;
    }

    public void changePassword(Context ctx) {
        Integer userId = ctx.sessionAttribute("userId");
        if (userId == null) { ctx.status(401).result("Log ind"); return; }
        try {
            Map<?, ?> b = ctx.bodyAsClass(Map.class);
            String current = str(b.get("currentPassword"));
            String next = str(b.get("newPassword"));
            String repeat = str(b.get("repeatPassword"));
            if (next.length() < 4) { ctx.status(400).result("Ny adgangskode skal være mindst 4 tegn"); return; }
            if (!next.equals(repeat)) { ctx.status(400).result("De to nye adgangskoder er ikke ens"); return; }
            try (var conn = pool.getConnection()) {
                var ps = conn.prepareStatement("SELECT password FROM users WHERE user_id = ?");
                ps.setInt(1, userId);
                var rs = ps.executeQuery();
                if (!rs.next()) { ctx.status(404).result("Bruger findes ikke"); return; }
                if (!BCrypt.checkpw(current, rs.getString("password"))) {
                    ctx.status(403).result("Forkert nuværende adgangskode"); return;
                }
                String hash = BCrypt.hashpw(next, BCrypt.gensalt());
                var up = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?");
                up.setString(1, hash);
                up.setInt(2, userId);
                up.executeUpdate();
                ctx.json(Map.of("ok", true));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Kunne ikke skifte adgangskode");
        }
    }

    public void handleLogout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.json(Map.of("ok", true));
    }

    private Map<String, Object> summary(Order o) {
        CarportSpec s = o.spec;
        Contact c = o.contact;
        String contactName = c == null ? "" :
                ((c.firstName == null ? "" : c.firstName) + " " + (c.lastName == null ? "" : c.lastName)).trim();
        return Map.ofEntries(
                Map.entry("orderId", o.orderId),
                Map.entry("username", o.username == null ? "" : o.username),
                Map.entry("contactName", contactName),
                Map.entry("contactPhone", c == null || c.phone == null ? "" : c.phone),
                Map.entry("contactEmail", c == null || c.email == null ? "" : c.email),
                Map.entry("contactAddress", c == null ? "" :
                        ((c.address == null ? "" : c.address) + ", " + (c.postalCode == null ? "" : c.postalCode)
                                + " " + (c.city == null ? "" : c.city)).trim()),
                Map.entry("message", c == null || c.message == null ? "" : c.message),
                Map.entry("roofType", s.roofType == null ? "" : s.roofType),
                Map.entry("widthCm", s.widthCm),
                Map.entry("lengthCm", s.lengthCm),
                Map.entry("withShed", s.withShed),
                Map.entry("shedWidthCm", s.shedWidthCm),
                Map.entry("shedLengthCm", s.shedLengthCm),
                Map.entry("status", o.status),
                Map.entry("statusLabel", o.statusLabel()),
                Map.entry("totalPrice", o.totalPrice == null ? "" : o.totalPrice),
                Map.entry("adminNote", o.adminNote == null ? "" : o.adminNote),
                Map.entry("paid", o.paid),
                Map.entry("sketchReleased", o.sketchReleased));
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
    private static String n(String s) { return s == null ? "" : s; }
    private static int intOf(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return 0; }
    }
    private static boolean boolOf(Object o) { return Boolean.TRUE.equals(o) || "true".equals(str(o)); }
}

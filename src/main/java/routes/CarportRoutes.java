package routes;

import controller.CarportController;
import io.javalin.Javalin;

public class CarportRoutes {

    public static void register(Javalin app, CarportController c) {

        app.get("/", ctx -> ctx.redirect("/Order.html"));

        app.post("/api/login", c::handleLogin);
        app.post("/api/signup", c::handleSignup);
        app.post("/api/logout", c::handleLogout);
        app.get("/api/profile", c::profile);
        app.post("/api/profile", c::updateProfile);
        app.post("/api/profile/password", c::changePassword);

        app.post("/api/preview", c::preview);

        app.post("/api/orders", c::createRequest);

        app.get("/api/orders", c::myOrders);
        app.post("/api/orders/{id}/pay", c::pay);
        app.get("/api/orders/{id}/bom", c::bom);
        app.get("/api/orders/{id}/pdf", c::orderPdf);
        app.get("/api/orders/{id}/dims", c::dims);

        app.before("/api/admin/*", c::requireAdmin);

        app.get("/api/admin/orders", c::adminAllOrders);
        app.get("/api/admin/orders/{id}", c::adminDetail);
        app.post("/api/admin/orders/{id}/quote", c::adminQuote);
    }
}

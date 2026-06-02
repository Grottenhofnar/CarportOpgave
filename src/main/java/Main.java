import controller.CarportController;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import persistence.ConnectionPool;
import routes.CarportRoutes;

public class Main {

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }

    public static void main(String[] args) {
        String user = env("DB_USER", "postgres");
        String password = env("DB_PASSWORD", "postgres");
        String url = env("DB_URL", "jdbc:postgresql://localhost:5432/%s?currentSchema=public");
        String db = env("DB_NAME", "carport");

        ConnectionPool pool = ConnectionPool.getInstance(user, password, url, db);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
        }).start(7000);

        CarportController controller = new CarportController(pool);
        CarportRoutes.register(app, controller);

        System.out.println("Carport kører på http://localhost:7000");
    }
}

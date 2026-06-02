package persistence;

import exceptions.DatabaseException;
import model.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MaterialMapper {

    private final ConnectionPool pool;

    public MaterialMapper(ConnectionPool pool) {
        this.pool = pool;
    }

    public Map<String, Material> getMaterialsByName() throws DatabaseException {
        Map<String, Material> map = new HashMap<>();
        String sql = "SELECT material_id, material_type, material_name, material_price, " +
                "material_length, material_amount FROM materials";
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Integer len = rs.getObject("material_length") == null ? null
                        : (int) rs.getDouble("material_length");
                map.put(rs.getString("material_name"), new Material(
                        rs.getInt("material_id"),
                        rs.getString("material_type"),
                        rs.getString("material_name"),
                        rs.getBigDecimal("material_price"),
                        len,
                        (Integer) rs.getObject("material_amount")));
            }
        } catch (SQLException ex) {
            throw new DatabaseException(ex, "Kunne ikke hente materialer");
        }
        return map;
    }
}

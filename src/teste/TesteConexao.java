package teste; // ajuste se você colocou outro nome de pacote

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TesteConexao {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:database/folha.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("Conectado ao banco com sucesso!");

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM funcionarios");

                while (rs.next()) {
                    System.out.println(rs.getInt("id") + " - " + rs.getString("nome") + " - " + rs.getString("cargo"));
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
        }
    }
}
package com.applicationsbar.chatserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MySql {


    public static Connection getConnection() {
        Connection con = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/chat?useUnicode=true&characterEncoding=utf-8&verifyServerCertificate=false&useSSL=true&requireSSL=true", "sa1", "sa1");

        } catch (Exception e) {
            System.out.println(e);
        }
        return con;
    }


    public static void main(String[] args) throws SQLException {
        Connection con = MySql.getConnection();
        System.out.println(con.isClosed());
    }

}


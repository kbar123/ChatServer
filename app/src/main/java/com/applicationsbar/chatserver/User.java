package com.applicationsbar.chatserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class User {

    public  String firstName;
    public  String lastName;

    public  String password;
    public  String username;


    public static User getUser(String username, String password){
        String sql="select * from users where username=? and password=?";
        Connection con= MySql.getConnection();
        User u=null;
        try {
            PreparedStatement st=con.prepareStatement(sql);
            st.setString(1,username);
            st.setString(2,password);
            ResultSet rs=st.executeQuery();
            if (rs.next()) {
                u=new User();
                u.firstName=rs.getString("firstName");
                u.lastName=rs.getString("lastName");
                u.username=username;
                u.password=password;
            }
            rs.close();
            st.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return u;


    }
}
